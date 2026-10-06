package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.lib.graph.NodePos;
import io.github.dirnei.mc.servus.lib.graph.Side;
import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The shuttles of one warehouse, kept per garage: each parks on its garage's
 * rail, all serve one queue of pick and put-away jobs, and clients draw them
 * per garage. Everything here is data; nothing scans blocks.
 */
public final class ShuttleFleet {

    /** What the fleet needs from its warehouse. */
    public interface Host {
        ServerLevel level();

        /** Rails and elevator segments the graph links to this node. */
        Set<NodePos> travelNeighbours(NodePos node);

        /** Whether the node is a rail or an elevator segment of this warehouse. */
        boolean isTravelNode(NodePos node);

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
        ItemStack deliverToTerminal(PickJob job, ItemStack stack);

        /** Puts an instant load into the output slots, holding what does not fit until there is room. */
        void deliverInstant(PickJob job, ItemStack stack);

        double shuttleSpeed();
    }

    /** Where a shuttle is heading. */
    private enum Leg { TO_SOURCE, TO_TARGET, TO_STATION, TO_TERMINAL, WAITING_ROOM, HOME }

    private static final int SYNC_INTERVAL = 5;

    private final Host host;
    private final Map<BlockPos, Garage> garages = new LinkedHashMap<>();
    private final ArrayDeque<ShuttleJob> jobs = new ArrayDeque<>();
    private boolean syncPending;
    private boolean wasMoving;
    private int syncTimer;

    public ShuttleFleet(Host host) {
        this.host = host;
    }

    // Fleet shape

    public int garageCount() {
        return garages.size();
    }

    public int size() {
        int count = 0;
        for (Garage garage : garages.values()) {
            count += garage.shuttles.size();
        }
        return count;
    }

