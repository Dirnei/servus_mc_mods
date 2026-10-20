package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork.FluidKey;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryMode;
import io.github.dirnei.mc.servus.shuttles.gantry.TankRef;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.power.StepPower;
import io.github.dirnei.mc.servus.shuttles.shuttle.Shafts.Shaft;
import io.github.dirnei.mc.servus.shuttles.shuttle.Shuttle.Load;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.CargoBatch;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.JobNeeds;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleProfile;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleTiers;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The shuttles of one warehouse, kept per garage and per garage slot: each
 * parks on its garage's rail, all serve one queue of jobs as far as their
 * modules fit, and clients draw them per garage. Everything here is data;
 * nothing scans blocks.
 */
public final class ShuttleFleet {

    /** What the fleet needs from its warehouse. */
    public interface Host {
        ServerLevel level();

        /** Rails and elevator segments the graph links to this node. */
        Set<NodePos> travelNeighbours(NodePos node);

        /** Whether the node is a rail or an elevator segment of this warehouse. */
        boolean isTravelNode(NodePos node);

        /**
         * Travel nodes a shuttle may stand on to serve the block at that
         * position: the front rails of a rack's section, every touching rail
         * for other blocks.
         */
        Set<NodePos> dockRails(NodePos storage);

        /** Positions of storage blocks holding the key right now. */
        List<BlockPos> sourcesHolding(ItemKey key);

        /** Takes up to amount of the key out of exactly that source. */
        ItemStack extractFrom(BlockPos source, ItemKey key, int amount);

        /** Takes up to amount of the key out of a station's buffer and frees the slot's claim. */
        ItemStack takeFromStation(BlockPos station, int slot, ItemKey key, int amount);

        /** Inserts into exactly that storage block; returns what did not fit. */
        ItemStack insertAt(BlockPos target, ItemStack stack);

        /** Puts a stack back into a station's buffer, dropping what does not fit there. */
        void returnToStation(BlockPos station, ItemStack stack);

        /** Frees a station slot a job no longer waits for. */
        void releaseClaim(BlockPos station, int slot);

        /** Whether the source is a Heavy Duty Rack, which only shuttles serve. */
        boolean isRack(BlockPos source);

        /** A pick took this many from its source; the shortfall counts as missing. */
        void pickLoaded(PickJob job, int loaded);

        /** A loaded pick was lost with its garage; the count counts as missing. */
        void pickDropped(PickJob job, int count);

        /** Puts a load into the terminal's output slots; returns what did not fit. */
        ItemStack deliverTo(PickJob job, ItemStack stack);

        /** Puts an instant load into the output slots, holding what does not fit until there is room. */
        void deliverInstant(PickJob job, ItemStack stack);

        /** Blocks per tick the lift car of a shaft moves. */
        double liftSpeed();

        /** The shaft the node is a segment of, or null when it is not an elevator segment. */
        Shaft shaftOf(NodePos node);

        /** The block the put-away routing picks for the stack among those allowed; empty when nothing allowed has room. */
        Optional<BlockPos> putAwayTarget(ItemStack stack, Predicate<BlockPos> allowed);

        /** How many of the key the warehouse holds. */
        long available(ItemKey key);

        /** The mode of the gantry whose master is at that position, empty when none is loaded there. */
        Optional<GantryMode> gantryMode(BlockPos gantry);

        /** Takes up to max of the fluid out of a gantry tank; empty when the tank holds another fluid. */
        FluidStack drainGantry(BlockPos gantry, int tank, FluidKey key, int max);

        /** Puts fluid into a gantry tank as its lock allows; returns what did not fit. */
        FluidStack fillGantry(BlockPos gantry, int tank, FluidStack stack);

        /** The Storage gantry tanks a Tanker reaches with room for the fluid, best first by lock, then by the way from that gantry. */
        List<TankRef> fluidTargets(FluidKey key, BlockPos from);

        /** A tanker's stop under a gantry tank starts or ends; the spout pumps into the gantry or out of it while on. */
        void gantryPumping(BlockPos gantry, int tank, boolean intoGantry, FluidStack fluid, boolean on);

        /** Draws FE from the warehouse's controller: paid in full or refused. */
        default boolean drawPower(long amount) {
            return true;
        }

        /** Whether the warehouse is in the no power state; no new job starts while it is. */
        default boolean noPower() {
            return false;
        }
    }

    /** A free shuttle and its path to a job's first goal. */
    private record Route(Shuttle shuttle, List<NodePos> path) {
    }

    private static final int SYNC_INTERVAL = 5;
    /** Garage slots the out mask can tell apart; container data travels as a short. */
    public static final int MASK_SLOTS = 15;

    private final Host host;
    private final Map<BlockPos, Garage> garages = new LinkedHashMap<>();
    private final ArrayDeque<ShuttleJob> jobs = new ArrayDeque<>();
    private final ShuttleLifts lifts;
    private final FieldRuns fieldRuns;
    private boolean syncPending;
    private boolean wasMoving;
    private int syncTimer;

    public ShuttleFleet(Host host) {
        this.host = host;
        this.lifts = new ShuttleLifts(this, host);
        this.fieldRuns = new FieldRuns(this, host);
    }

    void markSync() {
        syncPending = true;
    }

    private List<Shuttle> shuttles() {
        List<Shuttle> all = new ArrayList<>();
        for (Garage garage : garages.values()) {
            all.addAll(garage.shuttles);
        }
        return all;
    }

    private Shuttle findShuttle(Predicate<Shuttle> test) {
        for (Shuttle shuttle : shuttles()) {
            if (test.test(shuttle)) {
                return shuttle;
            }
        }
        return null;
    }

    private static <T> Set<T> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    public int size() {
        return shuttles().size();
    }

    public int busy() {
        int count = 0;
        for (Shuttle shuttle : shuttles()) {
            if (shuttle.busy()) {
                count++;
            }
        }
        return count;
    }

    /** Shuttles whose garage touches no rail. */
    public int withoutRail() {
        int count = 0;
        for (Garage garage : garages.values()) {
            if (garage.home == null) {
                count += garage.shuttles.size();
            }
        }
        return count;
    }

    /** Whether any garage with a rail has a shuttle that takes warehouse jobs. */
    public boolean hasShuttles() {
        return anyFits(JobNeeds.WAREHOUSE);
    }

