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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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

        /** Takes up to amount of the key out of exactly that source, from slots no job locked. */
        ItemStack extractFrom(BlockPos source, ItemKey key, int amount);

        /** Inserts into exactly that storage block, into slots no job locked; returns what did not fit. */
        ItemStack insertAt(BlockPos target, ItemStack stack);

        /** Takes what the put-away's lock on its station slot reserves and releases that lock. */
        ItemStack loadPutAway(PutAwayJob job);

        /** Takes what the pick's locks on its source reserve and releases them. */
        ItemStack loadPick(PickJob job);

        /** Puts cargo into the slots of that rack locked for the job and releases them; returns what did not fit. */
        ItemStack unloadInto(BlockPos target, int jobId, ItemStack cargo);

        /**
         * Whether the pick may be sent: its destination slot is locked, or was
         * just locked now, or its destination is gone, so its cargo will be stored.
         */
        boolean readyToSend(PickJob job);

        /** Whether the pick's destination is loaded and no longer takes deliveries. */
        boolean destinationGone(PickJob job);

        /**
         * Locks room for cargo of the job in the racks the put-away rule picks
         * as seen from {@code from} over {@code ways}, as many racks as it takes;
         * returns them best first, empty when none has room.
         */
        List<BlockPos> storeTargets(int jobId, ItemStack cargo, Set<NodePos> from, Function<NodePos, Set<NodePos>> ways);

        /** Whether some rack the put-away rule allows has room for the cargo at all, reachable or not. */
        boolean anyRoomFor(ItemStack cargo);

        /** Releases every lock of the job on those blocks. */
        void releaseLocks(int jobId, Collection<BlockPos> at);

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

        /** The saved state of the node at that position changed, so its chunk must be saved again. */
        default void stateChanged(BlockPos pos) {
        }

        /** The queue changed or holds jobs, so the controller's chunk must be saved again. */
        default void queueChanged() {
        }

        /** Whether a shuttle of that garage parks with cargo that has nowhere to go changed. */
        default void strandedChanged(BlockPos garage, boolean stranded) {
        }
    }

    /** A free shuttle and its path to a job's first goal. */
    private record Route(Shuttle shuttle, List<NodePos> path) {
    }

    private static final int SYNC_INTERVAL = 5;
    /** Ticks between two tries of shuttles parked with cargo that has nowhere to go. */
    static final int STRAND_RETRY_TICKS = 100;
    /** Ticks a restored shuttle waits once its path's chunks are loaded, and the queue waits after a restore. */
    static final int RESTORE_SETTLE_TICKS = 20;
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
    /** Ticks job assignment still waits after a restore. */
    private int assignWait;
    private int strandTimer;
    /** Set when room may have appeared; parked shuttles with cargo try again on the next tick. */
    private boolean roomChanged;
    /** Garages whose lamp shows a shuttle parked with cargo, as last told to the host. */
    private final Set<BlockPos> strandedGarages = new HashSet<>();
    private int lastQueueSize;

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

    /**
     * Called when a garage leaves. One that leaves for good drops its
     * shuttles' cargo there and their jobs go back to the queue; any other
     * leave keeps nothing here because the garage took its shuttles' state
     * along ({@link #saveGarage}). The shuttles are forgotten either way.
     */
    public void releaseGarage(BlockPos pos, boolean forGood) {
        Garage garage = garages.remove(pos);
        if (garage == null) {
            return;
        }
        for (Shuttle shuttle : garage.shuttles) {
            lifts.release(shuttle);
            if (forGood) {
                dropCargo(shuttle);
            } else {
                endPumping(shuttle);
            }
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
        if (garage.home == null && previous != null && !loaded(previous)) {
            garage.home = previous;
        }
        if (Objects.equals(garage.home, previous)) {
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
        shuttle.restoring = null;
        shuttle.strandReason = null;
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

    /** Drops the queued picks of a request and their locks; loads already on a shuttle still arrive. */
    public void removeQueued(int requestId) {
        jobs.removeIf(job -> {
            if (job instanceof PickJob pick && pick.requestId() == requestId) {
                host.releaseLocks(pick.id(), blocksOf(pick));
                return true;
            }
            return false;
        });
    }

    /**
     * Drops the picks from that source that are queued or on their way to it
     * and not loaded yet; their shuttles go home when nothing else is aboard.
     * Their locks stay for the caller to release.
     */
    public List<PickJob> dropPicksFrom(BlockPos source) {
        List<PickJob> dropped = new ArrayList<>();
        jobs.removeIf(job -> {
            if (job instanceof PickJob pick && pick.source().equals(source)) {
                dropped.add(pick);
                return true;
            }
            return false;
        });
        for (Shuttle shuttle : shuttles()) {
            if (shuttle.leg != Leg.TO_SOURCE) {
                continue;
            }
            boolean any = shuttle.loads.removeIf(load -> {
                if (load.job instanceof PickJob pick && pick.source().equals(source) && load.cargo.isEmpty()) {
                    dropped.add(pick);
                    return true;
                }
                return false;
            });
            if (any && !shuttle.busy()) {
                goHome(shuttle);
            }
        }
        syncPending |= !dropped.isEmpty();
        return dropped;
    }

    /** The blocks a job names: a pick's source and destination, a put-away's station and target. */
    static List<BlockPos> blocksOf(ShuttleJob job) {
        return switch (job) {
            case PickJob pick -> List.of(pick.source(), pick.terminal());
            case PutAwayJob putAway -> List.of(putAway.station(), putAway.target());
            default -> List.of();
        };
    }

    /** Releases every lock of a load: on the blocks its job names and on its stops. */
    private void releaseLoad(Load load) {
        List<BlockPos> at = new ArrayList<>(blocksOf(load.job));
        at.addAll(load.stops);
        if (!at.isEmpty()) {
            host.releaseLocks(load.job.id(), at);
        }
    }

    /**
     * A put-away of the item from that station that no shuttle has loaded
     * yet, so more of the item may join it; empty when there is none.
     */
    public Optional<PutAwayJob> openPutAway(BlockPos station, ItemKey key) {
        for (ShuttleJob job : jobs) {
            if (job instanceof PutAwayJob putAway && putAway.station().equals(station) && putAway.key().equals(key)) {
                return Optional.of(putAway);
            }
        }
        for (Shuttle shuttle : shuttles()) {
            if (shuttle.leg != Leg.TO_SOURCE) {
                continue;
            }
            for (Load load : shuttle.loads) {
                if (load.job instanceof PutAwayJob putAway && putAway.station().equals(station) && putAway.key().equals(key)
                        && load.cargo.isEmpty()) {
                    return Optional.of(putAway);
                }
            }
        }
        return Optional.empty();
    }

    /** Queued jobs that are put-aways. */
    public int queuedPutAways() {
        return sumQueued(job -> job instanceof PutAwayJob ? 1 : 0);
    }

    public int queued() {
        return jobs.size();
    }

    /** Whether a job with that id is queued or a shuttle carries it. */
    public boolean knows(int jobId) {
        return sumQueued(job -> job.id() == jobId ? 1 : 0) > 0 || sumRiding(job -> job.id() == jobId ? 1 : 0) > 0;
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

    /** Whether a tanker run is queued that brings fluid to that tank, or one under way is bound for it. */
    public boolean hasFluidRunTo(TankRef target) {
        boolean[] any = {false};
        forIncoming(target, (key, amount) -> any[0] = true);
        return any[0];
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
        if (assignWait > 0) {
            assignWait--;
        } else if (!host.noPower()) {
            assignJobs();
        }
        if (++strandTimer >= STRAND_RETRY_TICKS || roomChanged) {
            strandTimer = 0;
            roomChanged = false;
            retryStranded();
        }
        updateStrandedLamps();
        boolean moving = lifts.tick();
        for (Garage garage : garages.values()) {
            boolean out = false;
            for (Shuttle shuttle : garage.shuttles) {
                moving |= tickShuttle(shuttle);
                out |= shuttle.out();
            }
            if (out) {
                host.stateChanged(garage.pos);
            }
        }
        if (!jobs.isEmpty() || jobs.size() != lastQueueSize) {
            lastQueueSize = jobs.size();
            host.queueChanged();
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
        if (shuttle.restoring != null) {
            tickRestore(shuttle);
            return false;
        }
        if (shuttle.motion != null && !shuttle.riding && !nextLoaded(shuttle.motion)) {
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
            if (taken.contains(job) || waiting.contains(job.needs()) || !ready(job)) {
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

    /** Whether a job may go out now: a pick only once its destination slot is locked. */
    private boolean ready(ShuttleJob job) {
        return !(job instanceof PickJob pick) || host.readyToSend(pick);
    }

    /** The free shuttle with the cheapest way to one of the goals, or null when none has a way. */
    private Route nearest(List<Shuttle> free, Set<NodePos> goals) {
        if (goals.isEmpty()) {
            return null;
        }
        Route best = null;
        double bestCost = Double.POSITIVE_INFINITY;
        for (Shuttle shuttle : free) {
            NodePos from = shuttle.motion.currentNode();
            RailCosts.Search<NodePos> search = costsFrom(List.of(from), ways(shuttle), goals::contains);
            Optional<NodePos> goal = search.cheapest(goals::contains);
            if (goal.isPresent() && search.costTo(goal.get()) < bestCost) {
                bestCost = search.costTo(goal.get());
                best = new Route(shuttle, search.pathTo(goal.get()));
            }
        }
        return best;
    }

    /** Travel costs from the starts over every rail and shaft, for the warehouse's choices. */
    public RailCosts.Search<NodePos> costsFrom(Collection<NodePos> starts) {
        return costsFrom(starts, host::travelNeighbours, node -> false);
    }

    /** Travel costs from the starts over these ways. */
    public RailCosts.Search<NodePos> costsFrom(Collection<NodePos> starts, Function<NodePos, ? extends Collection<NodePos>> ways) {
        return costsFrom(starts, ways, node -> false);
    }

    /** Travel costs from the starts over these ways, stopping at the first node passing {@code stopAt}. */
    RailCosts.Search<NodePos> costsFrom(Collection<NodePos> starts, Function<NodePos, ? extends Collection<NodePos>> ways, Predicate<NodePos> stopAt) {
        TravelWeights weights = ShuttlesStatsManager.INSTANCE.travelWeights();
        TravelWeights.Lifts lifts = liftLoads(weights);
        return RailCosts.search(starts, ways, (previous, from, to) -> weights.step(previous, from, to, lifts), stopAt);
    }

    /** The cheapest way for the shuttle from a node to the cheapest goal, start first; empty without a way. */
    List<NodePos> path(Shuttle shuttle, NodePos from, Predicate<NodePos> goal) {
        if (goal.test(from)) {
            return List.of(from);
        }
        return costsFrom(List.of(from), ways(shuttle), goal).pathToCheapest(goal);
    }

    /**
     * The lifts as one search sees them: per shaft the car's height and the
     * calls queued at it plus the busy shuttles whose way still passes the shaft.
     */
    private TravelWeights.Lifts liftLoads(TravelWeights weights) {
        Map<Shaft, Integer> planned = new HashMap<>();
        for (Shuttle shuttle : shuttles()) {
            if (!shuttle.busy() || shuttle.motion == null) {
                continue;
            }
            Set<Shaft> passed = new HashSet<>();
            for (NodePos node : shuttle.motion.remaining()) {
                Shaft shaft = host.shaftOf(node);
                if (shaft != null && passed.add(shaft)) {
                    planned.merge(shaft, 1, Integer::sum);
                }
            }
        }
        return new TravelWeights.Lifts() {
            @Override
            public Shaft shaftOf(NodePos node) {
                return host.shaftOf(node);
            }

            @Override
            public double waitAt(Shaft shaft, int floorY) {
                LiftCar car = lifts.car(Conversions.toBlockPos(shaft.bottom()));
                double carY = car == null ? floorY : car.y();
                int calls = car == null ? 0 : car.calls().size();
                return weights.liftWait(carY, floorY, calls + planned.getOrDefault(shaft, 0));
            }
        };
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
        for (int index : CargoBatch.take(queue, job -> lead.sameWay(job) && job.needs().fits(shuttle.profile) && ready(job), room)) {
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
                if (!shuttle.busy() && shuttle.motion != null && shuttle.lift == null && shuttle.restoring == null
                        && needs.fits(shuttle.profile)) {
                    free.add(shuttle);
                }
            }
        }
        return free;
    }

    /** A job no shuttle can take: a pick ends as a shortfall; either kind releases its locks. */
    private void refuse(ShuttleJob job) {
        if (job instanceof PickJob pick) {
            host.pickLoaded(pick, 0);
        }
        List<BlockPos> blocks = blocksOf(job);
        if (!blocks.isEmpty()) {
            host.releaseLocks(job.id(), blocks);
        }
    }

    private void arrive(Shuttle shuttle) {
        if (shuttle.leg == Leg.STRANDED) {
            return;
        }
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
            case TO_TARGET -> shuttle.lead() instanceof PutAwayJob || shuttle.lead() instanceof PickJob || shuttle.lead() instanceof FluidJob;
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
            case TO_SOURCE -> job.sourceMode();
            case TO_TARGET -> tank == null ? null : job.targetMode(tank);
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

    private void arrivePick(Shuttle shuttle, PickJob lead) {
        switch (shuttle.leg) {
            case TO_SOURCE -> {
                topUp(shuttle);
                for (Load load : shuttle.loads) {
                    PickJob job = (PickJob) load.job;
                    load.cargo = host.loadPick(job);
                    host.pickLoaded(job, load.cargo.getCount());
                }
                dropEmptyLoads(shuttle);
                if (!shuttle.busy()) {
                    goHome(shuttle);
                } else if (!go(shuttle, Leg.TO_TERMINAL, host.dockRails(Conversions.toNodePos(lead.terminal())))) {
                    for (Load load : shuttle.loads) {
                        handOverAtOnce(load, (PickJob) load.job);
                    }
                    continueStores(shuttle);
                }
            }
            case TO_TERMINAL, WAITING_ROOM -> {
                for (Load load : shuttle.loads) {
                    if (load.job instanceof PickJob pick && load.stops.isEmpty()) {
                        deliver(load, pick);
                    }
                }
                continueStores(shuttle);
            }
            case TO_TARGET -> unloadHere(shuttle);
            case HOME -> parkWithCargo(shuttle);
            default -> goHome(shuttle);
        }
    }

    /**
     * Hands a pick load its shuttle cannot drive to the destination over at
     * once, as an instant delivery held until it fits; when the destination is
     * gone the cargo counts as missing and is stored instead.
     */
    private void handOverAtOnce(Load load, PickJob pick) {
        if (host.destinationGone(pick)) {
            host.pickDropped(pick, load.cargo.getCount());
            host.releaseLocks(pick.id(), List.of(pick.terminal()));
            load.storing = true;
            return;
        }
        host.deliverInstant(pick, load.cargo);
        load.cargo = ItemStack.EMPTY;
    }

    /**
     * Hands a pick load to its destination's locked slot. Cargo the
     * destination does not take, because it is gone or lost its lock, counts
     * as missing and is stored like a leftover.
     */
    private void deliver(Load load, PickJob pick) {
        if (!host.destinationGone(pick)) {
            load.cargo = host.deliverTo(pick, load.cargo);
        }
        if (!load.cargo.isEmpty()) {
            host.pickDropped(pick, load.cargo.getCount());
            host.releaseLocks(pick.id(), List.of(pick.terminal()));
            load.storing = true;
        }
    }

    private void arrivePutAway(Shuttle shuttle, PutAwayJob lead) {
        switch (shuttle.leg) {
            case TO_SOURCE -> {
                topUp(shuttle);
                for (Load load : shuttle.loads) {
                    load.cargo = host.loadPutAway((PutAwayJob) load.job);
                    load.storing = true;
                }
                continueStores(shuttle);
            }
            case TO_TARGET -> unloadHere(shuttle);
            case HOME -> parkWithCargo(shuttle);
            default -> continueStores(shuttle);
        }
    }

    /** Loads that ended without cargo leave the shuttle and release their locks. */
    private void dropEmptyLoads(Shuttle shuttle) {
        shuttle.loads.removeIf(load -> {
            if (load.cargo.isEmpty()) {
                releaseLoad(load);
                return true;
            }
            return false;
        });
    }

    /** The stop the shuttle stands at: the first stop of its loads whose dock rails hold the shuttle's node. */
    private BlockPos stopHere(Shuttle shuttle) {
        NodePos at = shuttle.motion.end();
        for (Load load : shuttle.loads) {
            for (BlockPos stop : load.stops) {
                if (host.dockRails(Conversions.toNodePos(stop)).contains(at)) {
                    return stop;
                }
            }
        }
        return null;
    }

    /** At a rack: every load with this rack as a stop unloads into its locked slots, then the shuttle goes on. */
    private void unloadHere(Shuttle shuttle) {
        BlockPos stop = stopHere(shuttle);
        for (Load load : shuttle.loads) {
            if (stop != null && load.stops.remove(stop)) {
                load.cargo = host.unloadInto(stop, load.job.id(), load.cargo);
            }
        }
        continueStores(shuttle);
    }

    /**
     * Sends a shuttle with cargo to be stored on to its next stop. Cargo
     * without a stop is routed again by the put-away rule from where the
     * shuttle stands, with new locks; cargo no rack takes parks the shuttle
     * in its garage. Empty, the shuttle goes home.
     */
    void continueStores(Shuttle shuttle) {
        dropEmptyLoads(shuttle);
        if (!shuttle.busy()) {
            shuttle.strandReason = null;
            goHome(shuttle);
            return;
        }
        for (Load load : shuttle.loads) {
            if (!load.storing && load.job instanceof PickJob pick) {
                if (go(shuttle, Leg.TO_TERMINAL, host.dockRails(Conversions.toNodePos(pick.terminal())))) {
                    return;
                }
                handOverAtOnce(load, pick);
            }
        }
        dropEmptyLoads(shuttle);
        if (!shuttle.busy()) {
            goHome(shuttle);
            return;
        }
        NodePos at = shuttle.motion.end();
        int guard = 16;
        while (guard-- > 0) {
            for (Load load : shuttle.loads) {
                if (load.storing && load.stops.isEmpty()) {
                    load.stops.addAll(host.storeTargets(load.job.id(), load.cargo, Set.of(at), ways(shuttle)));
                }
            }
            Load next = firstWithStop(shuttle);
            if (next == null) {
                break;
            }
            BlockPos stop = next.stops.get(0);
            if (go(shuttle, Leg.TO_TARGET, host.dockRails(Conversions.toNodePos(stop)))) {
                shuttle.strandReason = null;
                return;
            }
            for (Load load : shuttle.loads) {
                if (load.stops.remove(stop)) {
                    host.releaseLocks(load.job.id(), List.of(stop));
                }
            }
        }
        if (anyStoring(shuttle)) {
            strand(shuttle);
        } else {
            goHome(shuttle);
        }
    }

    private static Load firstWithStop(Shuttle shuttle) {
        for (Load load : shuttle.loads) {
            if (!load.stops.isEmpty()) {
                return load;
            }
        }
        return null;
    }

    private static boolean anyStoring(Shuttle shuttle) {
        for (Load load : shuttle.loads) {
            if (load.storing && !load.cargo.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** No rack takes the cargo: the shuttle drives into its garage and parks there with it. */
    private void strand(Shuttle shuttle) {
        shuttle.strandReason = anyRoom(shuttle) ? StrandReason.NO_WAY : StrandReason.NO_ROOM;
        if (shuttle.leg == Leg.STRANDED && shuttle.motion.arrived()) {
            return;
        }
        NodePos home = shuttle.garage.home;
        List<NodePos> homePath = home == null ? List.of() : path(shuttle, shuttle.motion.end(), home::equals);
        shuttle.leg = Leg.STRANDED;
        shuttle.motion = homePath.isEmpty() ? (home == null ? ShuttleMotion.parked(shuttle.motion.end()) : ShuttleMotion.parked(home))
                : new ShuttleMotion(homePath);
        syncPending = true;
    }

    private boolean anyRoom(Shuttle shuttle) {
        for (Load load : shuttle.loads) {
            if (load.storing && !load.cargo.isEmpty() && host.anyRoomFor(load.cargo)) {
                return true;
            }
        }
        return false;
    }

    /** Room may have appeared: parked shuttles with cargo try again on the next tick. */
    public void roomChanged() {
        roomChanged = true;
    }

    /** Shuttles parked at home with cargo look for a rack again. */
    private void retryStranded() {
        for (Shuttle shuttle : shuttles()) {
            if (shuttle.leg == Leg.STRANDED && shuttle.motion != null && shuttle.motion.arrived() && shuttle.restoring == null) {
                continueStores(shuttle);
            }
        }
    }

    /** Tells the host which garages have a shuttle parked with cargo, when that changed. */
    private void updateStrandedLamps() {
        for (Garage garage : garages.values()) {
            boolean stranded = !strandedOf(garage).isEmpty();
            if (stranded != strandedGarages.contains(garage.pos)) {
                if (stranded) {
                    strandedGarages.add(garage.pos);
                } else {
                    strandedGarages.remove(garage.pos);
                }
                host.strandedChanged(garage.pos, stranded);
            }
        }
        strandedGarages.removeIf(pos -> !garages.containsKey(pos));
    }

    private static List<Shuttle> strandedOf(Garage garage) {
        List<Shuttle> list = new ArrayList<>();
        for (Shuttle shuttle : garage.shuttles) {
            if (shuttle.leg == Leg.STRANDED && shuttle.strandReason != null && shuttle.busy()) {
                list.add(shuttle);
            }
        }
        return list;
    }

    /** One shuttle of a garage parked with cargo: its item, what it holds and why it waits. */
    public record Stranded(int slot, ItemStack shuttle, ItemStack cargo, StrandReason reason) {
    }

    /** The shuttles of a garage parked with cargo that has nowhere to go. */
    public List<Stranded> stranded(BlockPos pos) {
        Garage garage = garages.get(pos);
        List<Stranded> list = new ArrayList<>();
        if (garage == null) {
            return list;
        }
        for (Shuttle shuttle : strandedOf(garage)) {
            list.add(new Stranded(shuttle.slot, shuttle.item.copy(), shuttle.shownCargo(), shuttle.strandReason));
        }
        return list;
    }

    /** The item of the shuttle carrying out the job, or empty while it waits for one. */
    public Optional<ItemStack> shuttleOnJob(int jobId) {
        for (Shuttle shuttle : shuttles()) {
            for (Load load : shuttle.loads) {
                if (load.job.id() == jobId) {
                    return Optional.of(shuttle.item.copy());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * A tanker run at the end of a leg: at the source it takes what the tank
     * still holds up to the run's amount and what it carries, at a target it
     * puts in what fits, back at the source of a put-away it puts the rest
     * back and loses what does not fit. Fluid left over drives on by {@link #deliverFluid}.
     */
    private void arriveFluid(Shuttle shuttle, FluidJob job) {
        endPumping(shuttle);
        Load load = shuttle.loads.get(0);
        switch (shuttle.leg) {
            case TO_SOURCE -> {
                if (host.gantryMode(job.source()).orElse(null) == job.sourceMode()) {
                    load.fluid = host.drainGantry(job.source(), job.sourceTank(), job.key(), Math.min(job.amount(), shuttle.profile.fluidCapacity()));
                }
                deliverFluid(shuttle, load, job, job.source());
            }
            case TO_TARGET -> {
                TankRef target = load.fluidTarget;
                load.tried.add(target);
                if (host.gantryMode(target.gantry()).orElse(null) == job.targetMode(target)) {
                    load.fluid = host.fillGantry(target.gantry(), target.tank(), load.fluid);
                }
                deliverFluid(shuttle, load, job, target.gantry());
            }
            case TO_STATION -> {
                host.fillGantry(job.source(), job.sourceTank(), load.fluid);
                shuttle.loads.clear();
                goHome(shuttle);
            }
            case HOME -> recallShuttle(shuttle);
            default -> goHome(shuttle);
        }
    }

    /**
     * Sends a tanker with fluid on to the next tank it was not offered to:
     * the run's target while its gantry still serves the run, then the
     * Storage target order from the gantry it stands at. With none left a
     * put-away drives back to the source; without a way there, or on a
     * restock, the fluid is lost. Empty, it goes home.
     */
    private void deliverFluid(Shuttle shuttle, Load load, FluidJob job, BlockPos at) {
        if (load.fluid.isEmpty()) {
            shuttle.loads.clear();
            goHome(shuttle);
            return;
        }
        List<TankRef> order = new ArrayList<>();
        if (load.fluidTarget != null && host.gantryMode(load.fluidTarget.gantry()).orElse(null) == job.targetMode(load.fluidTarget)) {
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
        if (!job.returnsToSource() || host.gantryMode(job.source()).isEmpty() || !go(shuttle, Leg.TO_STATION, host.dockRails(Conversions.toNodePos(job.source())))) {
            shuttle.loads.clear();
            goHome(shuttle);
        }
    }

    /** Sends the shuttle towards any of the goal rails; false when no path exists. */
    private boolean go(Shuttle shuttle, Leg leg, Set<NodePos> goals) {
        if (goals.isEmpty()) {
            return false;
        }
        List<NodePos> path = path(shuttle, shuttle.motion.end(), goals::contains);
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
        List<NodePos> homePath = home == null ? List.of() : path(shuttle, shuttle.motion.end(), home::equals);
        shuttle.motion = homePath.isEmpty() ? shuttle.motion.reverse() : new ShuttleMotion(homePath);
    }

    /** Puts the shuttle back into its garage at once; a field run's cargo drops there. */
    void park(Shuttle shuttle) {
        lifts.release(shuttle);
        shuttle.restoring = null;
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
     * back to the front of the queue, in its old order, with its locks; a
     * loaded job releases its locks and a loaded pick counts as missing; a
     * field run is not queued again; a tanker run loses its fluid and is not
     * queued again.
     */
    private void dropCargo(Shuttle shuttle) {
        endPumping(shuttle);
        shuttle.strandReason = null;
        if (dropFieldRun(shuttle)) {
            shuttle.loads.clear();
            return;
        }
        List<ShuttleJob> requeue = new ArrayList<>();
        for (Load load : shuttle.loads) {
            boolean loaded = !load.cargo.isEmpty();
            if (loaded) {
                if (load.job instanceof PickJob pick && !load.storing) {
                    host.pickDropped(pick, load.cargo.getCount());
                }
                Block.popResource(host.level(), shuttle.garage.pos, load.cargo);
                load.cargo = ItemStack.EMPTY;
            }
            if (load.job instanceof FluidJob) {
                continue;
            }
            if (!loaded && shuttle.leg == Leg.TO_SOURCE) {
                requeue.add(load.job);
            } else {
                releaseLoad(load);
            }
        }
        requeueFirst(requeue);
        shuttle.loads.clear();
    }

    private void requeueFirst(List<ShuttleJob> requeue) {
        for (int i = requeue.size() - 1; i >= 0; i--) {
            jobs.addFirst(requeue.get(i));
        }
    }

    /**
     * Puts a shuttle that cannot drive on back into its garage without
     * dropping anything: jobs not loaded yet go back to the queue with their
     * locks, picks hand their cargo to their destination as instant
     * deliveries, and cargo to be stored stays aboard, parked as cargo with
     * nowhere to go. A field run ends as a recall does.
     */
    private void parkWithCargo(Shuttle shuttle) {
        if (shuttle.fieldRun() != null) {
            recallShuttle(shuttle);
            return;
        }
        lifts.release(shuttle);
        endPumping(shuttle);
        shuttle.restoring = null;
        List<ShuttleJob> requeue = new ArrayList<>();
        boolean waiting = shuttle.leg == Leg.TO_SOURCE;
        shuttle.loads.removeIf(load -> {
            if (load.cargo.isEmpty()) {
                if (waiting && !(load.job instanceof FluidJob)) {
                    requeue.add(load.job);
                } else {
                    releaseLoad(load);
                }
                return true;
            }
            if (load.job instanceof PickJob pick && !load.storing) {
                handOverAtOnce(load, pick);
                return load.cargo.isEmpty();
            }
            load.storing = true;
            return false;
        });
        requeueFirst(requeue);
        if (!shuttle.busy()) {
            park(shuttle);
            return;
        }
        shuttle.strandReason = anyRoom(shuttle) ? StrandReason.NO_WAY : StrandReason.NO_ROOM;
        shuttle.leg = Leg.STRANDED;
        NodePos home = shuttle.garage.home;
        shuttle.motion = ShuttleMotion.parked(home == null ? shuttle.motion.currentNode() : home);
        syncPending = true;
    }

    /** Whether the chunk of the node is loaded. */
    private boolean loaded(NodePos node) {
        return host.level().isLoaded(Conversions.toBlockPos(node));
    }

    /** Whether the node the motion is on and the one it drives to next lie in loaded chunks. */
    private boolean nextLoaded(ShuttleMotion motion) {
        if (!loaded(motion.currentNode())) {
            return false;
        }
        return motion.arrived() || loaded(motion.path().get(motion.segment() + 1));
    }

    /** Whether every node the motion still has ahead lies in a loaded chunk. */
    private boolean aheadLoaded(ShuttleMotion motion) {
        for (NodePos node : motion.remaining()) {
            if (!loaded(node)) {
                return false;
            }
        }
        return true;
    }

    /** A restored shuttle waits until the chunks ahead of it have been loaded for a while, then goes on. */
    private void tickRestore(Shuttle shuttle) {
        boolean ready = shuttle.fieldRun() != null ? loaded(shuttle.motion.currentNode()) : aheadLoaded(shuttle.motion);
        if (shuttle.restoring.tick(ready)) {
            shuttle.restoring = null;
            resume(shuttle);
            syncPending = true;
        }
    }

    /**
     * A restored shuttle goes on: a field run decides again from the node it
     * stands on; any other shuttle drives on while its path is whole, else
     * looks for a new way to its leg's goal, else home, else is recalled with its cargo.
     */
    private void resume(Shuttle shuttle) {
        if (shuttle.fieldRun() != null) {
            shuttle.motion = ShuttleMotion.parked(shuttle.motion.currentNode());
            fieldRuns.resume(shuttle);
            return;
        }
        if (shuttle.leg == Leg.WAITING_ROOM) {
            shuttle.leg = Leg.TO_TERMINAL;
        }
        if (shuttle.leg == Leg.STRANDED && shuttle.motion.arrived() || pathWhole(shuttle.motion)) {
            return;
        }
        NodePos at = shuttle.motion.currentNode();
        if (!host.isTravelNode(at)) {
            parkWithCargo(shuttle);
            return;
        }
        Set<NodePos> goals = legGoals(shuttle);
        List<NodePos> path = goals.isEmpty() ? List.of() : path(shuttle, at, goals::contains);
        if (!path.isEmpty()) {
            shuttle.motion = new ShuttleMotion(path);
            return;
        }
        if (shuttle.leg == Leg.TO_TARGET && !(shuttle.lead() instanceof FluidJob)) {
            shuttle.motion = ShuttleMotion.parked(at);
            for (Load load : shuttle.loads) {
                releaseStops(load);
            }
            continueStores(shuttle);
            return;
        }
        NodePos home = shuttle.garage.home;
        List<NodePos> homePath = home == null ? List.of() : path(shuttle, at, home::equals);
        if (homePath.isEmpty()) {
            parkWithCargo(shuttle);
            return;
        }
        shuttle.leg = shuttle.leg == Leg.STRANDED ? Leg.STRANDED : Leg.HOME;
        shuttle.motion = new ShuttleMotion(homePath);
    }

    /** Releases the locks of a load's stops and forgets them, so its cargo is routed again. */
    private void releaseStops(Load load) {
        if (!load.stops.isEmpty()) {
            host.releaseLocks(load.job.id(), load.stops);
            load.stops.clear();
        }
    }

    /** Whether every node the motion still has ahead is a rail or segment of this warehouse. */
    private boolean pathWhole(ShuttleMotion motion) {
        for (NodePos node : motion.remaining()) {
            if (!host.isTravelNode(node)) {
                return false;
            }
        }
        return true;
    }

    /** The rails the shuttle's leg ends on: where its lead job goes next, or its home. */
    private Set<NodePos> legGoals(Shuttle shuttle) {
        ShuttleJob lead = shuttle.lead();
        if (shuttle.leg == Leg.HOME || shuttle.leg == Leg.STRANDED || lead == null) {
            return shuttle.garage.home == null ? Set.of() : Set.of(shuttle.garage.home);
        }
        if (shuttle.leg == Leg.TO_SOURCE) {
            return firstGoals(lead);
        }
        BlockPos goal = switch (shuttle.leg) {
            case TO_TERMINAL -> lead instanceof PickJob pick ? pick.terminal() : null;
            case TO_TARGET -> lead instanceof FluidJob ? fluidTargetOf(shuttle) : firstStop(shuttle);
            case TO_STATION -> lead instanceof PutAwayJob putAway ? putAway.station()
                    : lead instanceof FluidJob fluid ? fluid.source() : null;
            default -> null;
        };
        return goal == null ? Set.of() : host.dockRails(Conversions.toNodePos(goal));
    }

    private static BlockPos firstStop(Shuttle shuttle) {
        Load load = firstWithStop(shuttle);
        return load == null ? null : load.stops.get(0);
    }

    private static BlockPos fluidTargetOf(Shuttle shuttle) {
        TankRef target = shuttle.loads.get(0).fluidTarget;
        return target == null ? null : target.gantry();
    }

    /** The shuttles of a garage that are out, as a tag the garage keeps; empty when none is out. */
    public CompoundTag saveGarage(BlockPos pos, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        Garage garage = garages.get(pos);
        if (garage == null) {
            return tag;
        }
        ListTag saved = new ListTag();
        for (Shuttle shuttle : garage.shuttles) {
            if (shuttle.motion != null && shuttle.out()) {
                saved.add(FleetSaves.saveShuttle(shuttle, registries));
            }
        }
        if (!saved.isEmpty()) {
            tag.put("Shuttles", saved);
        }
        return tag;
    }

    /**
     * Sends the saved shuttles of a garage that just joined back out: each
     * slot that holds a shuttle and is inside takes its saved state and waits
     * for its path's chunks before it goes on. Their jobs leave the queue.
     */
    public void restoreGarage(BlockPos pos, CompoundTag tag, HolderLookup.Provider registries) {
        Garage garage = garages.get(pos);
        ListTag saved = tag.getListOrEmpty("Shuttles");
        if (garage == null || saved.isEmpty()) {
            return;
        }
        for (int i = 0; i < saved.size(); i++) {
            CompoundTag entry = saved.getCompoundOrEmpty(i);
            Shuttle shuttle = garage.inSlot(FleetSaves.slotOf(entry));
            if (shuttle == null || shuttle.out() || !FleetSaves.restoreShuttle(shuttle, entry, registries)) {
                continue;
            }
            lifts.release(shuttle);
            shuttle.stop = 0;
            shuttle.stopAt = null;
            shuttle.speed = 0;
            shuttle.speedFor = null;
            shuttle.paidFor = null;
            shuttle.paidThrough = -1;
            shuttle.pumping = null;
            shuttle.restoring = new Settle(RESTORE_SETTLE_TICKS);
            Set<Integer> carried = new HashSet<>();
            for (Load load : shuttle.loads) {
                carried.add(load.job.id());
            }
            jobs.removeIf(job -> carried.contains(job.id()));
        }
        assignWait = RESTORE_SETTLE_TICKS;
        syncPending = true;
    }

    /** The queued jobs in their order, as a tag the controller keeps. */
    public ListTag saveQueue(HolderLookup.Provider registries) {
        return FleetSaves.saveJobs(jobs, registries);
    }

    /** Queues saved jobs again behind those queued now, skipping any whose id is already queued or carried. */
    public void restoreQueue(ListTag saved, HolderLookup.Provider registries) {
        List<ShuttleJob> restored = FleetSaves.loadJobs(saved, registries);
        if (restored.isEmpty()) {
            return;
        }
        for (ShuttleJob job : restored) {
            if (!knows(job.id())) {
                jobs.add(job);
            }
        }
        assignWait = RESTORE_SETTLE_TICKS;
        syncPending = true;
    }

    /** Forgets every queued job; the controller took them along when it left. */
    public void clearQueue() {
        jobs.clear();
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
                        : path(shuttle, at, home::equals);
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
     * its top speed, braking for the end of its path and for the lift car and
     * slowing to its corner speed where the path turns. A new path starts from standing.
     */
    private double drive(Shuttle shuttle) {
        ShuttleMotion motion = shuttle.motion;
        if (motion != shuttle.speedFor) {
            shuttle.speedFor = motion;
            shuttle.speed = 0;
        }
        List<NodePos> path = motion.path();
        double toStop = ShuttleDrive.toStop(path.size(), motion.segment(), motion.progress(), i -> vertical(path, i));
        double limit = ShuttleDrive.cornerLimit(path.size(), motion.segment(), motion.progress(), i -> corner(path, i),
                shuttle.profile.cornerSpeed(), shuttle.profile.accel());
        shuttle.speed = ShuttleDrive.next(shuttle.speed, Math.min(shuttle.profile.speed(), limit), shuttle.profile.accel(), toStop);
        // Never zero, so a shuttle standing at the start of a ride still asks the gate, which hands it to the car.
        return Math.max(shuttle.speed, 1e-4);
    }

    private static boolean corner(List<NodePos> path, int i) {
        if (i <= 0 || i >= path.size() - 1) {
            return false;
        }
        NodePos a = path.get(i - 1);
        NodePos b = path.get(i);
        NodePos c = path.get(i + 1);
        return ShuttleDrive.corner(a.x(), a.y(), a.z(), b.x(), b.y(), b.z(), c.x(), c.y(), c.z());
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
            if (motion == null || (!shuttle.busy() || shuttle.leg == Leg.STRANDED) && motion.arrived()) {
                continue;
            }
            List<BlockPos> path = new ArrayList<>();
            for (NodePos node : motion.path()) {
                path.add(Conversions.toBlockPos(node));
            }
            boolean still = shuttle.riding || motion.held() || shuttle.stop > 0 || shuttle.handover > 0 || motion != shuttle.speedFor;
            ShuttleView.Drive shown = still ? ShuttleView.Drive.STILL
                    : new ShuttleView.Drive((float) shuttle.speed, (float) shuttle.profile.speed(), (float) shuttle.profile.accel(),
                            (float) shuttle.profile.cornerSpeed());
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