    public int busy() {
        int count = 0;
        for (Garage garage : garages.values()) {
            for (Shuttle shuttle : garage.shuttles) {
                if (shuttle.job != null) {
                    count++;
                }
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

    /** Whether any garage with a rail has a shuttle. */
    public boolean hasShuttles() {
        for (Garage garage : garages.values()) {
            if (garage.home != null && !garage.shuttles.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Called when a garage joins or its count changes: match its shuttle list to the count. */
    public void adoptGarage(BlockPos pos, int count) {
        Garage garage = garages.computeIfAbsent(pos, Garage::new);
        refreshHome(garage);
        while (garage.shuttles.size() < count) {
            Shuttle shuttle = new Shuttle(garage);
            shuttle.motion = garage.home == null ? null : ShuttleMotion.parked(garage.home);
            garage.shuttles.add(shuttle);
        }
        while (garage.shuttles.size() > count) {
            dropCargo(garage.shuttles.remove(garage.shuttles.size() - 1));
        }
        syncPending = true;
    }

    /** Called when a garage leaves: cargo drops there, jobs go back to the queue, the shuttles are forgotten. */
    public void releaseGarage(BlockPos pos) {
        Garage garage = garages.remove(pos);
        if (garage == null) {
            return;
        }
        for (Shuttle shuttle : garage.shuttles) {
            dropCargo(shuttle);
        }
        garage.shuttles.clear();
        syncPending = true;
    }

    /** Takes over another fleet's garages and queue, for network merges. */
    public void absorb(ShuttleFleet other) {
        garages.putAll(other.garages);
        jobs.addAll(other.jobs);
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

    private void refreshHome(Garage garage) {
        NodePos previous = garage.home;
        garage.home = null;
        for (NodePos neighbour : host.travelNeighbours(Conversions.toNodePos(garage.pos))) {
            garage.home = neighbour;
            break;
        }
        if (garage.home == previous) {
            return;
        }
        for (Shuttle shuttle : garage.shuttles) {
            if (shuttle.job == null) {
                shuttle.motion = garage.home == null ? null : ShuttleMotion.parked(garage.home);
            }
        }
        syncPending = true;
    }

    // Reach

    /** Travel nodes touching a position. */
    private Set<NodePos> railsBeside(NodePos node) {
        Set<NodePos> result = new HashSet<>();
        for (Side side : Side.ALL) {
            NodePos beside = node.relative(side);
            if (host.isTravelNode(beside)) {
                result.add(beside);
            }
        }
        return result;
    }

    /** Whether some garage with a rail and a shuttle can drive to a rail beside the position. */
    public boolean canReach(BlockPos pos) {
        Set<NodePos> rails = railsBeside(Conversions.toNodePos(pos));
        if (rails.isEmpty()) {
            return false;
        }
        for (Garage garage : garages.values()) {
            if (garage.home != null && !garage.shuttles.isEmpty()
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

    // Jobs

    public void requestPick(PickJob job) {
        jobs.add(job);
        syncPending = true;
    }

    public void requestPutAway(PutAwayJob job) {
        jobs.add(job);
        syncPending = true;
    }

    private Set<NodePos> firstGoals(ShuttleJob job) {
        return job instanceof PickJob pick ? railsBeside(Conversions.toNodePos(pick.source()))
                : railsBeside(Conversions.toNodePos(((PutAwayJob) job).station()));
    }

    /** Queued picks of a request. */
    public int queuedFor(int requestId) {
        int count = 0;
        for (ShuttleJob job : jobs) {
            if (job instanceof PickJob pick && pick.requestId() == requestId) {
                count++;
            }
        }
        return count;
    }

    /** Picks of a request a shuttle is working on. */
    public int ridingFor(int requestId) {
        int count = 0;
        for (Garage garage : garages.values()) {
            for (Shuttle shuttle : garage.shuttles) {
                if (shuttle.job instanceof PickJob pick && pick.requestId() == requestId) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Drops the queued picks of a request; loads already on a shuttle still arrive. */
    public void removeQueued(int requestId) {
        jobs.removeIf(job -> job instanceof PickJob pick && pick.requestId() == requestId);
    }

    // Ticking

    public void tick() {
        if (!hasShuttles()) {
            serveInstantly();
            return;
        }
        assignJobs();
        boolean moving = false;
        for (Garage garage : garages.values()) {
            for (Shuttle shuttle : garage.shuttles) {
                if (shuttle.leg == Leg.WAITING_ROOM && shuttle.job instanceof PickJob pick) {
                    shuttle.cargo = host.deliverToTerminal(pick, shuttle.cargo);
                    if (shuttle.cargo.isEmpty()) {
                        shuttle.job = null;
                        goHome(shuttle);
                        syncPending = true;
                    }
                    continue;
                }
                if (shuttle.motion != null && !shuttle.motion.arrived()) {
                    moving = true;
                    if (shuttle.motion.advance(host.shuttleSpeed())) {
                        arrive(shuttle);
                    }
                }
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

    /** Hands queued jobs to the free shuttle with the shortest way, in queue order. */
    private void assignJobs() {
        while (!jobs.isEmpty()) {
            List<Shuttle> free = freeShuttles();
            if (free.isEmpty()) {
                return;
            }
            ShuttleJob job = jobs.poll();
            Set<NodePos> goals = firstGoals(job);
            Shuttle best = null;
            List<NodePos> bestPath = null;
            if (!goals.isEmpty()) {
                for (Shuttle shuttle : free) {
                    List<NodePos> path = RailPaths.find(shuttle.motion.currentNode(), goals::contains, host::travelNeighbours);
                    if (!path.isEmpty() && (bestPath == null || path.size() < bestPath.size())) {
                        best = shuttle;
                        bestPath = path;
                    }
                }
            }
            if (best == null) {
                serveInstantly(job);
                continue;
            }
            best.job = job;
            best.leg = Leg.TO_SOURCE;
            best.motion = new ShuttleMotion(bestPath);
            syncPending = true;
        }
    }

    private List<Shuttle> freeShuttles() {
        List<Shuttle> free = new ArrayList<>();
        for (Garage garage : garages.values()) {
            if (garage.home == null) {
                continue;
            }
            for (Shuttle shuttle : garage.shuttles) {
                if (shuttle.job == null && shuttle.motion != null) {
                    free.add(shuttle);
                }
            }
        }
        return free;
    }

    private void serveInstantly() {
        while (!jobs.isEmpty()) {
            serveInstantly(jobs.poll());
        }
    }

    /** Does a job without a shuttle: picks extract anywhere, put-aways move station to target at once. */
    private void serveInstantly(ShuttleJob job) {
        if (job instanceof PickJob pick) {
            if (host.isRack(pick.source())) {
                host.pickLoaded(pick, 0);
                return;
            }
            ItemStack taken = host.extractFrom(pick.source(), pick.key(), pick.amount());
            host.pickLoaded(pick, taken.getCount());
            host.deliverInstant(pick, taken);
        } else if (job instanceof PutAwayJob putAway) {
            ItemStack taken = host.takeFromStation(putAway.station(), putAway.slot(), putAway.key(), putAway.amount());
            if (!taken.isEmpty()) {
                host.returnToStation(putAway.station(), host.insertAt(putAway.target(), taken));
            }
        }
    }

    private void arrive(Shuttle shuttle) {
        if (shuttle.job == null) {
            park(shuttle);
            syncPending = true;
            return;
        }
        if (shuttle.job instanceof PickJob pick) {
            arrivePick(shuttle, pick);
        } else if (shuttle.job instanceof PutAwayJob putAway) {
            arrivePutAway(shuttle, putAway);
        }
        syncPending = true;
    }

    private void arrivePick(Shuttle shuttle, PickJob job) {
        switch (shuttle.leg) {
            case TO_SOURCE -> {
                shuttle.cargo = host.extractFrom(job.source(), job.key(), job.amount());
                host.pickLoaded(job, shuttle.cargo.getCount());
                if (shuttle.cargo.isEmpty()) {
                    shuttle.job = null;
                    goHome(shuttle);
                } else if (!go(shuttle, Leg.TO_TERMINAL, railsBeside(Conversions.toNodePos(job.terminal())))) {
                    // The terminal lost its rail meanwhile: the load still has to arrive somewhere safe.
                    host.deliverInstant(job, shuttle.cargo);
                    shuttle.cargo = ItemStack.EMPTY;
                    shuttle.job = null;
                    goHome(shuttle);
                }
            }
            case TO_TERMINAL -> {
                shuttle.cargo = host.deliverToTerminal(job, shuttle.cargo);
                if (shuttle.cargo.isEmpty()) {
                    shuttle.job = null;
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

    private void arrivePutAway(Shuttle shuttle, PutAwayJob job) {
        switch (shuttle.leg) {
            case TO_SOURCE -> {
                shuttle.cargo = host.takeFromStation(job.station(), job.slot(), job.key(), job.amount());
                if (shuttle.cargo.isEmpty()) {
                    shuttle.job = null;
                    goHome(shuttle);
                } else if (!go(shuttle, Leg.TO_TARGET, railsBeside(Conversions.toNodePos(job.target())))) {
                    host.returnToStation(job.station(), shuttle.cargo);
                    shuttle.cargo = ItemStack.EMPTY;
                    goHome(shuttle);
                }
            }
            case TO_TARGET -> {
                shuttle.cargo = host.insertAt(job.target(), shuttle.cargo);
                if (shuttle.cargo.isEmpty()) {
                    shuttle.job = null;
                    goHome(shuttle);
                } else if (!go(shuttle, Leg.TO_STATION, railsBeside(Conversions.toNodePos(job.station())))) {
                    host.returnToStation(job.station(), shuttle.cargo);
                    shuttle.cargo = ItemStack.EMPTY;
                    goHome(shuttle);
                }
            }
            case TO_STATION -> {
                host.returnToStation(job.station(), shuttle.cargo);
                shuttle.cargo = ItemStack.EMPTY;
                shuttle.job = null;
                goHome(shuttle);
            }
            case HOME -> park(shuttle);
        }
    }

    /** Sends the shuttle towards any of the goal rails; false when no path exists. */
    private boolean go(Shuttle shuttle, Leg leg, Set<NodePos> goals) {
        if (goals.isEmpty()) {
            return false;
        }
        List<NodePos> path = RailPaths.find(shuttle.motion.end(), goals::contains, host::travelNeighbours);
        if (path.isEmpty()) {
            return false;
        }
        shuttle.leg = leg;
        shuttle.motion = new ShuttleMotion(path);
        return true;
    }

    private void goHome(Shuttle shuttle) {
        shuttle.leg = Leg.HOME;
        NodePos home = shuttle.garage.home;
        List<NodePos> homePath = home == null ? List.of() : RailPaths.find(shuttle.motion.end(), home::equals, host::travelNeighbours);
        shuttle.motion = homePath.isEmpty() ? shuttle.motion.reverse() : new ShuttleMotion(homePath);
    }

    private void park(Shuttle shuttle) {
        shuttle.job = null;
        shuttle.leg = Leg.HOME;
        shuttle.motion = shuttle.garage.home == null ? null : ShuttleMotion.parked(shuttle.garage.home);
    }

    /**
     * Cargo pops at the shuttle's garage. A job that was not loaded yet goes
     * back to the front of the queue; a loaded pick counts as missing.
     */
    private void dropCargo(Shuttle shuttle) {
        boolean loaded = !shuttle.cargo.isEmpty();
        if (loaded) {
            if (shuttle.job instanceof PickJob pick) {
                host.pickDropped(pick, shuttle.cargo.getCount());
            }
            Block.popResource(host.level(), shuttle.garage.pos, shuttle.cargo);
            shuttle.cargo = ItemStack.EMPTY;
        }
        if (shuttle.job != null) {
            if (!(shuttle.job instanceof PickJob) || shuttle.leg == Leg.TO_SOURCE) {
                jobs.addFirst(shuttle.job);
            }
            shuttle.job = null;
        }
    }

    // Sync

    private void send() {
        for (Garage garage : garages.values()) {
            PacketDistributor.sendToPlayersTrackingChunk(host.level(), new ChunkPos(garage.pos),
                    new ShuttleSyncPayload(garage.pos, views(garage.pos)));
        }
    }

    /** The shuttles of one garage as clients draw them; shuttles parked inside a rail-less garage are left out. */
    public List<ShuttleView> views(BlockPos garagePos) {
        List<ShuttleView> list = new ArrayList<>();
        Garage garage = garages.get(garagePos);
        if (garage == null) {
            return list;
        }
        float speed = (float) host.shuttleSpeed();
        for (Shuttle shuttle : garage.shuttles) {
            ShuttleMotion motion = shuttle.motion;
            if (motion == null) {
                continue;
            }
            List<BlockPos> path = new ArrayList<>();
            for (NodePos node : motion.path()) {
                path.add(Conversions.toBlockPos(node));
            }
            list.add(new ShuttleView(path, motion.segment(), (float) motion.progress(), speed, shuttle.cargo.copy()));
        }
        return list;
    }

    /** Queued jobs that are put-aways. */
    public int queuedPutAways() {
        int count = 0;
        for (ShuttleJob job : jobs) {
            if (job instanceof PutAwayJob) {
                count++;
            }
        }
        return count;
    }

    public int queued() {
        return jobs.size();
    }

    public String summary() {
        if (garages.isEmpty()) {
            return "no garages";
        }
        String rail = withoutRail() == 0 ? "" : ", " + withoutRail() + " parked without a rail";
        return garages.size() + " garages, " + size() + " shuttles, " + busy() + " busy, " + jobs.size() + " queued ("
                + queuedPutAways() + " put-aways)" + rail;
    }

    /** One garage's line: how many it holds, how many are out, whether it has a rail. */
    public String garageSummary(BlockPos pos) {
        Garage garage = garages.get(pos);
        if (garage == null) {
            return "not in a warehouse";
        }
        int out = 0;
        for (Shuttle shuttle : garage.shuttles) {
            if (shuttle.job != null) {
                out++;
            }
        }
        return garage.shuttles.size() + " shuttles, " + out + " out" + (garage.home == null ? ", no rail" : "");
    }

    /** A garage: its position, home rail and shuttles. */
    private static final class Garage {
        final BlockPos pos;
        NodePos home;
        final List<Shuttle> shuttles = new ArrayList<>();

        Garage(BlockPos pos) {
            this.pos = pos;
        }
    }

    /** One shuttle: where it is, what it carries, what it is doing, where it lives. */
    private static final class Shuttle {
        final Garage garage;
        ShuttleMotion motion;
        ItemStack cargo = ItemStack.EMPTY;
        ShuttleJob job;
        Leg leg = Leg.HOME;

        Shuttle(Garage garage) {
            this.garage = garage;
        }
    }
}
