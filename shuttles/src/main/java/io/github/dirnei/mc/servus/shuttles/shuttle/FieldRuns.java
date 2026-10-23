package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.shuttles.field.FertilizePlan;
import io.github.dirnei.mc.servus.shuttles.field.FieldCargo;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.FieldRoute;
import io.github.dirnei.mc.servus.shuttles.field.FieldStatus;
import io.github.dirnei.mc.servus.shuttles.field.HarvestStep;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterWork;
import io.github.dirnei.mc.servus.shuttles.field.PlotState;
import io.github.dirnei.mc.servus.shuttles.field.PlotWork;
import io.github.dirnei.mc.servus.shuttles.field.SeedPlan;
import io.github.dirnei.mc.servus.shuttles.field.SupplyBuffer;
import io.github.dirnei.mc.servus.shuttles.field.SupplyPlan;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleTiers;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LevelEvent;

/**
 * The legs of field runs: a harvester harvests, tills, plants and takes seeds
 * from the Field Head's buffer, a Spreader takes fertilizer from the buffer
 * and doses plots, a supply run fills the buffer from storage, and all put
 * their cargo away, Harvesters and Spreaders filling the buffer first. Called
 * by the fleet when a field run starts or its shuttle arrives.
 */
final class FieldRuns {

    /** What working one plot came to. */
    private enum PlotResult { CHANGED, NOTHING, FULL }

    private final ShuttleFleet fleet;
    private final ShuttleFleet.Host host;

    FieldRuns(ShuttleFleet fleet, ShuttleFleet.Host host) {
        this.fleet = fleet;
        this.host = host;
    }

