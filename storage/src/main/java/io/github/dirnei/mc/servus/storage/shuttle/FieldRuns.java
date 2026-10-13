package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.storage.field.FertilizePlan;
import io.github.dirnei.mc.servus.storage.field.FieldCargo;
import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.storage.field.FieldRoute;
import io.github.dirnei.mc.servus.storage.field.FieldStatus;
import io.github.dirnei.mc.servus.storage.field.HarvestStep;
import io.github.dirnei.mc.servus.storage.field.HarvesterWork;
import io.github.dirnei.mc.servus.storage.field.PlotState;
import io.github.dirnei.mc.servus.storage.field.PlotWork;
import io.github.dirnei.mc.servus.storage.field.SeedPlan;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleTiers;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;

/**
 * The legs of field runs: a harvester harvests, tills, plants and fetches
 * seeds, a Spreader fetches fertilizer and doses plots, and both put their
 * cargo away. Called by the fleet when a field run starts or its shuttle arrives.
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
        return ShuttleTiers.workTicks(StorageStatsManager.INSTANCE.fieldWorkTicks(), shuttle.profile.handling());
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
                if (shuttle.spread != null) {
                    arriveSpread(shuttle);
                } else {
                    arriveHarvest(shuttle);
                }
            }
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
            List<NodePos> path = RailPaths.find(from, hasWork, fleet.ways(shuttle));
            stop = path.isEmpty() ? Optional.empty() : Optional.of(path.get(path.size() - 1));
        }
        if (stop.isEmpty()) {
            return false;
        }
        NodePos goal = stop.get();
        List<NodePos> path = RailPaths.find(from, goal::equals, fleet.ways(shuttle));
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
        return dock.isEmpty() ? List.of() : RailPaths.find(shuttle.at(), dock::contains, fleet.ways(shuttle));
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
            if (PlotWork.plan(head.plotState(plot), plants, haveSeed).any()) {
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
        boolean workLeft = head != null && workLeft(run, head);
        int wanted = 0;
        boolean room = false;
        ItemKey crop = head == null ? null : cropKey(head);
        if (crop != null && plants(head) && !run.fetchTried && !run.idle) {
            int max = crop.stack(1).getMaxStackSize();
            int cargoRoom = run.cargo.room(crop, max);
            wanted = SeedPlan.toFetch(head.ripeCount(), head.plantableCount() + head.tillableCount(), run.cargo.count(crop),
                    host.available(crop), Integer.MAX_VALUE, max);
            room = cargoRoom > 0;
            run.seedAmount = Math.min(wanted, cargoRoom);
            if (!room && run.cargo.count(crop) == run.cargoCount()) {
                // Full of seeds: a put-away would keep them all again.
                wanted = 0;
            }
        }
        switch (HarvestStep.next(head != null, run.cargoFull, wanted, room, workLeft, !run.cargo.isEmpty())) {
            case FETCH_SEEDS -> fetchSeeds(shuttle, crop);
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
                shuttle.loads.clear();
                shuttle.run = null;
                fleet.goHome(shuttle);
            }
        }
    }

    /**
     * Puts the cargo away; while plots remain to plant, up to a stack of seeds
     * stays aboard, no more than those plots need.
     */
    private void putAwayHarvest(Shuttle shuttle, FieldHeadBlockEntity head, ItemKey crop, boolean plotsLeft) {
        HarvestRun run = shuttle.run;
        boolean keep = crop != null && plotsLeft && !run.keepNothing;
        int keepMax = keep ? Math.min(crop.stack(1).getMaxStackSize(), head.plantableCount() + head.tillableCount()) : 0;
        unloadToStore(run, keepMax > 0 ? crop : null, keepMax);
        run.cargoFull = false;
        run.keepNothing = false;
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
            case TO_SEEDS -> {
                if (run.seedSource != null) {
                    loadSeeds(run, run.seedSource);
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
                if (PlotWork.plan(state, run.stopPlants, haveSeed).any()) {
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
        PlotWork.Action action = PlotWork.plan(state, plants, carriesSeed(run, crop));
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

    /** Drives to a source of the crop item; a source no rail reaches gives its seeds at once. */
    private void fetchSeeds(Shuttle shuttle, ItemKey crop) {
        HarvestRun run = shuttle.run;
        run.fetchTried = true;
        for (BlockPos source : host.sourcesHolding(crop)) {
            Set<NodePos> dock = host.dockRails(Conversions.toNodePos(source));
            List<NodePos> path = pathTo(shuttle, dock);
            if (!path.isEmpty()) {
                run.seedSource = source;
                shuttle.leg = Leg.TO_SEEDS;
                shuttle.motion = new ShuttleMotion(path);
                return;
            }
            if (handsOverAtOnce(shuttle, source, dock)) {
                loadSeeds(run, source);
                if (run.seedAmount <= 0) {
                    break;
                }
            }
        }
        run.seedSource = null;
        turn(shuttle);
    }

    private void loadSeeds(HarvestRun run, BlockPos source) {
        FieldHeadBlockEntity head = servingHead(run.head);
        ItemKey crop = head == null ? null : cropKey(head);
        if (crop == null || run.seedAmount <= 0) {
            return;
        }
        ItemStack taken = host.extractFrom(source, crop, run.seedAmount);
        run.seedAmount -= taken.getCount();
        stow(run, source, crop, taken, taken.getMaxStackSize());
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
        if (shuttle.spread != null) {
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
        List<NodePos> path = home == null ? List.of() : RailPaths.find(shuttle.at(), home::equals, fleet.ways(shuttle));
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
                StorageStatsManager.INSTANCE.dosesPerPlot());
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
     * Decides what a Spreader does next: pick fertilizer above the reserve
     * while the fetch is not over, dose the next rail with plots that need a
     * dose while it carries fertilizer, else put what it carries away and go home.
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
                run.toFetch = FertilizePlan.toFetch(head.fertilizeTargetCount(), run.doses, host.available(run.fertilizer),
                        head.reserve(), run.cargo.room(run.fertilizer, max));
            }
            if (run.toFetch > 0 && fetchFertilizer(shuttle, head)) {
                return;
            }
            run.fetched = true;
        }
        if (head != null && run.cargo.count(run.fertilizer) > 0 && goToDose(shuttle, head)) {
            return;
        }
        if (!run.cargo.isEmpty()) {
            unloadToStore(run, null, 0);
            continueStore(shuttle);
            return;
        }
        shuttle.loads.clear();
        shuttle.spread = null;
        fleet.goHome(shuttle);
    }

    /** Drives to the next storage block holding the fertilizer; blocks no rail reaches give their part at once. False when none is left. */
    private boolean fetchFertilizer(Shuttle shuttle, FieldHeadBlockEntity head) {
        SpreadRun run = shuttle.spread;
        for (BlockPos source : host.sourcesHolding(run.fertilizer)) {
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
                loadFertilizer(run, head, source);
                if (run.toFetch <= 0) {
                    return false;
                }
            }
        }
        return false;
    }

    /** Takes the fertilizer still to fetch from the source, never the stock at or below the reserve. */
    private void loadFertilizer(SpreadRun run, FieldHeadBlockEntity head, BlockPos source) {
        int max = run.fertilizer.stack(1).getMaxStackSize();
        int amount = FertilizePlan.toFetch(1, run.toFetch, host.available(run.fertilizer), head.reserve(), run.cargo.room(run.fertilizer, max));
        if (amount <= 0) {
            run.toFetch = 0;
            return;
        }
        ItemStack taken = host.extractFrom(source, run.fertilizer, amount);
        run.toFetch -= taken.getCount();
        stow(run, source, run.fertilizer, taken, max);
    }

    private void arriveSpread(Shuttle shuttle) {
        SpreadRun run = shuttle.spread;
        switch (shuttle.leg) {
            case TO_SUPPLY -> {
                FieldHeadBlockEntity head = spreadHead(run);
                if (run.source != null && head != null) {
                    loadFertilizer(run, head, run.source);
                }
                run.source = null;
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
}