    /** Whether any garage with a rail, paused or not, has a shuttle the needs fit. */
    private boolean anyFits(JobNeeds needs) {
        for (Garage garage : garages.values()) {
            if (garage.home != null && garage.fits(needs)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Called when a garage joins or its slots or pause state change: one
     * stack per slot, empty for an empty one. A slot whose shuttle does the
     * same keeps it; an emptied slot loses it; a shuttle that does something
     * else now is recalled first.
     */
    public void adoptGarage(BlockPos pos, List<ItemStack> slots, boolean active) {
        Garage garage = garages.computeIfAbsent(pos, Garage::new);
        garage.active = active;
        refreshHome(garage);
        ShuttleTiers tiers = ShuttlesStatsManager.INSTANCE.shuttleTiers();
        for (Shuttle shuttle : new ArrayList<>(garage.shuttles)) {
            ItemStack stack = shuttle.slot < slots.size() ? slots.get(shuttle.slot) : ItemStack.EMPTY;
            if (stack.isEmpty()) {
                garage.shuttles.remove(shuttle);
                lifts.release(shuttle);
                dropCargo(shuttle);
                continue;
            }
            ShuttleProfile profile = ShuttleModules.profile(stack, tiers);
            if (!profile.equals(shuttle.profile)) {
                recallShuttle(shuttle);
                shuttle.profile = profile;
            }
            shuttle.item = stack.copyWithCount(1);
        }
        for (int slot = 0; slot < slots.size(); slot++) {
            ItemStack stack = slots.get(slot);
            if (!stack.isEmpty() && garage.inSlot(slot) == null) {
                Shuttle shuttle = new Shuttle(garage, slot, ShuttleModules.profile(stack, tiers));
                shuttle.item = stack.copyWithCount(1);
                shuttle.motion = garage.home == null ? null : ShuttleMotion.parked(garage.home);
                garage.shuttles.add(shuttle);
            }
        }
        garage.shuttles.sort(Comparator.comparingInt(shuttle -> shuttle.slot));
        syncPending = true;
    }

    /** Called when a garage leaves: cargo drops there, jobs go back to the queue, the shuttles are forgotten. */
    public void releaseGarage(BlockPos pos) {
        Garage garage = garages.remove(pos);
        if (garage == null) {
            return;
        }
        for (Shuttle shuttle : garage.shuttles) {
            lifts.release(shuttle);
            dropCargo(shuttle);
        }
        garage.shuttles.clear();
        syncPending = true;
    }

    /** Takes over another fleet's garages and queue, for network merges. */
    public void absorb(ShuttleFleet other) {
        garages.putAll(other.garages);
        jobs.addAll(other.jobs);
        lifts.absorb(other.lifts);
        other.garages.clear();
        other.jobs.clear();
        refreshHomes();
        syncPending = true;
    }

    /** Recomputes every garage's home rail; call after membership changes. */
    public void refreshHomes() {
        for (Garage garage : garages.values()) {
            refreshHome(garage);
        }
    }

    /** Sets the garage's home to its first travel neighbour; a garage that lost its rail recalls its shuttles. */
    private void refreshHome(Garage garage) {
        NodePos previous = garage.home;
        Set<NodePos> neighbours = host.travelNeighbours(Conversions.toNodePos(garage.pos));
        garage.home = neighbours.isEmpty() ? null : neighbours.iterator().next();
        if (garage.home == previous) {
            return;
        }
        if (garage.home == null) {
            recallShuttles(garage);
            return;
        }
        for (Shuttle shuttle : garage.shuttles) {
            if (!shuttle.busy()) {
                lifts.release(shuttle);
                shuttle.motion = ShuttleMotion.parked(garage.home);
            }
        }
        syncPending = true;
    }

    /** Brings every shuttle of a garage inside at once: cargo drops there, unloaded jobs go back to the queue. */
    public void recall(BlockPos pos) {
        Garage garage = garages.get(pos);
        if (garage != null) {
            recallShuttles(garage);
        }
    }

    private void recallShuttles(Garage garage) {
        for (Shuttle shuttle : garage.shuttles) {
            recallShuttle(shuttle);
        }
    }

    private void recallShuttle(Shuttle shuttle) {
        lifts.release(shuttle);
        dropCargo(shuttle);
        shuttle.leg = Leg.HOME;
        shuttle.motion = shuttle.garage.home == null ? null : ShuttleMotion.parked(shuttle.garage.home);
        syncPending = true;
    }

    /** Whether some garage with a rail and a shuttle for warehouse jobs can drive to a dock rail of the position. */
    public boolean canReach(BlockPos pos) {
        Set<NodePos> rails = host.dockRails(Conversions.toNodePos(pos));
        if (rails.isEmpty()) {
            return false;
        }
        for (Garage garage : garages.values()) {
            if (garage.home != null && garage.fits(JobNeeds.WAREHOUSE)
                    && !RailPaths.find(garage.home, rails::contains, host::travelNeighbours).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Whether a put-away from the station to the target rides a shuttle. */
    public boolean canCarry(BlockPos station, BlockPos target) {
        return canReach(station) && canReach(target);
    }

    /**
     * The steps the shuttle may take from a node: every travel neighbour, or
     * for a shuttle too wide for lifts those that do not go up or down a shaft.
     */
    Function<NodePos, Set<NodePos>> ways(Shuttle shuttle) {
        if (shuttle.profile.liftless()) {
            return node -> LiftlessSteps.filter(node, host.travelNeighbours(node));
        }
        return host::travelNeighbours;
    }

    public void requestPick(PickJob job) {
        jobs.add(job);
        syncPending = true;
    }

    public void requestPutAway(PutAwayJob job) {
        jobs.add(job);
        syncPending = true;
    }

    private Set<NodePos> firstGoals(ShuttleJob job) {
        return switch (job) {
            case PickJob pick -> host.dockRails(Conversions.toNodePos(pick.source()));
            case PutAwayJob putAway -> host.dockRails(Conversions.toNodePos(putAway.station()));
            case FieldJob field -> fieldRuns.fieldRails(field.head());
            case FertilizeJob fertilize -> fieldRuns.fieldRails(fertilize.head());
            case SupplyJob supply -> fieldRuns.headRails(supply.head());
            case FluidJob fluid -> host.dockRails(Conversions.toNodePos(fluid.source()));
        };
    }

    private int sumQueued(ToIntFunction<ShuttleJob> value) {
        int sum = 0;
        for (ShuttleJob job : jobs) {
            sum += value.applyAsInt(job);
        }
        return sum;
    }

    private int sumRiding(ToIntFunction<ShuttleJob> value) {
        int sum = 0;
        for (Shuttle shuttle : shuttles()) {
            for (Load load : shuttle.loads) {
                sum += value.applyAsInt(load.job);
            }
        }
        return sum;
    }

    private static int amountFor(ShuttleJob job, BlockPos destination, ItemKey key) {
        return job instanceof PickJob pick && pick.terminal().equals(destination) && pick.key().equals(key) ? pick.amount() : 0;
    }

    /** Items of the key on queued picks bound for a destination. */
    public int queuedAmount(BlockPos destination, ItemKey key) {
        return sumQueued(job -> amountFor(job, destination, key));
    }

    /** Items of the key on picks a shuttle is working on, bound for a destination. */
    public int ridingAmount(BlockPos destination, ItemKey key) {
        return sumRiding(job -> amountFor(job, destination, key));
    }

    /** Drops the queued picks of a request; loads already on a shuttle still arrive. */
    public void removeQueued(int requestId) {
        jobs.removeIf(job -> job instanceof PickJob pick && pick.requestId() == requestId);
    }

    /** Queued jobs that are put-aways. */
    public int queuedPutAways() {
        return sumQueued(job -> job instanceof PutAwayJob ? 1 : 0);
    }

    public int queued() {
        return jobs.size();
    }

    /** Jobs shuttles are carrying out right now, counting every load of a Cargo Hold. */
    public int riding() {
        return sumRiding(job -> 1);
    }

    /** Queues a field run for the field of that head. */
    public void requestField(FieldJob job) {
        jobs.add(job);
        syncPending = true;
    }

    /** Queues a fertilize run for the field of that head. */
    public void requestFertilize(FertilizeJob job) {
        jobs.add(job);
        syncPending = true;
    }

    /** Queues a supply run for the buffer of that head. */
    public void requestSupply(SupplyJob job) {
        jobs.add(job);
        syncPending = true;
    }

    /** Whether a supply run for that head is queued or under way. */
    public boolean hasSupplyJob(BlockPos head) {
        return sumQueued(job -> job instanceof SupplyJob supply && supply.head().equals(head) ? 1 : 0) > 0
                || findShuttle(shuttle -> shuttle.supply != null && shuttle.supply.head.equals(head)) != null;
    }

    /**
     * Whether some shuttle for warehouse jobs in a garage with a rail can
     * drive from its garage to the farm rail beside that head, lifts allowed.
     */
    public boolean supplyReachable(BlockPos head) {
        return reachable(fieldRuns.headRails(head), JobNeeds.WAREHOUSE);
    }

    /** Whether some shuttle the needs fit, in a garage with a rail, can drive from its garage to one of the rails. */
    private boolean reachable(Set<NodePos> rails, JobNeeds needs) {
        if (rails.isEmpty()) {
            return false;
        }
        for (Garage garage : garages.values()) {
            if (garage.home == null) {
                continue;
            }
            for (Shuttle shuttle : garage.shuttles) {
                if (needs.fits(shuttle.profile)
                        && !RailPaths.find(garage.home, rails::contains, ways(shuttle)).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether some garage with a rail has a shuttle with a Harvester. */
    public boolean hasFieldShuttles() {
        return hasFieldShuttles(JobNeeds.HARVESTER);
    }

    /** Whether some garage with a rail has a shuttle for field jobs with these needs. */
    public boolean hasFieldShuttles(JobNeeds needs) {
        return anyFits(needs);
    }

    /**
     * Whether some Harvester shuttle in a garage with a rail can drive from
     * its garage to a rail of that head's field, without a lift when it is too
     * wide for one.
     */
    public boolean fieldReachable(BlockPos head) {
        return fieldReachable(head, JobNeeds.HARVESTER);
    }

    /** Like {@link #fieldReachable(BlockPos)} for shuttles with these needs. */
    public boolean fieldReachable(BlockPos head, JobNeeds needs) {
        return reachable(fieldRuns.fieldRails(head), needs);
    }

    /** Whether a field run for that head is queued or under way. */
    public boolean hasFieldJob(BlockPos head) {
        return sumQueued(job -> job instanceof FieldJob field && field.head().equals(head) ? 1 : 0) > 0
                || harvesting(head) != null;
    }

    /** Whether a fertilize run for that head is queued or under way. */
    public boolean hasFertilizeJob(BlockPos head) {
        return sumQueued(job -> job instanceof FertilizeJob fertilize && fertilize.head().equals(head) ? 1 : 0) > 0
                || spreading(head) != null;
    }

    /** The item of the shuttle whose run serves that head, or empty when none is out for it. */
    public Optional<ItemStack> fieldWorker(BlockPos head) {
        return Optional.ofNullable(harvesting(head)).map(shuttle -> shuttle.item);
    }

    /** The item of the Spreader whose run serves that head, or empty when none is out for it. */
    public Optional<ItemStack> spreadWorker(BlockPos head) {
        return Optional.ofNullable(spreading(head)).map(shuttle -> shuttle.item);
    }

    private Shuttle harvesting(BlockPos head) {
        return findShuttle(shuttle -> shuttle.run != null && shuttle.run.head.equals(head));
    }

    private Shuttle spreading(BlockPos head) {
        return findShuttle(shuttle -> shuttle.spread != null && shuttle.spread.head.equals(head));
    }

    /** Queues a tanker run. */
    public void requestFluid(FluidJob job) {
        jobs.add(job);
        syncPending = true;
    }

    /** Whether some Tanker shuttle in a garage with a rail can drive to the rail under that gantry, lifts allowed. */
    public boolean tankerReaches(BlockPos gantry) {
        return reachable(host.dockRails(Conversions.toNodePos(gantry)), JobNeeds.TANKER);
    }

    /** The most fluid one Tanker shuttle of a garage with a rail carries, 0 without any. */
    public int tankerCapacity() {
        int most = 0;
        for (Garage garage : garages.values()) {
            if (garage.home == null) {
                continue;
            }
            for (Shuttle shuttle : garage.shuttles) {
                if (JobNeeds.TANKER.fits(shuttle.profile)) {
                    most = Math.max(most, shuttle.profile.fluidCapacity());
                }
            }
        }
        return most;
    }

    /** Whether a tanker run from that tank is queued or under way. */
    public boolean hasFluidRun(TankRef source) {
        ToIntFunction<ShuttleJob> from = job -> job instanceof FluidJob fluid && fluid.sourceRef().equals(source) ? 1 : 0;
        return sumQueued(from) > 0 || sumRiding(from) > 0;
    }

    /** Whether a tanker run is queued or under way that takes fluid out of that gantry or brings fluid to it. */
    public boolean hasFluidRunAt(BlockPos gantry) {
        if (sumQueued(job -> job instanceof FluidJob fluid && (fluid.source().equals(gantry) || fluid.target().equals(gantry)) ? 1 : 0) > 0) {
            return true;
        }
        for (Shuttle shuttle : shuttles()) {
            for (Load load : shuttle.loads) {
                if (load.job instanceof FluidJob fluid
                        && (fluid.source().equals(gantry) || load.fluidTarget != null && load.fluidTarget.gantry().equals(gantry))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Fluid that queued runs, and runs still on their way to load, will take out of that tank. */
    public int fluidQueuedFrom(TankRef source) {
        int sum = sumQueued(job -> job instanceof FluidJob fluid && fluid.sourceRef().equals(source) ? fluid.amount() : 0);
        for (Shuttle shuttle : shuttles()) {
            for (Load load : shuttle.loads) {
                if (load.job instanceof FluidJob fluid && fluid.sourceRef().equals(source) && shuttle.leg == Leg.TO_SOURCE) {
                    sum += Math.min(fluid.amount(), shuttle.profile.fluidCapacity());
                }
            }
        }
        return sum;
    }

    /** Fluid that queued and riding runs will put into that tank. */
    public int fluidIncomingTo(TankRef target) {
        int[] sum = {0};
        forIncoming(target, (key, amount) -> sum[0] += amount);
        return sum[0];
    }

    /** The fluid that runs on their way bring into that tank, empty when none is on its way. */
    public Optional<FluidKey> fluidIncomingKey(TankRef target) {
        List<FluidKey> keys = new ArrayList<>();
        forIncoming(target, (key, amount) -> keys.add(key));
        return keys.stream().findFirst();
    }

    /** Hands each run bound for that tank to {@code each} with the amount it will put in. */
    private void forIncoming(TankRef target, ObjIntConsumer<FluidKey> each) {
        for (ShuttleJob job : jobs) {
            if (job instanceof FluidJob fluid && fluid.targetRef().equals(target)) {
                each.accept(fluid.key(), fluid.amount());
            }
        }
        for (Shuttle shuttle : shuttles()) {
            for (Load load : shuttle.loads) {
                if (load.job instanceof FluidJob fluid && target.equals(load.fluidTarget)) {
                    int amount = shuttle.leg == Leg.TO_SOURCE ? Math.min(fluid.amount(), shuttle.profile.fluidCapacity()) : load.fluid.getAmount();
                    each.accept(fluid.key(), amount);
                }
            }
        }
    }

    public void tick() {
        if (!host.noPower()) {
            assignJobs();
        }
        boolean moving = lifts.tick();
        for (Garage garage : garages.values()) {
            for (Shuttle shuttle : garage.shuttles) {
                moving |= tickShuttle(shuttle);
            }
        }
        if (moving || wasMoving || syncPending) {
            if (++syncTimer >= SYNC_INTERVAL || syncPending) {
                syncTimer = 0;
                syncPending = false;
                send();
            }
        }
        wasMoving = moving;
    }

    /** One tick of one shuttle; true while it drives, rides or hands over to a lift. */
    private boolean tickShuttle(Shuttle shuttle) {
        if (shuttle.leg == Leg.WAITING_ROOM && shuttle.busy()) {
            deliverLoads(shuttle);
            if (!shuttle.busy()) {
                goHome(shuttle);
                syncPending = true;
            }
            return false;
        }
        if (shuttle.motion == null) {
            return false;
        }
        if (shuttle.stop > 0 && shuttle.stopAt != shuttle.motion) {
            shuttle.stop = 0;
            endPumping(shuttle);
        }
        if (shuttle.stop > 0) {
            if (--shuttle.stop == 0) {
                arrive(shuttle);
            }
            return false;
        }
        if (shuttle.handover > 0) {
            if (--shuttle.handover == 0) {
                shuttle.handedOver = true;
            }
            return true;
        }
        if (shuttle.riding) {
            if (!lifts.rideOver(shuttle)) {
                return true;
            }
            if (shuttle.motion.arrived()) {
                arrive(shuttle);
                return true;
            }
        }
        if (shuttle.motion.arrived()) {
            if (shuttle.busy()) {
                arrive(shuttle);
            }
            return false;
        }
        boolean wasHeld = shuttle.motion.held();
        boolean done = shuttle.motion.advance(drive(shuttle), segment -> pay(shuttle, segment) && lifts.mayStart(shuttle, segment));
        if (shuttle.motion.held()) {
            shuttle.speed = 0;
        }
        lifts.observe(shuttle, shuttle.motion.currentNode());
        if (wasHeld != shuttle.motion.held()) {
            syncPending = true;
        }
        if (done) {
            arrive(shuttle);
        }
        return true;
    }

    /**
     * Walks the queue in order and hands each job to the free shuttle with
     * the shortest way among those whose modules fit it; that shuttle also
     * takes queued jobs sharing the way while it has room. A job only busy
     * shuttles fit waits; a job no shuttle fits is refused: a pick ends as a
     * shortfall, a put-away goes back to its station, a field job is dropped
     * and asked for again by its field.
     */
    private void assignJobs() {
        if (jobs.isEmpty()) {
            return;
        }
        List<ShuttleJob> queue = new ArrayList<>(jobs);
        Set<ShuttleJob> taken = identitySet();
        List<ShuttleJob> refused = new ArrayList<>();
        Set<JobNeeds> waiting = new HashSet<>();
        for (int index = 0; index < queue.size(); index++) {
            ShuttleJob job = queue.get(index);
            if (taken.contains(job) || waiting.contains(job.needs())) {
                continue;
            }
            List<Shuttle> free = freeShuttles(job.needs());
            if (free.isEmpty() && anyFits(job.needs())) {
                waiting.add(job.needs());
                continue;
            }
            Route route = free.isEmpty() ? null : nearest(free, firstGoals(job));
            taken.add(job);
            if (route == null) {
                if (!job.needs().field()) {
                    refused.add(job);
                }
                continue;
            }
            dispatch(route, job, queue.subList(index + 1, queue.size()), taken);
        }
        if (taken.isEmpty()) {
            return;
        }
        jobs.removeIf(taken::contains);
        for (ShuttleJob job : refused) {
            refuse(job);
        }
    }

    /** The free shuttle with the shortest path to one of the goals, or null when none has a way. */
    private Route nearest(List<Shuttle> free, Set<NodePos> goals) {
        if (goals.isEmpty()) {
            return null;
        }
        Route best = null;
        for (Shuttle shuttle : free) {
            List<NodePos> path = RailPaths.find(shuttle.motion.currentNode(), goals::contains, ways(shuttle));
            if (!path.isEmpty() && (best == null || path.size() < best.path().size())) {
                best = new Route(shuttle, path);
            }
        }
        return best;
    }

    /** Sends the shuttle of the route on the job, with later queued jobs it can take along. */
    private void dispatch(Route route, ShuttleJob job, List<ShuttleJob> later, Set<ShuttleJob> taken) {
        Shuttle shuttle = route.shuttle();
        shuttle.loads.add(new Load(job));
        syncPending = true;
        if (job instanceof FieldJob field) {
            fieldRuns.startRun(shuttle, field);
            return;
        }
        if (job instanceof FertilizeJob fertilize) {
            fieldRuns.startSpread(shuttle, fertilize);
            return;
        }
        if (job instanceof SupplyJob supply) {
            fieldRuns.startSupply(shuttle, supply);
            return;
        }
        List<ShuttleJob> rest = new ArrayList<>();
        for (ShuttleJob next : later) {
            if (!taken.contains(next)) {
                rest.add(next);
            }
        }
        for (ShuttleJob extra : batch(shuttle, rest)) {
            taken.add(extra);
            shuttle.loads.add(new Load(extra));
        }
        shuttle.leg = Leg.TO_SOURCE;
        shuttle.motion = new ShuttleMotion(route.path());
    }

    /** Queued jobs the shuttle takes along with its first one: same way, fitting modules, as many as it has room for. */
    private List<ShuttleJob> batch(Shuttle shuttle, List<ShuttleJob> queue) {
        int room = shuttle.profile.stacks() - shuttle.loads.size();
        ShuttleJob lead = shuttle.lead();
        List<ShuttleJob> batch = new ArrayList<>();
        if (room <= 0 || lead == null) {
            return batch;
        }
        for (int index : CargoBatch.take(queue, job -> lead.sameWay(job) && job.needs().fits(shuttle.profile), room)) {
            batch.add(queue.get(index));
        }
        return batch;
    }

    /** At the source: jobs that were queued for the same way while the shuttle drove there come along too. */
    private void topUp(Shuttle shuttle) {
        List<ShuttleJob> extra = batch(shuttle, new ArrayList<>(jobs));
        if (extra.isEmpty()) {
            return;
        }
        Set<ShuttleJob> taken = identitySet();
        taken.addAll(extra);
        jobs.removeIf(taken::contains);
        for (ShuttleJob job : extra) {
            shuttle.loads.add(new Load(job));
        }
    }

    private List<Shuttle> freeShuttles(JobNeeds needs) {
        List<Shuttle> free = new ArrayList<>();
        for (Garage garage : garages.values()) {
            if (garage.home == null || !garage.active) {
                continue;
            }
            for (Shuttle shuttle : garage.shuttles) {
                if (!shuttle.busy() && shuttle.motion != null && shuttle.lift == null && needs.fits(shuttle.profile)) {
                    free.add(shuttle);
                }
            }
        }
        return free;
    }

    /** A job no shuttle can take: a pick ends as a shortfall, a put-away releases its station slot. */
    private void refuse(ShuttleJob job) {
        if (job instanceof PickJob pick) {
            host.pickLoaded(pick, 0);
        } else if (job instanceof PutAwayJob putAway) {
            host.releaseClaim(putAway.station(), putAway.slot());
        }
    }

    private void arrive(Shuttle shuttle) {
        if (!shuttle.busy()) {
            park(shuttle);
            syncPending = true;
            return;
        }
        if (holdForStop(shuttle)) {
            return;
        }
        if (shuttle.lead() instanceof PickJob pick) {
            arrivePick(shuttle, pick);
        } else if (shuttle.lead() instanceof PutAwayJob putAway) {
            arrivePutAway(shuttle, putAway);
        } else if (shuttle.lead() instanceof FluidJob fluid) {
            arriveFluid(shuttle, fluid);
        } else if (shuttle.lead() instanceof FieldJob || shuttle.lead() instanceof FertilizeJob || shuttle.lead() instanceof SupplyJob) {
            fieldRuns.arrive(shuttle);
            return;
        }
        syncPending = true;
    }

    /**
     * Starts the stop at a source, terminal, station or target the first time
     * the shuttle arrives there: it stands for the stop time of the stacks it
     * moves, then arrives for real. True while the stop holds the arrival back.
     */
    private boolean holdForStop(Shuttle shuttle) {
        boolean stopLeg = switch (shuttle.leg) {
            case TO_SOURCE -> shuttle.lead() instanceof PickJob || shuttle.lead() instanceof PutAwayJob || shuttle.lead() instanceof FluidJob;
            case TO_TERMINAL -> shuttle.lead() instanceof PickJob;
            case TO_TARGET -> shuttle.lead() instanceof PutAwayJob || shuttle.lead() instanceof FluidJob;
            case TO_STATION -> shuttle.lead() instanceof FluidJob;
            default -> false;
        };
        if (!stopLeg) {
            return false;
        }
        if (shuttle.stopAt == shuttle.motion) {
            shuttle.stopAt = null;
            return false;
        }
        if (shuttle.lead() instanceof FluidJob fluid) {
            return holdForPump(shuttle, fluid);
        }
        shuttle.stopAt = shuttle.motion;
        shuttle.stop = ShuttlesStatsManager.INSTANCE.shuttleTiers().stopTicks(shuttle.profile.handling(), shuttle.loads.size());
        syncPending = true;
        return true;
    }

    /**
     * Starts a tanker's stop under the gantry tank of its leg, {@code
     * pump_ticks} shortened by Handling, and lowers the spout; a gantry that
     * no longer serves the leg gets no stop.
     */
    private boolean holdForPump(Shuttle shuttle, FluidJob job) {
        Load load = shuttle.loads.get(0);
        TankRef tank = shuttle.leg == Leg.TO_TARGET ? load.fluidTarget : job.sourceRef();
        GantryMode wanted = switch (shuttle.leg) {
            case TO_SOURCE -> GantryMode.IMPORT;
            case TO_TARGET -> GantryMode.STORAGE;
            default -> null;
        };
        Optional<GantryMode> mode = tank == null ? Optional.empty() : host.gantryMode(tank.gantry());
        if (mode.isEmpty() || wanted != null && mode.get() != wanted) {
            return false;
        }
        shuttle.stopAt = shuttle.motion;
        shuttle.stop = ShuttleTiers.workTicks(ShuttlesStatsManager.INSTANCE.gantryPumpTicks(), shuttle.profile.handling());
        shuttle.pumping = tank;
        shuttle.pumpingIntoGantry = shuttle.leg != Leg.TO_SOURCE;
        host.gantryPumping(tank.gantry(), tank.tank(), shuttle.pumpingIntoGantry,
                load.fluid.isEmpty() ? job.key().stack(1000) : load.fluid.copy(), true);
        syncPending = true;
        return true;
    }

    /** Raises the spout of the gantry the shuttle pumped at, if any. */
    private void endPumping(Shuttle shuttle) {
        if (shuttle.pumping != null) {
            host.gantryPumping(shuttle.pumping.gantry(), shuttle.pumping.tank(), shuttle.pumpingIntoGantry, FluidStack.EMPTY, false);
            shuttle.pumping = null;
        }
    }

    /** Hands each pick load to its destination; loads that went in completely are done. */
    private void deliverLoads(Shuttle shuttle) {
        for (Load load : shuttle.loads) {
            if (load.job instanceof PickJob pick) {
                load.cargo = host.deliverTo(pick, load.cargo);
            }
        }
        shuttle.loads.removeIf(load -> load.cargo.isEmpty());
    }

    private void arrivePick(Shuttle shuttle, PickJob lead) {
        switch (shuttle.leg) {
            case TO_SOURCE -> {
                topUp(shuttle);
                for (Load load : shuttle.loads) {
                    PickJob job = (PickJob) load.job;
                    load.cargo = host.extractFrom(job.source(), job.key(), job.amount());
                    host.pickLoaded(job, load.cargo.getCount());
                }
                shuttle.loads.removeIf(load -> load.cargo.isEmpty());
                if (!shuttle.busy()) {
                    goHome(shuttle);
                } else if (!go(shuttle, Leg.TO_TERMINAL, host.dockRails(Conversions.toNodePos(lead.terminal())))) {
                    for (Load load : shuttle.loads) {
                        host.deliverInstant((PickJob) load.job, load.cargo);
                    }
                    shuttle.loads.clear();
                    goHome(shuttle);
                }
            }
            case TO_TERMINAL -> {
                deliverLoads(shuttle);
                if (!shuttle.busy()) {
                    goHome(shuttle);
                } else {
                    shuttle.leg = Leg.WAITING_ROOM;
                    shuttle.motion = ShuttleMotion.parked(shuttle.motion.end());
                }
            }
            case HOME -> park(shuttle);
            default -> goHome(shuttle);
        }
    }

    private void arrivePutAway(Shuttle shuttle, PutAwayJob lead) {
        switch (shuttle.leg) {
            case TO_SOURCE -> {
                topUp(shuttle);
                for (Load load : shuttle.loads) {
                    PutAwayJob job = (PutAwayJob) load.job;
                    load.cargo = host.takeFromStation(job.station(), job.slot(), job.key(), job.amount());
                }
                shuttle.loads.removeIf(load -> load.cargo.isEmpty());
                goOnOrHome(shuttle, lead, Leg.TO_TARGET, lead.target());
            }
            case TO_TARGET -> {
                for (Load load : shuttle.loads) {
                    load.cargo = host.insertAt(lead.target(), load.cargo);
                }
                shuttle.loads.removeIf(load -> load.cargo.isEmpty());
                goOnOrHome(shuttle, lead, Leg.TO_STATION, lead.station());
            }
            case TO_STATION -> {
                returnLoads(shuttle, lead);
                goHome(shuttle);
            }
            case HOME -> park(shuttle);
        }
    }

    /**
     * A tanker run at the end of a leg: at the source it takes what the tank
     * still holds up to the run's amount and what it carries, at a target it
     * puts in what fits, back at the source it puts the rest back and loses
     * what does not fit. Fluid left over drives on by {@link #deliverFluid}.
     */
    private void arriveFluid(Shuttle shuttle, FluidJob job) {
        endPumping(shuttle);
        Load load = shuttle.loads.get(0);
        switch (shuttle.leg) {
            case TO_SOURCE -> {
                if (host.gantryMode(job.source()).orElse(null) == GantryMode.IMPORT) {
                    load.fluid = host.drainGantry(job.source(), job.sourceTank(), job.key(), Math.min(job.amount(), shuttle.profile.fluidCapacity()));
                }
                deliverFluid(shuttle, load, job, job.source());
            }
            case TO_TARGET -> {
                TankRef target = load.fluidTarget;
                load.tried.add(target);
                if (host.gantryMode(target.gantry()).orElse(null) == GantryMode.STORAGE) {
                    load.fluid = host.fillGantry(target.gantry(), target.tank(), load.fluid);
                }
                deliverFluid(shuttle, load, job, target.gantry());
            }
            case TO_STATION -> {
                host.fillGantry(job.source(), job.sourceTank(), load.fluid);
                shuttle.loads.clear();
                goHome(shuttle);
            }
            case HOME -> park(shuttle);
            default -> goHome(shuttle);
        }
    }

    /**
     * Sends a tanker with fluid on to the next tank it was not offered to:
     * the run's target while it is a Storage gantry, then the target order
     * from the gantry it stands at. With none left it drives back to the
     * source; without a way there the fluid is lost. Empty, it goes home.
     */
    private void deliverFluid(Shuttle shuttle, Load load, FluidJob job, BlockPos at) {
        if (load.fluid.isEmpty()) {
            shuttle.loads.clear();
            goHome(shuttle);
            return;
        }
        List<TankRef> order = new ArrayList<>();
        if (load.fluidTarget != null && host.gantryMode(load.fluidTarget.gantry()).orElse(null) == GantryMode.STORAGE) {
            order.add(load.fluidTarget);
        }
        order.addAll(host.fluidTargets(job.key(), at));
        for (TankRef next : order) {
            if (!load.tried.contains(next) && go(shuttle, Leg.TO_TARGET, host.dockRails(Conversions.toNodePos(next.gantry())))) {
                load.fluidTarget = next;
                return;
            }
        }
        load.fluidTarget = null;
        if (host.gantryMode(job.source()).isEmpty() || !go(shuttle, Leg.TO_STATION, host.dockRails(Conversions.toNodePos(job.source())))) {
            shuttle.loads.clear();
            goHome(shuttle);
        }
    }

    /** A put-away with loads left drives on to the block; without a way there the loads go back to the station. */
    private void goOnOrHome(Shuttle shuttle, PutAwayJob lead, Leg leg, BlockPos next) {
        if (!shuttle.busy()) {
            goHome(shuttle);
        } else if (!go(shuttle, leg, host.dockRails(Conversions.toNodePos(next)))) {
            returnLoads(shuttle, lead);
            goHome(shuttle);
        }
    }

    /** Every put-away load goes back into its station's buffer. */
    private void returnLoads(Shuttle shuttle, PutAwayJob lead) {
        for (Load load : shuttle.loads) {
            host.returnToStation(lead.station(), load.cargo);
        }
        shuttle.loads.clear();
    }

    /** Sends the shuttle towards any of the goal rails; false when no path exists. */
    private boolean go(Shuttle shuttle, Leg leg, Set<NodePos> goals) {
        if (goals.isEmpty()) {
            return false;
        }
        List<NodePos> path = RailPaths.find(shuttle.motion.end(), goals::contains, ways(shuttle));
        if (path.isEmpty()) {
            return false;
        }
        shuttle.leg = leg;
        shuttle.motion = new ShuttleMotion(path);
        return true;
    }

    /** Sends the shuttle home, or back the way it came when no path leads home. */
    void goHome(Shuttle shuttle) {
        shuttle.leg = Leg.HOME;
        NodePos home = shuttle.garage.home;
        List<NodePos> homePath = home == null ? List.of() : RailPaths.find(shuttle.motion.end(), home::equals, ways(shuttle));
        shuttle.motion = homePath.isEmpty() ? shuttle.motion.reverse() : new ShuttleMotion(homePath);
    }

    /** Puts the shuttle back into its garage at once; a field run's cargo drops there. */
    void park(Shuttle shuttle) {
        lifts.release(shuttle);
        endPumping(shuttle);
        dropFieldRun(shuttle);
        shuttle.loads.clear();
        shuttle.leg = Leg.HOME;
        shuttle.motion = shuttle.garage.home == null ? null : ShuttleMotion.parked(shuttle.garage.home);
    }

    /** Pops a field run's cargo at the garage and ends the run; false when the shuttle is on none. */
    private boolean dropFieldRun(Shuttle shuttle) {
        FieldRun fieldRun = shuttle.fieldRun();
        if (fieldRun == null) {
            return false;
        }
        for (ItemStack stack : fieldRun.everything()) {
            Block.popResource(host.level(), shuttle.garage.pos, stack);
        }
        shuttle.run = null;
        shuttle.spread = null;
        shuttle.supply = null;
        return true;
    }

    /**
     * Cargo pops at the shuttle's garage. A job that was not loaded yet goes
     * back to the front of the queue, in its old order; a loaded pick counts
     * as missing; a field run is not queued again; a tanker run loses its
     * fluid and is not queued again.
     */
    private void dropCargo(Shuttle shuttle) {
        endPumping(shuttle);
        if (dropFieldRun(shuttle)) {
            shuttle.loads.clear();
            return;
        }
        List<ShuttleJob> requeue = new ArrayList<>();
        for (Load load : shuttle.loads) {
            if (!load.cargo.isEmpty()) {
                if (load.job instanceof PickJob pick) {
                    host.pickDropped(pick, load.cargo.getCount());
                }
                Block.popResource(host.level(), shuttle.garage.pos, load.cargo);
                load.cargo = ItemStack.EMPTY;
            }
            if (load.job instanceof FluidJob) {
                continue;
            }
            if (!(load.job instanceof PickJob) || shuttle.leg == Leg.TO_SOURCE) {
                requeue.add(load.job);
            }
        }
        for (int i = requeue.size() - 1; i >= 0; i--) {
            jobs.addFirst(requeue.get(i));
        }
        shuttle.loads.clear();
    }

    /** The network's shafts changed: keep, resize, drop and create cars to match. */
    public void refreshShafts(List<Shaft> shafts) {
        lifts.refresh(shafts);
        syncPending = true;
    }

    /** The car of the shaft at that bottom, or null. For tests and the network. */
    public LiftCar lift(BlockPos bottom) {
        return lifts.car(bottom);
    }

    /**
     * The shaft lost segments: its riders and callers lose their cargo and go
     * home, or into their garage when no way home is left. Crossers carry on.
     */
    void liftBroken(BlockPos bottom) {
        for (Garage garage : garages.values()) {
            for (Shuttle shuttle : garage.shuttles) {
                if (!bottom.equals(shuttle.lift) || shuttle.crossing) {
                    continue;
                }
                lifts.release(shuttle);
                dropCargo(shuttle);
                shuttle.leg = Leg.HOME;
                NodePos home = garage.home;
                NodePos at = shuttle.motion == null ? null : shuttle.motion.currentNode();
                List<NodePos> path = home == null || at == null || !host.isTravelNode(at) ? List.of()
                        : RailPaths.find(at, home::equals, ways(shuttle));
                if (path.isEmpty()) {
                    park(shuttle);
                } else {
                    shuttle.motion = new ShuttleMotion(path);
                }
                syncPending = true;
            }
        }
    }

    /**
     * Pays for the segment the shuttle is about to start, once per segment of
     * a path: one block of rail, or, when the shuttle enters a shaft to ride
     * the car, that block and the whole ride at once. A ride a shuttle starts
     * inside a shaft is paid when it starts. Refused, the shuttle stands at
     * the start of the segment and asks again next tick.
     */
    private boolean pay(Shuttle shuttle, int segment) {
        ShuttleMotion motion = shuttle.motion;
        if (shuttle.paidFor == motion && segment <= shuttle.paidThrough) {
            return true;
        }
        List<NodePos> path = motion.path();
        double perBlock = ShuttlesStatsManager.INSTANCE.shuttleFePerBlock();
        ShaftSteps.Kind kind = ShaftSteps.classify(path, segment, host::shaftOf);
        long cost;
        int last;
        if (kind == ShaftSteps.Kind.ENTER_RIDE) {
            int end = ShaftSteps.rideEnd(path, segment + 1);
            cost = StepPower.segmentCost(false, 1, perBlock) + StepPower.segmentCost(true, end - (segment + 1), perBlock);
            last = Math.max(segment, end - 1);
        } else if (kind == ShaftSteps.Kind.RIDE) {
            int end = ShaftSteps.rideEnd(path, segment);
            cost = StepPower.segmentCost(true, end - segment, perBlock);
            last = Math.max(segment, end - 1);
        } else {
            cost = StepPower.segmentCost(false, 1, perBlock);
            last = segment;
        }
        if (!StepPower.mayStart(host.drawPower(cost))) {
            return false;
        }
        shuttle.paidFor = motion;
        shuttle.paidThrough = last;
        return true;
    }

    /**
     * The blocks the shuttle drives this tick: faster from tick to tick up to
     * its top speed, braking for the end of its path and for the lift car. A
     * new path starts from standing.
     */
    private double drive(Shuttle shuttle) {
        ShuttleMotion motion = shuttle.motion;
        if (motion != shuttle.speedFor) {
            shuttle.speedFor = motion;
            shuttle.speed = 0;
        }
        List<NodePos> path = motion.path();
        double toStop = ShuttleDrive.toStop(path.size(), motion.segment(), motion.progress(), i -> vertical(path, i));
        shuttle.speed = ShuttleDrive.next(shuttle.speed, shuttle.profile.speed(), shuttle.profile.accel(), toStop);
        // Never zero, so a shuttle standing at the start of a ride still asks the gate, which hands it to the car.
        return Math.max(shuttle.speed, 1e-4);
    }

    private static boolean vertical(List<NodePos> path, int i) {
        NodePos a = path.get(i);
        NodePos b = path.get(i + 1);
        return a.x() == b.x() && a.z() == b.z() && a.y() != b.y();
    }

    private void send() {
        for (Garage garage : garages.values()) {
            PacketDistributor.sendToPlayersTrackingChunk(host.level(), ChunkPos.containing(garage.pos),
                    new ShuttleSyncPayload(garage.pos, views(garage.pos)));
        }
        lifts.sendAll();
    }

    /** The shuttles of one garage as clients draw them: only those out on the rails, idle ones sit inside the garage. */
    public List<ShuttleView> views(BlockPos garagePos) {
        List<ShuttleView> list = new ArrayList<>();
        Garage garage = garages.get(garagePos);
        if (garage == null) {
            return list;
        }
        for (Shuttle shuttle : garage.shuttles) {
            ShuttleMotion motion = shuttle.motion;
            if (motion == null || (!shuttle.busy() && motion.arrived())) {
                continue;
            }
            List<BlockPos> path = new ArrayList<>();
            for (NodePos node : motion.path()) {
                path.add(Conversions.toBlockPos(node));
            }
            boolean still = shuttle.riding || motion.held() || shuttle.stop > 0 || shuttle.handover > 0 || motion != shuttle.speedFor;
            ShuttleView.Drive shown = still ? ShuttleView.Drive.STILL
                    : new ShuttleView.Drive((float) shuttle.speed, (float) shuttle.profile.speed(), (float) shuttle.profile.accel());
            Optional<BlockPos> lift = shuttle.riding && shuttle.lift != null ? Optional.of(shuttle.lift) : Optional.empty();
            list.add(new ShuttleView(path, motion.segment(), (float) motion.progress(), shown, shuttle.shownCargo(), lift, shuttle.item.copy(),
                    sightGlass(shuttle)));
        }
        return list;
    }

    /** What a tanker's sight glass shows: the fluid it carries and its share of what it may carry. */
    private static Optional<ShuttleView.SightGlass> sightGlass(Shuttle shuttle) {
        FluidStack fluid = shuttle.shownFluid();
        int capacity = shuttle.profile.fluidCapacity();
        if (fluid.isEmpty() || capacity <= 0) {
            return Optional.empty();
        }
        return Optional.of(new ShuttleView.SightGlass(fluid.copyWithAmount(1), Math.min(1, (float) fluid.getAmount() / capacity)));
    }

    public String summary() {
        if (garages.isEmpty()) {
            return "no garages";
        }
        String rail = withoutRail() == 0 ? "" : ", " + withoutRail() + " parked without a rail";
        return garages.size() + " garages, " + size() + " shuttles, " + busy() + " busy, " + jobs.size() + " queued ("
                + queuedPutAways() + " put-aways)" + rail;
    }

    /** Shuttles of one garage that are out on a job. */
    public int outOf(BlockPos pos) {
        Garage garage = garages.get(pos);
        if (garage == null) {
            return 0;
        }
        int out = 0;
        for (Shuttle shuttle : garage.shuttles) {
            if (shuttle.out()) {
                out++;
            }
        }
        return out;
    }

    /** A bit per garage slot whose shuttle is out on a job, for the first {@link #MASK_SLOTS} slots. */
    public int outSlots(BlockPos pos) {
        Garage garage = garages.get(pos);
        if (garage == null) {
            return 0;
        }
        int mask = 0;
        for (Shuttle shuttle : garage.shuttles) {
            if (shuttle.out() && shuttle.slot < MASK_SLOTS) {
                mask |= 1 << shuttle.slot;
            }
        }
        return mask;
    }

    /** One garage's line: how many it holds, how many are out, whether it has a rail. */
    public String garageSummary(BlockPos pos) {
        Garage garage = garages.get(pos);
        if (garage == null) {
            return "not in a warehouse";
        }
        int out = 0;
        for (Shuttle shuttle : garage.shuttles) {
            if (shuttle.busy()) {
                out++;
            }
        }
        return garage.shuttles.size() + " shuttles, " + out + " out" + (garage.home == null ? ", no rail" : "") + (garage.active ? "" : ", paused");
    }
}