    /** The head of a run while it still serves its field, else null. */
    private FieldHeadBlockEntity servingHead(BlockPos pos) {
        ServerLevel level = host.level();
        if (level == null || !level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof FieldHeadBlockEntity head)) {
            return null;
        }
        return head.status() == FieldStatus.SERVING ? head : null;
    }

    /** The farm rails of the head's field that shuttles of this warehouse drive on. */
    Set<NodePos> fieldRails(BlockPos headPos) {
        FieldHeadBlockEntity head = servingHead(headPos);
        Set<NodePos> rails = new HashSet<>();
        if (head != null) {
            for (BlockPos rail : head.rails()) {
                NodePos node = Conversions.toNodePos(rail);
                if (host.isTravelNode(node)) {
                    rails.add(node);
                }
            }
        }
        return rails;
    }

    /** The farm rail beside the head, where its buffer is filled and taken from, while a shuttle of this warehouse drives on it. */
    Set<NodePos> headRails(BlockPos headPos) {
        FieldHeadBlockEntity head = servingHead(headPos);
        BlockPos rail = head == null ? null : head.headRail();
        if (rail == null || !host.isTravelNode(Conversions.toNodePos(rail))) {
            return Set.of();
        }
        return Set.of(Conversions.toNodePos(rail));
    }

    /** Whether the shuttle stands on the farm rail beside the head. */
    private static boolean atHead(Shuttle shuttle, FieldHeadBlockEntity head) {
        BlockPos rail = head.headRail();
        return rail != null && rail.equals(Conversions.toBlockPos(shuttle.at()));
    }

    /** Drives to the farm rail beside the head; false when no way leads there. */
    private boolean goToHead(Shuttle shuttle, FieldHeadBlockEntity head) {
        List<NodePos> path = pathTo(shuttle, headRails(head.getBlockPos()));
        if (path.isEmpty()) {
            return false;
        }
        shuttle.leg = Leg.TO_HEAD;
        shuttle.motion = new ShuttleMotion(path);
        fleet.markSync();
        return true;
    }

    private static ItemKey cropKey(FieldHeadBlockEntity head) {
        return head.plantMode() ? ItemKey.of(head.crop()) : null;
    }

    /** Whether the field gets tilled and planted now: a crop is set, harvest only is off and it has water. */
    private static boolean plants(FieldHeadBlockEntity head) {
        return head.plantMode() && head.hydrated();
    }

    private static boolean carriesSeed(FieldRun run, ItemKey crop) {
        return crop != null && run.cargo.count(crop) > 0;
    }

    /** Ticks one plot takes for this shuttle: the field's work time shortened by its Handling. */
    private static int workTicks(Shuttle shuttle) {
        return ShuttleTiers.workTicks(ShuttlesStatsManager.INSTANCE.fieldWorkTicks(), shuttle.profile.handling());
    }

    /** Called every tick while a field shuttle stands still at the end of its leg. */
    void arrive(Shuttle shuttle) {
        FieldRun fieldRun = shuttle.fieldRun();
        if (fieldRun == null) {
            lose(shuttle);
            return;
        }
        switch (shuttle.leg) {
            case TO_STORE -> {
                unloadAtStop(shuttle, fieldRun);
                continueStore(shuttle);
            }
            default -> {
                if (shuttle.supply != null) {
                    arriveSupply(shuttle);
                } else if (shuttle.spread != null) {
                    arriveSpread(shuttle);
                } else {
                    arriveHarvest(shuttle);
                }
            }
        }
    }

    /**
     * A field shuttle restored from a save goes on from the node it stands
     * on: stacks on their way into storage first, then its run decides again.
     */
    void resume(Shuttle shuttle) {
        FieldRun run = shuttle.fieldRun();
        if (!fleet.onWay(shuttle, shuttle.at())) {
            lose(shuttle);
        } else if (!run.store.isEmpty()) {
            continueStore(shuttle);
        } else if (shuttle.supply != null) {
            supplyTurn(shuttle);
        } else if (shuttle.spread != null) {
            spreadTurn(shuttle);
        } else {
            turn(shuttle);
        }
    }

    /** The field shuttle lost its way: it is back in its garage and its cargo drops there. */
    private void lose(Shuttle shuttle) {
        fleet.park(shuttle);
        fleet.markSync();
    }

    /** The id of the field job the shuttle is out on; its locks carry it. */
    private static int jobId(Shuttle shuttle) {
        ShuttleJob lead = shuttle.lead();
        return lead == null ? 0 : lead.id();
    }

    /** Releases every lock the shuttle's field run still holds. */
    void release(Shuttle shuttle) {
        FieldRun run = shuttle.fieldRun();
        if (run != null && !run.locked.isEmpty()) {
            host.releaseLocks(jobId(shuttle), List.copyOf(run.locked));
            run.locked.clear();
        }
    }

    /** Ends the field run: its locks are released and the shuttle goes home. */
    private void endRun(Shuttle shuttle) {
        release(shuttle);
        shuttle.loads.clear();
        shuttle.run = null;
        shuttle.spread = null;
        shuttle.supply = null;
        fleet.goHome(shuttle);
    }

    /**
     * Drives to the nearest rail of the head's field that the shuttle has not
     * worked in this segment and that has work; false when none can be reached.
     */
    private boolean goToRail(Shuttle shuttle, FieldHeadBlockEntity head, FieldRun run, Predicate<BlockPos> railHasWork) {
        Predicate<NodePos> hasWork = node -> {
            BlockPos rail = Conversions.toBlockPos(node);
            return head.rails().contains(rail) && !run.visited.contains(rail) && railHasWork.test(rail);
        };
        NodePos from = shuttle.at();
        Optional<NodePos> stop;
        if (head.rails().contains(Conversions.toBlockPos(from))) {
            stop = FieldRoute.next(from, node -> fieldNeighbours(head, node), hasWork);
        } else {
            List<NodePos> path = fleet.path(shuttle, from, hasWork);
            stop = path.isEmpty() ? Optional.empty() : Optional.of(path.get(path.size() - 1));
        }
        if (stop.isEmpty()) {
            return false;
        }
        NodePos goal = stop.get();
        List<NodePos> path = fleet.path(shuttle, from, goal::equals);
        if (path.isEmpty()) {
            return false;
        }
        shuttle.leg = Leg.TO_FIELD;
        shuttle.motion = new ShuttleMotion(path);
        return true;
    }

    private List<NodePos> fieldNeighbours(FieldHeadBlockEntity head, NodePos node) {
        List<NodePos> list = new ArrayList<>(4);
        for (NodePos neighbour : host.travelNeighbours(node)) {
            if (head.rails().contains(Conversions.toBlockPos(neighbour))) {
                list.add(neighbour);
            }
        }
        return list;
    }

    /** The path from the shuttle to a dock rail of the target, or empty. */
    private List<NodePos> pathTo(Shuttle shuttle, Set<NodePos> dock) {
        return dock.isEmpty() ? List.of() : fleet.path(shuttle, shuttle.at(), dock::contains);
    }

    /**
     * Adds what was taken from the source to the cargo; what does not fit goes
     * back into the source, and what the source refuses rides along to be put away.
     */
    private void stow(FieldRun run, BlockPos source, ItemStack taken) {
        if (taken.isEmpty()) {
            return;
        }
        FieldCargo.Entry<ItemKey> entry = new FieldCargo.Entry<>(ItemKey.of(taken), taken.getCount(), taken.getMaxStackSize());
        for (FieldCargo.Entry<ItemKey> left : run.cargo.stow(entry,
                overflow -> overflow.count() - host.insertAt(source, overflow.key().stack(overflow.count())).getCount())) {
            run.store.add(left.key().stack(left.count()));
        }
    }

    /** Moves the cargo, but for up to keepMax of keep, onto the stacks to put away. */
    private static void unloadToStore(FieldRun run, ItemKey keep, int keepMax) {
        for (FieldCargo.Entry<ItemKey> entry : run.cargo.unload(keep, keepMax)) {
            run.store.add(entry.key().stack(entry.count()));
        }
    }

    void startRun(Shuttle shuttle, FieldJob job) {
        shuttle.run = new HarvestRun(job.head(), shuttle.profile.stacks());
        turn(shuttle);
    }

    /** Whether some plot worked from the rail needs something the harvester can do now. */
    private boolean railHasWork(HarvestRun run, FieldHeadBlockEntity head, BlockPos rail, boolean plants, boolean haveSeed) {
        List<BlockPos> plots = run.reach(head.rails(), head.plots(), head.radius()).get(rail);
        if (plots == null) {
            return false;
        }
        for (BlockPos plot : plots) {
            if (PlotWork.plan(head.plotState(plot), plants, haveSeed, run.harvest).any()) {
                return true;
            }
        }
        return false;
    }

    /** Whether any rail of the field this warehouse drives on has work. */
    private boolean workLeft(HarvestRun run, FieldHeadBlockEntity head) {
        if (run.idle) {
            return false;
        }
        boolean plants = plants(head);
        boolean haveSeed = carriesSeed(run, cropKey(head));
        for (BlockPos rail : head.rails()) {
            if (host.isTravelNode(Conversions.toNodePos(rail)) && railHasWork(run, head, rail, plants, haveSeed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Decides what a harvester does next, at the start of a run and after
     * each work segment, put-away and seed fetch: fetch seeds, work, put the
     * harvest away or go home.
     */
    private void turn(Shuttle shuttle) {
        HarvestRun run = shuttle.run;
        fleet.markSync();
        if (!fleet.onWay(shuttle, shuttle.at())) {
            lose(shuttle);
            return;
        }
        FieldHeadBlockEntity head = servingHead(run.head);
        if (head != null) {
            run.harvest = !head.harvestPaused();
        }
        boolean workLeft = head != null && workLeft(run, head);
        int wanted = 0;
        boolean room = false;
        ItemKey crop = head == null ? null : cropKey(head);
        if (crop != null && plants(head) && !run.fetchTried && !run.idle) {
            int max = crop.stack(1).getMaxStackSize();
            int cargoRoom = run.cargo.room(crop, max);
            wanted = SeedPlan.toFetch(run.harvest ? head.ripeCount() : 0, head.plantableCount() + head.tillableCount(), run.cargo.count(crop),
                    head.supplyAvailable(SupplyBuffer.Part.SEEDS), Integer.MAX_VALUE, max);
            room = cargoRoom > 0;
            run.seedAmount = Math.min(wanted, cargoRoom);
            if (!room && run.cargo.count(crop) == run.cargoCount()) {
                // Full of seeds: a put-away would keep them all again.
                wanted = 0;
            }
        }
        switch (HarvestStep.next(head != null, run.cargoFull, wanted, room, workLeft, !run.cargo.isEmpty())) {
            case FETCH_SEEDS -> fetchSeeds(shuttle, head);
            case WORK -> {
                run.worked = false;
                run.fetchTried = false;
                run.visited.clear();
                if (!goToWork(shuttle, head)) {
                    run.idle = true;
                    turn(shuttle);
                }
            }
            case PUT_AWAY -> putAwayHarvest(shuttle, head, crop, workLeft || wanted > 0);
            case HOME -> {
                if (head != null && !run.tally.isEmpty()) {
                    head.learnProduce(run.tally);
                }
                endRun(shuttle);
            }
        }
    }

    /**
     * Puts the cargo away; while plots remain to plant, up to a stack of seeds
     * stays aboard, no more than those plots need. Spare seeds fill the head's
     * buffer first.
     */
    private void putAwayHarvest(Shuttle shuttle, FieldHeadBlockEntity head, ItemKey crop, boolean plotsLeft) {
        HarvestRun run = shuttle.run;
        boolean keep = crop != null && plotsLeft && !run.keepNothing;
        int keepMax = keep ? Math.min(crop.stack(1).getMaxStackSize(), head.plantableCount() + head.tillableCount()) : 0;
        unloadToStore(run, keepMax > 0 ? crop : null, keepMax);
        run.cargoFull = false;
        run.keepNothing = false;
        if (head != null && fillBufferFirst(shuttle, head, SupplyBuffer.Part.SEEDS)) {
            return;
        }
        continueStore(shuttle);
    }

    /**
     * Before a put-away: when the stacks to put away hold the part's item and
     * the part has room, they go into the head's buffer first. True while the
     * shuttle drives to the head's rail for that; at the rail it fills the
     * buffer at once.
     */
    private boolean fillBufferFirst(Shuttle shuttle, FieldHeadBlockEntity head, SupplyBuffer.Part part) {
        FieldRun run = shuttle.fieldRun();
        Item item = head.supplyItem(part);
        if (item == null || head.supplyRoom(part) <= 0 || run.store.stream().noneMatch(stack -> stack.is(item))) {
            return false;
        }
        if (atHead(shuttle, head)) {
            storeIntoBuffer(run, head, part);
            return false;
        }
        return goToHead(shuttle, head);
    }

    /** Moves the stacks to put away that hold the part's item into the head's buffer as far as it takes them. */
    private static void storeIntoBuffer(FieldRun run, FieldHeadBlockEntity head, SupplyBuffer.Part part) {
        for (ItemStack stack : run.store) {
            if (!stack.isEmpty() && stack.getComponentsPatch().isEmpty()) {
                stack.shrink(head.fillSupply(part, stack.getItem(), stack.getCount()));
            }
        }
        run.store.removeIf(ItemStack::isEmpty);
    }

    /** At the head's rail on the way to a put-away: fill the buffer, then put the rest away. */
    private void arriveToFillBuffer(Shuttle shuttle, FieldHeadBlockEntity head, SupplyBuffer.Part part) {
        if (head != null) {
            storeIntoBuffer(shuttle.fieldRun(), head, part);
        }
        continueStore(shuttle);
    }

    /** Drives to the nearest rail with work; false when none can be reached. */
    private boolean goToWork(Shuttle shuttle, FieldHeadBlockEntity head) {
        HarvestRun run = shuttle.run;
        boolean plants = plants(head);
        boolean haveSeed = carriesSeed(run, cropKey(head));
        return goToRail(shuttle, head, run, rail -> railHasWork(run, head, rail, plants, haveSeed));
    }

    private void arriveHarvest(Shuttle shuttle) {
        HarvestRun run = shuttle.run;
        switch (shuttle.leg) {
            case TO_FIELD -> beginStop(shuttle);
            case WORKING -> work(shuttle);
            case TO_HEAD -> {
                FieldHeadBlockEntity head = servingHead(run.head);
                if (!run.store.isEmpty()) {
                    arriveToFillBuffer(shuttle, head, SupplyBuffer.Part.SEEDS);
                    return;
                }
                if (head != null) {
                    loadSeeds(shuttle, head);
                }
                turn(shuttle);
            }
            default -> turn(shuttle);
        }
    }

    /** At a rail with work: queue the plots worked from it. */
    private void beginStop(Shuttle shuttle) {
        HarvestRun run = shuttle.run;
        FieldHeadBlockEntity head = servingHead(run.head);
        if (head == null) {
            turn(shuttle);
            return;
        }
        BlockPos rail = Conversions.toBlockPos(shuttle.at());
        run.queue.clear();
        run.queue.addAll(run.reach(head.rails(), head.plots(), head.radius()).getOrDefault(rail, List.of()));
        run.stopPlants = plants(head);
        run.harvest = !head.harvestPaused();
        run.countdown = workTicks(shuttle);
        shuttle.leg = Leg.WORKING;
        fleet.markSync();
    }

    /** One tick at a rail: wait out the work time, then work the next plot that needs something. */
    private void work(Shuttle shuttle) {
        HarvestRun run = shuttle.run;
        if (!fleet.onWay(shuttle, shuttle.at())) {
            lose(shuttle);
            return;
        }
        FieldHeadBlockEntity head = servingHead(run.head);
        if (head == null) {
            run.queue.clear();
            turn(shuttle);
            return;
        }
        if (--run.countdown > 0) {
            return;
        }
        BlockPos plot = nextPlot(run, head);
        if (plot != null) {
            run.queue.poll();
            PlotResult result = workPlot(run, head, plot);
            if (result == PlotResult.FULL) {
                ItemKey crop = cropKey(head);
                run.keepNothing = crop != null && run.cargo.count(crop) == run.cargoCount();
                run.cargoFull = true;
                run.queue.clear();
                turn(shuttle);
                return;
            }
            head.recheck(plot);
            if (!run.store.isEmpty()) {
                run.cargoFull = true;
                run.queue.clear();
                turn(shuttle);
                return;
            }
            if (result == PlotResult.CHANGED) {
                run.worked = true;
                run.countdown = workTicks(shuttle);
                fleet.markSync();
                if (nextPlot(run, head) != null) {
                    return;
                }
            } else if (nextPlot(run, head) != null) {
                // Nothing came of that plot: the next one is worked on the next tick.
                run.countdown = 1;
                return;
            }
        }
        run.visited.add(Conversions.toBlockPos(shuttle.at()));
        if (!goToWork(shuttle, head)) {
            if (!run.worked) {
                run.idle = true;
            }
            turn(shuttle);
        }
    }

    /** Drops leading plots with nothing to do and returns the first one with work, or null. */
    private BlockPos nextPlot(HarvestRun run, FieldHeadBlockEntity head) {
        ServerLevel level = host.level();
        boolean haveSeed = carriesSeed(run, cropKey(head));
        while (!run.queue.isEmpty()) {
            BlockPos plot = run.queue.peek();
            if (level.isLoaded(plot)) {
                PlotState state = HarvesterWork.classify(level, plot);
                if (PlotWork.plan(state, run.stopPlants, haveSeed, run.harvest).any()) {
                    return plot;
                }
                if (head.plotState(plot) != state) {
                    // The head missed a change here; it looks again so the route stops coming back.
                    head.recheck(plot);
                }
            }
            run.queue.poll();
        }
        return null;
    }

    /** Harvests, tills and plants one plot; FULL when its harvest does not fit and the cargo is not empty. */
    private PlotResult workPlot(HarvestRun run, FieldHeadBlockEntity head, BlockPos plot) {
        ServerLevel level = host.level();
        boolean plants = run.stopPlants;
        ItemKey crop = plants ? cropKey(head) : null;
        PlotState state = HarvesterWork.classify(level, plot);
        PlotWork.Action action = PlotWork.plan(state, plants, carriesSeed(run, crop), run.harvest);
        boolean seedInHand = false;
        boolean changed = false;
        if (action.harvest()) {
            List<FieldCargo.Entry<ItemKey>> drops = new ArrayList<>();
            for (ItemStack drop : HarvesterWork.rollDrops(level, plot)) {
                if (!drop.isEmpty()) {
                    drops.add(new FieldCargo.Entry<>(ItemKey.of(drop), drop.getCount(), drop.getMaxStackSize()));
                }
            }
            if (crop != null) {
                seedInHand = takeOne(drops, crop);
            }
            if (!run.cargo.fits(drops) && !run.cargo.isEmpty()) {
                return PlotResult.FULL;
            }
            HarvesterWork.breakCrop(level, plot);
            changed = true;
            tally(run, drops, seedInHand ? crop : null);
            addOrStore(run, drops);
        }
        if (action.till()) {
            changed |= HarvesterWork.till(level, plot);
        }
        if (crop != null && (action.plant() || seedInHand)) {
            if (seedInHand) {
                if (HarvesterWork.plant(level, plot, crop.stack(1))) {
                    changed = true;
                } else {
                    addOrStore(run, List.of(new FieldCargo.Entry<>(crop, 1, crop.stack(1).getMaxStackSize())));
                }
            } else if (run.cargo.count(crop) > 0 && HarvesterWork.plant(level, plot, crop.stack(1))) {
                run.cargo.take(crop, 1);
                changed = true;
            }
        }
        return changed ? PlotResult.CHANGED : PlotResult.NOTHING;
    }

    /** Counts a harvested plot's drops, the seed kept for replanting included, toward the run's produce. */
    private static void tally(HarvestRun run, List<FieldCargo.Entry<ItemKey>> drops, ItemKey replanted) {
        for (FieldCargo.Entry<ItemKey> drop : drops) {
            run.tally.merge(BuiltInRegistries.ITEM.getKey(drop.key().item()), (long) drop.count(), Long::sum);
        }
        if (replanted != null) {
            run.tally.merge(BuiltInRegistries.ITEM.getKey(replanted.item()), 1L, Long::sum);
        }
    }

    /** Adds the entries to the cargo; what does not fit rides along to be put away. */
    private static void addOrStore(HarvestRun run, List<FieldCargo.Entry<ItemKey>> entries) {
        for (FieldCargo.Entry<ItemKey> left : run.cargo.add(entries)) {
            run.store.add(left.key().stack(left.count()));
        }
    }

    /** Takes one of the key out of the drops; false when they hold none. */
    private static boolean takeOne(List<FieldCargo.Entry<ItemKey>> drops, ItemKey key) {
        for (int i = 0; i < drops.size(); i++) {
            FieldCargo.Entry<ItemKey> entry = drops.get(i);
            if (entry.key().equals(key)) {
                if (entry.count() > 1) {
                    drops.set(i, new FieldCargo.Entry<>(key, entry.count() - 1, entry.max()));
                } else {
                    drops.remove(i);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Locks the seeds the run wants in the head's buffer and drives to the
     * farm rail beside the head for them; standing there it takes them at once.
     */
    private void fetchSeeds(Shuttle shuttle, FieldHeadBlockEntity head) {
        HarvestRun run = shuttle.run;
        run.fetchTried = true;
        run.seedAmount = lockFromHead(shuttle, head, SupplyBuffer.Part.SEEDS, run.seedAmount);
        if (atHead(shuttle, head)) {
            loadSeeds(shuttle, head);
        } else if (run.seedAmount > 0 && goToHead(shuttle, head)) {
            return;
        } else {
            head.releaseSupply(jobId(shuttle));
        }
        turn(shuttle);
    }

    /** Locks up to amount of the part's item in the head's buffer for the shuttle's job; returns how many it locked. */
    private static int lockFromHead(Shuttle shuttle, FieldHeadBlockEntity head, SupplyBuffer.Part part, int amount) {
        int locked = amount <= 0 ? 0 : head.lockSupplyOut(jobId(shuttle), part, amount);
        if (locked > 0) {
            shuttle.fieldRun().locked.add(head.getBlockPos());
        }
        return locked;
    }

    /** Takes the seeds the run locked out of the head's buffer, as far as the cargo has room. */
    private static void loadSeeds(Shuttle shuttle, FieldHeadBlockEntity head) {
        HarvestRun run = shuttle.run;
        ItemKey crop = cropKey(head);
        if (crop != null && run.seedAmount > 0) {
            int max = crop.stack(1).getMaxStackSize();
            int taken = head.takeSupplyAs(jobId(shuttle), SupplyBuffer.Part.SEEDS, Math.min(run.seedAmount, run.cargo.room(crop, max)));
            run.cargo.add(List.of(new FieldCargo.Entry<>(crop, taken, max)));
        }
        run.seedAmount = 0;
        head.releaseSupply(jobId(shuttle));
    }

    /**
     * Puts the stacks on their way into storage: plans racks for them by the
     * put-away rule from where the shuttle stands, locking their room, and
     * drives to the first; with no rack for some, the shuttle parks in its
     * garage with them. Once all are stored, the run decides again.
     */
    void continueStore(Shuttle shuttle) {
        FieldRun run = shuttle.fieldRun();
        int jobId = jobId(shuttle);
        run.store.removeIf(ItemStack::isEmpty);
        int guard = run.store.size() * 2 + 4;
        while (!run.store.isEmpty() && guard-- > 0) {
            if (run.stops.isEmpty()) {
                planStore(shuttle, run, jobId);
                if (run.stops.isEmpty()) {
                    break;
                }
            }
            BlockPos stop = run.stops.get(0);
            List<NodePos> path = pathTo(shuttle, host.dockRails(Conversions.toNodePos(stop)));
            if (!path.isEmpty()) {
                shuttle.strandReason = null;
                shuttle.leg = Leg.TO_STORE;
                shuttle.motion = new ShuttleMotion(path);
                fleet.markSync();
                return;
            }
            run.stops.remove(0);
            host.releaseLocks(jobId, List.of(stop));
        }
        if (!run.store.isEmpty()) {
            fleet.strand(shuttle);
            fleet.markSync();
            return;
        }
        shuttle.strandReason = null;
        if (shuttle.supply != null) {
            supplyTurn(shuttle);
        } else if (shuttle.spread != null) {
            spreadTurn(shuttle);
        } else {
            turn(shuttle);
        }
    }

    /** Locks room for every stack on its way into storage in the racks the put-away rule ranks first from here. */
    private void planStore(Shuttle shuttle, FieldRun run, int jobId) {
        Set<NodePos> here = Set.of(shuttle.at());
        for (ItemStack stack : run.store) {
            for (BlockPos stop : host.storeTargets(jobId, stack, here, fleet.ways(shuttle))) {
                if (!run.stops.contains(stop)) {
                    run.stops.add(stop);
                }
                run.locked.add(stop);
            }
        }
    }

    /** At a store stop: the stacks go into the slots the rack holds locked for the run; what does not fit stays aboard. */
    private void unloadAtStop(Shuttle shuttle, FieldRun run) {
        if (run.stops.isEmpty()) {
            return;
        }
        BlockPos stop = run.stops.remove(0);
        int jobId = jobId(shuttle);
        List<ItemStack> left = new ArrayList<>(host.unloadAllInto(stop, jobId, List.copyOf(run.store)));
        run.store.clear();
        run.store.addAll(left);
        run.store.removeIf(ItemStack::isEmpty);
        host.releaseLocks(jobId, List.of(stop));
    }

    /** Starts a Spreader on a fertilize run with the head's fertilizer; a head without one ends the run at once. */
    void startSpread(Shuttle shuttle, FertilizeJob job) {
        FieldHeadBlockEntity head = servingHead(job.head());
        if (head == null || !head.fertilizing()) {
            shuttle.loads.clear();
            return;
        }
        shuttle.spread = new SpreadRun(job.head(), shuttle.profile.stacks(), ItemKey.of(head.fertilizer()),
                ShuttlesStatsManager.INSTANCE.dosesPerPlot());
        spreadTurn(shuttle);
    }

    /** The head of a fertilize run while it serves the field and still fertilizes with the run's fertilizer, else null. */
    private FieldHeadBlockEntity spreadHead(SpreadRun run) {
        FieldHeadBlockEntity head = servingHead(run.head);
        if (head == null || !head.fertilizing() || !ItemKey.of(head.fertilizer()).equals(run.fertilizer)) {
            return null;
        }
        return head;
    }

    /**
     * Decides what a Spreader does next: take fertilizer from the head's
     * buffer while the fetch is not over, dose the next rail with plots that
     * need a dose while it carries fertilizer, else put what it carries back
     * into the buffer, the rest away, and go home.
     */
    private void spreadTurn(Shuttle shuttle) {
        SpreadRun run = shuttle.spread;
        fleet.markSync();
        if (!fleet.onWay(shuttle, shuttle.at())) {
            lose(shuttle);
            return;
        }
        FieldHeadBlockEntity head = spreadHead(run);
        if (head != null && !run.fetched) {
            if (run.toFetch < 0) {
                int max = run.fertilizer.stack(1).getMaxStackSize();
                run.toFetch = lockFromHead(shuttle, head, SupplyBuffer.Part.FERTILIZER, FertilizePlan.toFetch(head.fertilizeTargetCount(),
                        run.doses, head.supplyAvailable(SupplyBuffer.Part.FERTILIZER), 0, run.cargo.room(run.fertilizer, max)));
            }
            if (run.toFetch > 0) {
                if (atHead(shuttle, head)) {
                    loadFertilizer(shuttle, head);
                } else if (goToHead(shuttle, head)) {
                    return;
                } else {
                    head.releaseSupply(jobId(shuttle));
                }
            }
            run.fetched = true;
        }
        if (head != null && run.cargo.count(run.fertilizer) > 0 && goToDose(shuttle, head)) {
            return;
        }
        if (!run.cargo.isEmpty()) {
            unloadToStore(run, null, 0);
            if (head != null && fillBufferFirst(shuttle, head, SupplyBuffer.Part.FERTILIZER)) {
                return;
            }
            continueStore(shuttle);
            return;
        }
        endRun(shuttle);
    }

    /** Takes the fertilizer the run locked out of the head's buffer, as far as the cargo has room. */
    private static void loadFertilizer(Shuttle shuttle, FieldHeadBlockEntity head) {
        SpreadRun run = shuttle.spread;
        int max = run.fertilizer.stack(1).getMaxStackSize();
        int taken = head.takeSupplyAs(jobId(shuttle), SupplyBuffer.Part.FERTILIZER, Math.min(run.toFetch, run.cargo.room(run.fertilizer, max)));
        run.toFetch = 0;
        run.cargo.add(List.of(new FieldCargo.Entry<>(run.fertilizer, taken, max)));
        head.releaseSupply(jobId(shuttle));
    }

    private void arriveSpread(Shuttle shuttle) {
        SpreadRun run = shuttle.spread;
        switch (shuttle.leg) {
            case TO_HEAD -> {
                FieldHeadBlockEntity head = spreadHead(run);
                if (!run.store.isEmpty()) {
                    arriveToFillBuffer(shuttle, head, SupplyBuffer.Part.FERTILIZER);
                    return;
                }
                if (head != null && !run.fetched) {
                    loadFertilizer(shuttle, head);
                    run.fetched = true;
                }
                spreadTurn(shuttle);
            }
            case TO_FIELD -> beginDose(shuttle);
            case WORKING -> dose(shuttle);
            default -> spreadTurn(shuttle);
        }
    }

    /** The plots worked from that rail that need a dose now. */
    private static List<BlockPos> doseQueue(SpreadRun run, FieldHeadBlockEntity head, BlockPos rail, Set<BlockPos> targets) {
        List<BlockPos> plots = run.reach(head.rails(), head.plots(), head.radius()).get(rail);
        List<BlockPos> queue = new ArrayList<>();
        if (plots != null) {
            for (BlockPos plot : plots) {
                if (targets.contains(plot)) {
                    queue.add(plot);
                }
            }
        }
        return queue;
    }

    /** Drives to the nearest rail of the field with plots that need a dose; false when none can be reached. */
    private boolean goToDose(Shuttle shuttle, FieldHeadBlockEntity head) {
        SpreadRun run = shuttle.spread;
        Set<BlockPos> targets = head.fertilizeTargets();
        if (targets.isEmpty()) {
            return false;
        }
        return goToRail(shuttle, head, run, rail -> !doseQueue(run, head, rail, targets).isEmpty());
    }

    /** At a rail with plots that need a dose: queue them. */
    private void beginDose(Shuttle shuttle) {
        SpreadRun run = shuttle.spread;
        FieldHeadBlockEntity head = spreadHead(run);
        if (head == null) {
            spreadTurn(shuttle);
            return;
        }
        run.queue.clear();
        run.queue.addAll(doseQueue(run, head, Conversions.toBlockPos(shuttle.at()), head.fertilizeTargets()));
        run.countdown = workTicks(shuttle);
        shuttle.leg = Leg.WORKING;
        fleet.markSync();
    }

    /** One tick at a rail: wait out the work time, then dose the next plot that still needs it. */
    private void dose(Shuttle shuttle) {
        SpreadRun run = shuttle.spread;
        if (!fleet.onWay(shuttle, shuttle.at())) {
            lose(shuttle);
            return;
        }
        FieldHeadBlockEntity head = spreadHead(run);
        if (head == null) {
            run.queue.clear();
            spreadTurn(shuttle);
            return;
        }
        if (--run.countdown > 0) {
            return;
        }
        BlockPos plot = nextDosePlot(run, head);
        if (plot != null) {
            run.queue.poll();
            dosePlot(run, head, plot);
            if (run.cargo.count(run.fertilizer) > 0 && nextDosePlot(run, head) != null) {
                run.countdown = workTicks(shuttle);
                fleet.markSync();
                return;
            }
        }
        run.queue.clear();
        run.visited.add(Conversions.toBlockPos(shuttle.at()));
        spreadTurn(shuttle);
    }

    /** Drops leading plots that no longer need a dose and returns the first one that does, or null. */
    private BlockPos nextDosePlot(SpreadRun run, FieldHeadBlockEntity head) {
        Set<BlockPos> targets = head.fertilizeTargets();
        while (!run.queue.isEmpty()) {
            BlockPos plot = run.queue.peek();
            if (targets.contains(plot) && host.level().isLoaded(plot)) {
                return plot;
            }
            run.queue.poll();
        }
        return null;
    }

    /**
     * Gives the plot its round: up to the run's doses, each one fertilizer
     * item applied like bone meal, while the crop grows and is not ripe. The
     * bone meal event lets the head see the growth; the head looks at once too.
     */
    private void dosePlot(SpreadRun run, FieldHeadBlockEntity head, BlockPos plot) {
        ServerLevel level = host.level();
        for (int i = 0; i < run.doses && run.cargo.count(run.fertilizer) > 0; i++) {
            if (HarvesterWork.classify(level, plot) != PlotState.GROWING) {
                break;
            }
            ItemStack dose = run.fertilizer.stack(1);
            if (!BoneMealItem.applyBonemeal(dose, level, plot, null)) {
                break;
            }
            if (dose.isEmpty()) {
                run.cargo.take(run.fertilizer, 1);
            }
            level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, plot, 15);
        }
        head.dosed(plot);
        head.recheckNow(plot);
    }

    /** Starts a supply run for the head's buffer; a head that serves no field ends the run at once. */
    void startSupply(Shuttle shuttle, SupplyJob job) {
        FieldHeadBlockEntity head = servingHead(job.head());
        if (head == null) {
            shuttle.loads.clear();
            return;
        }
        SupplyRun run = new SupplyRun(job.head(), shuttle.profile.stacks());
        shuttle.supply = run;
        planSupply(shuttle, run, head);
        supplyTurn(shuttle);
    }

    /**
     * Plans what the run picks for each part of the buffer: what it is short
     * of its level, low parts first, as far as the cargo has room. Locks it
     * in the cheapest racks holding it, as seen from the head's rail, and the
     * room for what it found in the head's buffer.
     */
    private void planSupply(Shuttle shuttle, SupplyRun run, FieldHeadBlockEntity head) {
        int jobId = jobId(shuttle);
        Set<NodePos> from = headRails(run.head);
        FieldCargo<ItemKey> planned = new FieldCargo<>(run.cargo.slots());
        Set<SupplyBuffer.Part> done = EnumSet.noneOf(SupplyBuffer.Part.class);
        for (boolean lowPass : new boolean[] {true, false}) {
            for (SupplyBuffer.Part part : SupplyBuffer.Part.VALUES) {
                Item item = head.supplyItem(part);
                int held = head.supplyCount(part);
                int level = head.supplyLevel(part);
                if (item == null || done.contains(part) || SupplyPlan.low(held, level) != lowPass) {
                    continue;
                }
                ItemKey key = ItemKey.of(new ItemStack(item));
                int max = key.stack(1).getMaxStackSize();
                int amount = SupplyPlan.toBring(held, level, host.available(key), head.supplyReserve(part), planned.room(key, max));
                Map<BlockPos, Integer> locked = host.lockStock(jobId, key, amount, from, fleet.ways(shuttle));
                int found = locked.values().stream().mapToInt(Integer::intValue).sum();
                if (found <= 0) {
                    continue;
                }
                head.lockSupplyIn(jobId, part, found);
                run.locked.add(run.head);
                planned.add(List.of(new FieldCargo.Entry<>(key, found, max)));
                done.add(part);
                for (BlockPos rack : locked.keySet()) {
                    if (!run.sources.contains(rack)) {
                        run.sources.add(rack);
                    }
                    run.locked.add(rack);
                }
            }
        }
    }

    /**
     * Decides what a supply run does next: take the locked items from their
     * racks, stop at the head's rail to fill the buffer and load stale items,
     * then put what it carries away and go home.
     */
    private void supplyTurn(Shuttle shuttle) {
        SupplyRun run = shuttle.supply;
        fleet.markSync();
        if (!fleet.onWay(shuttle, shuttle.at())) {
            lose(shuttle);
            return;
        }
        FieldHeadBlockEntity head = servingHead(run.head);
        if (head != null && !run.fetched) {
            if (fetchSupply(shuttle)) {
                return;
            }
            run.fetched = true;
        }
        if (head != null && !run.unloaded && (!run.cargo.isEmpty() || head.holdsStale())) {
            if (atHead(shuttle, head)) {
                unloadAtHead(run, head, jobId(shuttle));
            } else if (goToHead(shuttle, head)) {
                return;
            }
        }
        if (!run.unloaded) {
            run.unloaded = true;
            release(shuttle);
        }
        if (!run.cargo.isEmpty() || !run.store.isEmpty()) {
            unloadToStore(run, null, 0);
            continueStore(shuttle);
            return;
        }
        endRun(shuttle);
    }

    /** Drives to the next rack the run locked items in; a rack it cannot reach loses the run's locks. False when none is left. */
    private boolean fetchSupply(Shuttle shuttle) {
        SupplyRun run = shuttle.supply;
        while (!run.sources.isEmpty()) {
            BlockPos source = run.sources.get(0);
            List<NodePos> path = pathTo(shuttle, host.dockRails(Conversions.toNodePos(source)));
            if (!path.isEmpty()) {
                run.source = source;
                shuttle.leg = Leg.TO_SUPPLY;
                shuttle.motion = new ShuttleMotion(path);
                return true;
            }
            run.sources.remove(0);
            host.releaseLocks(jobId(shuttle), List.of(source));
        }
        return false;
    }

    private void arriveSupply(Shuttle shuttle) {
        SupplyRun run = shuttle.supply;
        if (shuttle.leg == Leg.TO_SUPPLY && run.source != null) {
            run.sources.remove(run.source);
            if (servingHead(run.head) != null) {
                for (ItemStack taken : host.takeLocked(run.source, jobId(shuttle))) {
                    stow(run, run.source, taken);
                }
            }
        }
        run.source = null;
        supplyTurn(shuttle);
    }

    /** At the head's rail: stale items come aboard to be put away, then the cargo fills the buffer as the run's job. */
    private static void unloadAtHead(FieldRun run, FieldHeadBlockEntity head, int jobId) {
        run.store.addAll(head.takeStale());
        for (FieldCargo.Entry<ItemKey> entry : run.cargo.entries()) {
            for (SupplyBuffer.Part part : SupplyBuffer.Part.VALUES) {
                fillFromCargo(run, head, part, entry.key(), jobId);
            }
        }
    }

    /** Moves as much of the key from the cargo into the part as it takes; returns how many moved. */
    private static int fillFromCargo(FieldRun run, FieldHeadBlockEntity head, SupplyBuffer.Part part, ItemKey key, int jobId) {
        int carried = run.cargo.count(key);
        if (carried <= 0 || !key.components().isEmpty()) {
            return 0;
        }
        int moved = head.fillSupplyAs(jobId, part, key.item(), carried);
        run.cargo.take(key, moved);
        return moved;
    }
}
