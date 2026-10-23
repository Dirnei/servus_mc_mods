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
import net.minecraft.world.level.block.Block;
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
                if (fieldRun.storeTarget != null) {
                    insertAll(fieldRun, fieldRun.storeTarget, storable(shuttle, shuttle.at()));
                    fieldRun.storeTarget = null;
                }
                continueStore(shuttle);
            }
            case WAIT_STORE -> {
                if (--fieldRun.waitTimer <= 0) {
                    fieldRun.waitTimer = FieldRun.STORE_RETRY_TICKS;
                    if (anyStorable(shuttle)) {
                        continueStore(shuttle);
                    }
                }
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
        if (!host.isTravelNode(shuttle.at())) {
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

    /** Whether a source the shuttle cannot drive to hands its items over at once: never a rack, never by lift for a liftless shuttle. */
    private boolean handsOverAtOnce(Shuttle shuttle, BlockPos source, Set<NodePos> dock) {
        return !host.isRack(source) && (dock.isEmpty() || !shuttle.profile.liftless());
    }

    /** Adds what was taken from the source to the cargo; what does not fit goes back. */
    private void stow(FieldRun run, BlockPos source, ItemKey key, ItemStack taken, int max) {
        if (taken.isEmpty()) {
            return;
        }
        for (FieldCargo.Entry<ItemKey> left : run.cargo.add(List.of(new FieldCargo.Entry<>(key, taken.getCount(), max)))) {
            host.insertAt(source, left.key().stack(left.count()));
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
        if (!host.isTravelNode(shuttle.at())) {
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
                    head.supplyCount(SupplyBuffer.Part.SEEDS), Integer.MAX_VALUE, max);
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
                shuttle.loads.clear();
                shuttle.run = null;
                fleet.goHome(shuttle);
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
                    loadSeeds(run, head);
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
        if (!host.isTravelNode(shuttle.at())) {
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
            addOrDrop(run, drops, plot);
        }
        if (action.till()) {
            changed |= HarvesterWork.till(level, plot);
        }
        if (crop != null && (action.plant() || seedInHand)) {
            if (seedInHand) {
                if (HarvesterWork.plant(level, plot, crop.stack(1))) {
                    changed = true;
                } else {
                    addOrDrop(run, List.of(new FieldCargo.Entry<>(crop, 1, crop.stack(1).getMaxStackSize())), plot);
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

    /** Adds the entries to the cargo and pops what does not fit at the plot. */
    private void addOrDrop(HarvestRun run, List<FieldCargo.Entry<ItemKey>> entries, BlockPos plot) {
        for (FieldCargo.Entry<ItemKey> left : run.cargo.add(entries)) {
            Block.popResource(host.level(), plot, left.key().stack(left.count()));
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

    /** Drives to the farm rail beside the head for seeds from its buffer; standing there it takes them at once. */
    private void fetchSeeds(Shuttle shuttle, FieldHeadBlockEntity head) {
        HarvestRun run = shuttle.run;
        run.fetchTried = true;
        if (atHead(shuttle, head)) {
            loadSeeds(run, head);
        } else if (goToHead(shuttle, head)) {
            return;
        }
        turn(shuttle);
    }

    /** Takes the seeds the run wants out of the head's buffer, as far as the cargo has room. */
    private static void loadSeeds(HarvestRun run, FieldHeadBlockEntity head) {
        ItemKey crop = cropKey(head);
        if (crop == null || run.seedAmount <= 0) {
            return;
        }
        int max = crop.stack(1).getMaxStackSize();
        int taken = head.takeSupply(SupplyBuffer.Part.SEEDS, Math.min(run.seedAmount, run.cargo.room(crop, max)));
        run.seedAmount = 0;
        run.cargo.add(List.of(new FieldCargo.Entry<>(crop, taken, max)));
    }

    /** Puts the stacks on their way into storage one target after another, then decides again. */
    private void continueStore(Shuttle shuttle) {
        FieldRun run = shuttle.fieldRun();
        run.store.removeIf(ItemStack::isEmpty);
        Predicate<BlockPos> reachable = storable(shuttle, shuttle.at());
        int guard = run.store.size() * 2 + 4;
        while (!run.store.isEmpty() && guard-- > 0) {
            BlockPos target = null;
            int index = 0;
            for (; index < run.store.size(); index++) {
                Optional<BlockPos> found = host.putAwayTarget(run.store.get(index), reachable);
                if (found.isPresent()) {
                    target = found.get();
                    break;
                }
            }
            if (target == null) {
                waitStore(shuttle);
                return;
            }
            List<NodePos> path = pathTo(shuttle, host.dockRails(Conversions.toNodePos(target)));
            if (!path.isEmpty()) {
                run.storeTarget = target;
                shuttle.leg = Leg.TO_STORE;
                shuttle.motion = new ShuttleMotion(path);
                fleet.markSync();
                return;
            }
            // No rail reaches the target: it takes its part at once.
            run.store.set(index, host.insertAt(target, run.store.get(index)));
            run.store.removeIf(ItemStack::isEmpty);
        }
        if (!run.store.isEmpty()) {
            waitStore(shuttle);
            return;
        }
        if (shuttle.supply != null) {
            supplyTurn(shuttle);
        } else if (shuttle.spread != null) {
            spreadTurn(shuttle);
        } else {
            turn(shuttle);
        }
    }

    /** Inserts every stack on its way into storage whose put-away routing picks this target, so zones keep applying. */
    private void insertAll(FieldRun run, BlockPos target, Predicate<BlockPos> reachable) {
        for (int i = 0; i < run.store.size(); i++) {
            ItemStack stack = run.store.get(i);
            if (host.putAwayTarget(stack, reachable).filter(target::equals).isPresent()) {
                run.store.set(i, host.insertAt(target, stack));
            }
        }
        run.store.removeIf(ItemStack::isEmpty);
    }

    /**
     * Storage the shuttle standing there may put away into: a block it can
     * drive to, or one no rail reaches, which takes its part at once. Racks
     * need a way; a shuttle too wide for lifts skips what it reaches only by lift.
     */
    private Predicate<BlockPos> storable(Shuttle shuttle, NodePos from) {
        return target -> {
            Set<NodePos> dock = host.dockRails(Conversions.toNodePos(target));
            if (dock.isEmpty()) {
                return !host.isRack(target);
            }
            if (!RailPaths.find(from, dock::contains, fleet.ways(shuttle)).isEmpty()) {
                return true;
            }
            return !host.isRack(target) && !shuttle.profile.liftless();
        };
    }

    private boolean anyStorable(Shuttle shuttle) {
        Predicate<BlockPos> reachable = storable(shuttle, shuttle.at());
        for (ItemStack stack : shuttle.fieldRun().store) {
            if (host.putAwayTarget(stack, reachable).isPresent()) {
                return true;
            }
        }
        return false;
    }

    /** Nothing has room for the cargo: drive home with it and try again now and then. */
    private void waitStore(Shuttle shuttle) {
        FieldRun run = shuttle.fieldRun();
        run.waitTimer = FieldRun.STORE_RETRY_TICKS;
        NodePos home = shuttle.garage.home;
        if (shuttle.leg == Leg.WAIT_STORE && shuttle.motion.arrived()) {
            return;
        }
        List<NodePos> path = home == null ? List.of() : fleet.path(shuttle, shuttle.at(), home::equals);
        if (path.isEmpty()) {
            lose(shuttle);
            return;
        }
        shuttle.leg = Leg.WAIT_STORE;
        shuttle.motion = new ShuttleMotion(path);
        fleet.markSync();
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
        if (!host.isTravelNode(shuttle.at())) {
            lose(shuttle);
            return;
        }
        FieldHeadBlockEntity head = spreadHead(run);
        if (head != null && !run.fetched) {
            if (run.toFetch < 0) {
                int max = run.fertilizer.stack(1).getMaxStackSize();
                run.toFetch = FertilizePlan.toFetch(head.fertilizeTargetCount(), run.doses, head.supplyCount(SupplyBuffer.Part.FERTILIZER),
                        0, run.cargo.room(run.fertilizer, max));
            }
            if (run.toFetch > 0) {
                if (atHead(shuttle, head)) {
                    loadFertilizer(run, head);
                } else if (goToHead(shuttle, head)) {
                    return;
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
        shuttle.loads.clear();
        shuttle.spread = null;
        fleet.goHome(shuttle);
    }

    /** Takes the fertilizer still to fetch out of the head's buffer, as far as the cargo has room. */
    private static void loadFertilizer(SpreadRun run, FieldHeadBlockEntity head) {
        int max = run.fertilizer.stack(1).getMaxStackSize();
        int taken = head.takeSupply(SupplyBuffer.Part.FERTILIZER, Math.min(run.toFetch, run.cargo.room(run.fertilizer, max)));
        run.toFetch = 0;
        run.cargo.add(List.of(new FieldCargo.Entry<>(run.fertilizer, taken, max)));
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
                    loadFertilizer(run, head);
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
        if (!host.isTravelNode(shuttle.at())) {
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
        planSupply(run, head);
        shuttle.supply = run;
        supplyTurn(shuttle);
    }

    /**
     * Plans what the run picks for each part of the buffer: what it is short
     * of its level, low parts first, as far as the cargo has room.
     */
    private void planSupply(SupplyRun run, FieldHeadBlockEntity head) {
        for (SupplyBuffer.Part part : SupplyBuffer.Part.VALUES) {
            run.reserves.put(part, head.supplyReserve(part));
        }
        FieldCargo<ItemKey> planned = new FieldCargo<>(run.cargo.slots());
        for (boolean lowPass : new boolean[] {true, false}) {
            for (SupplyBuffer.Part part : SupplyBuffer.Part.VALUES) {
                Item item = head.supplyItem(part);
                int held = head.supplyCount(part);
                int level = head.supplyLevel(part);
                if (item == null || run.keys.containsKey(part) || SupplyPlan.low(held, level) != lowPass) {
                    continue;
                }
                ItemKey key = ItemKey.of(new ItemStack(item));
                int max = key.stack(1).getMaxStackSize();
                int amount = SupplyPlan.toBring(held, level, host.available(key), run.reserveOf(part), planned.room(key, max));
                if (amount > 0) {
                    planned.add(List.of(new FieldCargo.Entry<>(key, amount, max)));
                    run.keys.put(part, key);
                    run.remaining.put(part, amount);
                }
            }
        }
    }

    /**
     * Decides what a supply run does next: pick the planned items, stop at
     * the head's rail to fill the buffer and load stale items, then put what
     * it carries away and go home.
     */
    private void supplyTurn(Shuttle shuttle) {
        SupplyRun run = shuttle.supply;
        fleet.markSync();
        if (!host.isTravelNode(shuttle.at())) {
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
                unloadAtHead(run, head);
            } else if (goToHead(shuttle, head)) {
                return;
            }
        }
        run.unloaded = true;
        if (!run.cargo.isEmpty() || !run.store.isEmpty()) {
            unloadToStore(run, null, 0);
            continueStore(shuttle);
            return;
        }
        shuttle.loads.clear();
        shuttle.supply = null;
        fleet.goHome(shuttle);
    }

    /** Drives to the next storage block holding a planned item; blocks no rail reaches give their part at once. False when none is left. */
    private boolean fetchSupply(Shuttle shuttle) {
        SupplyRun run = shuttle.supply;
        for (Map.Entry<SupplyBuffer.Part, ItemKey> planned : run.keys.entrySet()) {
            if (run.remaining.getOrDefault(planned.getKey(), 0) <= 0) {
                continue;
            }
            for (BlockPos source : host.sourcesHolding(planned.getValue())) {
                if (!run.sourcesTried.add(source)) {
                    continue;
                }
                Set<NodePos> dock = host.dockRails(Conversions.toNodePos(source));
                List<NodePos> path = pathTo(shuttle, dock);
                if (!path.isEmpty()) {
                    run.source = source;
                    shuttle.leg = Leg.TO_SUPPLY;
                    shuttle.motion = new ShuttleMotion(path);
                    return true;
                }
                if (handsOverAtOnce(shuttle, source, dock)) {
                    loadSupply(run, source);
                }
            }
        }
        return false;
    }

    /** Takes what the run still picks of every planned item from the source, never the stock at or below the reserve. */
    private void loadSupply(SupplyRun run, BlockPos source) {
        for (Map.Entry<SupplyBuffer.Part, ItemKey> planned : run.keys.entrySet()) {
            SupplyBuffer.Part part = planned.getKey();
            ItemKey key = planned.getValue();
            int left = run.remaining.getOrDefault(part, 0);
            int max = key.stack(1).getMaxStackSize();
            int amount = SupplyPlan.toBring(0, left, host.available(key), run.reserveOf(part), run.cargo.room(key, max));
            if (amount <= 0) {
                continue;
            }
            ItemStack taken = host.extractFrom(source, key, amount);
            run.remaining.put(part, left - taken.getCount());
            stow(run, source, key, taken, max);
        }
    }

    private void arriveSupply(Shuttle shuttle) {
        SupplyRun run = shuttle.supply;
        if (shuttle.leg == Leg.TO_SUPPLY && run.source != null && servingHead(run.head) != null) {
            loadSupply(run, run.source);
        }
        run.source = null;
        supplyTurn(shuttle);
    }

    /** At the head's rail: stale items come aboard to be put away, then the cargo fills the buffer as far as it takes it. */
    private static void unloadAtHead(FieldRun run, FieldHeadBlockEntity head) {
        run.store.addAll(head.takeStale());
        for (FieldCargo.Entry<ItemKey> entry : run.cargo.entries()) {
            for (SupplyBuffer.Part part : SupplyBuffer.Part.VALUES) {
                fillFromCargo(run, head, part, entry.key());
            }
        }
    }

    /** Moves as much of the key from the cargo into the part as it takes; returns how many moved. */
    private static int fillFromCargo(FieldRun run, FieldHeadBlockEntity head, SupplyBuffer.Part part, ItemKey key) {
        int carried = run.cargo.count(key);
        if (carried <= 0 || !key.components().isEmpty()) {
            return 0;
        }
        int moved = head.fillSupply(part, key.item(), carried);
        run.cargo.take(key, moved);
        return moved;
    }
}
