package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.lib.graph.NodePos;
import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.rail.ElevatorBlockEntity;
import io.github.dirnei.mc.servus.storage.shuttle.Shafts.Shaft;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
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

        double shuttleSpeed();

        /** Blocks per tick the lift car of a shaft moves. */
        double liftSpeed();

        /** The shaft the node is a segment of, or null when it is not an elevator segment. */
        Shaft shaftOf(NodePos node);
    }

    /** Where a shuttle is heading. */
    private enum Leg { TO_SOURCE, TO_TARGET, TO_STATION, TO_TERMINAL, WAITING_ROOM, HOME }

    private static final int SYNC_INTERVAL = 5;

    private final Host host;
    private final Map<BlockPos, Garage> garages = new LinkedHashMap<>();
    private final ArrayDeque<ShuttleJob> jobs = new ArrayDeque<>();
    /** One lift car per shaft, keyed by the shaft's lowest segment. */
    private final Map<BlockPos, LiftCar> lifts = new LinkedHashMap<>();
    /** Bottoms of the cars that moved on the last tick, to notice starts and stops. */
    private final Set<BlockPos> movingLifts = new HashSet<>();
    private static int nextShuttleId;
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

    /** Called when a garage joins or its count or pause state changes: match its shuttle list to the count. */
    public void adoptGarage(BlockPos pos, int count, boolean active) {
        Garage garage = garages.computeIfAbsent(pos, Garage::new);
        garage.active = active;
        refreshHome(garage);
        while (garage.shuttles.size() < count) {
            Shuttle shuttle = new Shuttle(garage);
            shuttle.motion = garage.home == null ? null : ShuttleMotion.parked(garage.home);
            garage.shuttles.add(shuttle);
        }
        while (garage.shuttles.size() > count) {
            Shuttle removed = garage.shuttles.remove(garage.shuttles.size() - 1);
            releaseLift(removed);
            dropCargo(removed);
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
            releaseLift(shuttle);
            dropCargo(shuttle);
        }
        garage.shuttles.clear();
        syncPending = true;
    }

    /** Takes over another fleet's garages and queue, for network merges. */
    public void absorb(ShuttleFleet other) {
        garages.putAll(other.garages);
        jobs.addAll(other.jobs);
        lifts.putAll(other.lifts);
        other.garages.clear();
        other.jobs.clear();
        other.lifts.clear();
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
        if (garage.home == null) {
            // No way home any more: everything out comes in now rather than freezing on the rails.
            recallShuttles(garage);
            return;
        }
        for (Shuttle shuttle : garage.shuttles) {
            if (shuttle.job == null) {
                releaseLift(shuttle);
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
            releaseLift(shuttle);
            dropCargo(shuttle);
            shuttle.leg = Leg.HOME;
            shuttle.motion = garage.home == null ? null : ShuttleMotion.parked(garage.home);
        }
        syncPending = true;
    }

    // Reach

    /** Whether some garage with a rail and a shuttle can drive to a dock rail of the position. */
    public boolean canReach(BlockPos pos) {
        Set<NodePos> rails = host.dockRails(Conversions.toNodePos(pos));
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
        return job instanceof PickJob pick ? host.dockRails(Conversions.toNodePos(pick.source()))
                : host.dockRails(Conversions.toNodePos(((PutAwayJob) job).station()));
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

    /** Items of the key on queued picks bound for a destination. */
    public int queuedAmount(BlockPos destination, ItemKey key) {
        int count = 0;
        for (ShuttleJob job : jobs) {
            if (job instanceof PickJob pick && pick.terminal().equals(destination) && pick.key().equals(key)) {
                count += pick.amount();
            }
        }
        return count;
    }

    /** Items of the key on picks a shuttle is working on, bound for a destination. */
    public int ridingAmount(BlockPos destination, ItemKey key) {
        int count = 0;
        for (Garage garage : garages.values()) {
            for (Shuttle shuttle : garage.shuttles) {
                if (shuttle.job instanceof PickJob pick && pick.terminal().equals(destination) && pick.key().equals(key)) {
                    count += pick.amount();
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
        // Shuttles already out keep moving even when no garage has a home; only new jobs need one.
        if (hasShuttles()) {
            assignJobs();
        } else {
            serveInstantly();
        }
        double liftSpeed = host.liftSpeed();
        boolean moving = false;
        for (Map.Entry<BlockPos, LiftCar> entry : lifts.entrySet()) {
            LiftCar car = entry.getValue();
            car.tick(liftSpeed);
            if (car.moving() != movingLifts.contains(entry.getKey())) {
                if (car.moving()) {
                    movingLifts.add(entry.getKey());
                } else {
                    movingLifts.remove(entry.getKey());
                    parkOnBlock(entry.getKey(), car);
                }
                sendLift(entry.getKey(), car);
            }
            moving |= car.moving();
        }
        for (Garage garage : garages.values()) {
            for (Shuttle shuttle : garage.shuttles) {
                if (shuttle.leg == Leg.WAITING_ROOM && shuttle.job instanceof PickJob pick) {
                    shuttle.cargo = host.deliverTo(pick, shuttle.cargo);
                    if (shuttle.cargo.isEmpty()) {
                        shuttle.job = null;
                        goHome(shuttle);
                        syncPending = true;
                    }
                    continue;
                }
                if (shuttle.motion == null) {
                    continue;
                }
                if (shuttle.riding) {
                    moving = true;
                    if (!rideOver(shuttle)) {
                        continue;
                    }
                    if (shuttle.motion.arrived()) {
                        arrive(shuttle);
                        continue;
                    }
                }
                if (!shuttle.motion.arrived()) {
                    moving = true;
                    boolean wasHeld = shuttle.motion.held();
                    boolean done = shuttle.motion.advance(host.shuttleSpeed(), segment -> mayStart(shuttle, segment));
                    observeLift(shuttle, shuttle.motion.currentNode());
                    if (wasHeld != shuttle.motion.held()) {
                        syncPending = true;
                    }
                    if (done) {
                        arrive(shuttle);
                    }
                } else if (shuttle.job != null) {
                    // A one node leg (already standing beside the goal) arrives without moving.
                    arrive(shuttle);
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
            if (garage.home == null || !garage.active) {
                continue;
            }
            for (Shuttle shuttle : garage.shuttles) {
                if (shuttle.job == null && shuttle.motion != null && shuttle.lift == null) {
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
                } else if (!go(shuttle, Leg.TO_TERMINAL, host.dockRails(Conversions.toNodePos(job.terminal())))) {
                    // The terminal lost its rail meanwhile: the load still has to arrive somewhere safe.
                    host.deliverInstant(job, shuttle.cargo);
                    shuttle.cargo = ItemStack.EMPTY;
                    shuttle.job = null;
                    goHome(shuttle);
                }
            }
            case TO_TERMINAL -> {
                shuttle.cargo = host.deliverTo(job, shuttle.cargo);
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
                } else if (!go(shuttle, Leg.TO_TARGET, host.dockRails(Conversions.toNodePos(job.target())))) {
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
                } else if (!go(shuttle, Leg.TO_STATION, host.dockRails(Conversions.toNodePos(job.station())))) {
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
        releaseLift(shuttle);
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

    // Lifts

    /** The network's shafts changed: keep, resize, drop and create cars to match. */
    public void refreshShafts(List<Shaft> shafts) {
        Map<BlockPos, Shaft> byBottom = new LinkedHashMap<>();
        for (Shaft shaft : shafts) {
            byBottom.put(Conversions.toBlockPos(shaft.bottom()), shaft);
        }
        for (Map.Entry<BlockPos, LiftCar> entry : new ArrayList<>(lifts.entrySet())) {
            BlockPos bottom = entry.getKey();
            LiftCar car = entry.getValue();
            Shaft shaft = byBottom.get(bottom);
            if (shaft == null) {
                lifts.remove(bottom);
                breakLift(bottom);
            } else if (shaft.topY() < car.topY()) {
                car.resize(shaft.bottomY(), shaft.topY());
                breakLift(bottom);
            } else if (shaft.topY() != car.topY()) {
                car.resize(shaft.bottomY(), shaft.topY());
            }
        }
        for (Map.Entry<BlockPos, Shaft> entry : byBottom.entrySet()) {
            if (!lifts.containsKey(entry.getKey())) {
                lifts.put(entry.getKey(), new LiftCar(entry.getValue().bottomY(), entry.getValue().topY()));
            }
        }
        movingLifts.retainAll(lifts.keySet());
        for (Map.Entry<BlockPos, LiftCar> entry : lifts.entrySet()) {
            parkOnBlock(entry.getKey(), entry.getValue());
        }
        syncPending = true;
    }

    /** The car of the shaft at that bottom, or null. For tests and the network. */
    public LiftCar lift(BlockPos bottom) {
        return lifts.get(bottom);
    }

    /**
     * The shaft lost segments: its riders and callers lose their cargo and go
     * home, or into their garage when no way home is left. Crossers carry on.
     */
    private void breakLift(BlockPos bottom) {
        for (Garage garage : garages.values()) {
            for (Shuttle shuttle : garage.shuttles) {
                if (!bottom.equals(shuttle.lift) || shuttle.crossing) {
                    continue;
                }
                releaseLift(shuttle);
                dropCargo(shuttle);
                shuttle.leg = Leg.HOME;
                NodePos home = garage.home;
                NodePos at = shuttle.motion == null ? null : shuttle.motion.currentNode();
                List<NodePos> path = home == null || at == null || !host.isTravelNode(at) ? List.of()
                        : RailPaths.find(at, home::equals, host::travelNeighbours);
                if (path.isEmpty()) {
                    park(shuttle);
                } else {
                    shuttle.motion = new ShuttleMotion(path);
                }
                syncPending = true;
            }
        }
    }

    /** Forgets the shuttle's call, ride or crossing. */
    private void releaseLift(Shuttle shuttle) {
        if (shuttle.lift != null) {
            LiftCar car = lifts.get(shuttle.lift);
            if (car != null) {
                car.remove(shuttle.id);
            }
        }
        shuttle.lift = null;
        shuttle.crossing = false;
        shuttle.inside = false;
        shuttle.riding = false;
    }

    private LiftCar carFor(Shaft shaft) {
        return shaft == null ? null : lifts.get(Conversions.toBlockPos(shaft.bottom()));
    }

    private static boolean inShaft(BlockPos bottom, LiftCar car, NodePos node) {
        return node.x() == bottom.getX() && node.z() == bottom.getZ() && node.y() >= car.bottomY() && node.y() <= car.topY();
    }

    /** Notes when a shuttle got onto a shaft and lets the car go once it is off again. */
    private void observeLift(Shuttle shuttle, NodePos at) {
        if (shuttle.lift == null) {
            return;
        }
        LiftCar car = lifts.get(shuttle.lift);
        if (car == null) {
            releaseLift(shuttle);
            return;
        }
        if (inShaft(shuttle.lift, car, at)) {
            shuttle.inside = true;
        } else if (shuttle.inside) {
            releaseLift(shuttle);
        }
    }

    /**
     * The gate before each path segment: shuttles wait for the car before a
     * ride, wait for a folded deck before a crossing, and never drive up or
     * down a shaft by themselves.
     */
    private boolean mayStart(Shuttle shuttle, int segment) {
        List<NodePos> path = shuttle.motion.path();
        observeLift(shuttle, path.get(segment));
        ShaftSteps.Kind kind = ShaftSteps.classify(path, segment, host::shaftOf);
        switch (kind) {
            case ENTER_RIDE -> {
                NodePos entry = path.get(segment + 1);
                Shaft shaft = host.shaftOf(entry);
                LiftCar car = carFor(shaft);
                if (car == null) {
                    return false;
                }
                if (car.rider() != null && car.rider() == shuttle.id) {
                    return true;
                }
                bindLift(shuttle, shaft, false);
                car.call(shuttle.id, entry.y());
                if (car.canBoard(shuttle.id, entry.y())) {
                    car.board(shuttle.id);
                    syncPending = true;
                    return true;
                }
                return false;
            }
            case ENTER_CROSS -> {
                NodePos entry = path.get(segment + 1);
                Shaft shaft = host.shaftOf(entry);
                LiftCar car = carFor(shaft);
                if (car == null) {
                    return false;
                }
                if (car.isCrossing(shuttle.id)) {
                    return true;
                }
                if (car.canCross(entry.y())) {
                    bindLift(shuttle, shaft, true);
                    car.beginCross(shuttle.id, entry.y());
                    return true;
                }
                return false;
            }
            case RIDE -> {
                NodePos from = path.get(segment);
                Shaft shaft = host.shaftOf(from);
                LiftCar car = carFor(shaft);
                if (car == null) {
                    return false;
                }
                boolean rider = car.rider() != null && car.rider() == shuttle.id;
                if (!rider) {
                    // A leg that starts on a segment without the car: call it to this floor first.
                    bindLift(shuttle, shaft, false);
                    shuttle.inside = true;
                    car.call(shuttle.id, from.y());
                    if (!car.canBoard(shuttle.id, from.y())) {
                        return false;
                    }
                    car.board(shuttle.id);
                }
                bindLift(shuttle, shaft, false);
                shuttle.inside = true;
                shuttle.rideEnd = ShaftSteps.rideEnd(path, segment);
                car.send(shuttle.id, path.get(shuttle.rideEnd).y());
                shuttle.riding = true;
                syncPending = true;
                return false;
            }
            default -> {
                return true;
            }
        }
    }

    private void bindLift(Shuttle shuttle, Shaft shaft, boolean crossing) {
        BlockPos bottom = Conversions.toBlockPos(shaft.bottom());
        if (!bottom.equals(shuttle.lift)) {
            releaseLift(shuttle);
            shuttle.lift = bottom;
        }
        shuttle.crossing = crossing;
    }

    /** Whether the car has brought the riding shuttle to its floor; moves it to the exit node when so. */
    private boolean rideOver(Shuttle shuttle) {
        LiftCar car = shuttle.lift == null ? null : lifts.get(shuttle.lift);
        if (car == null) {
            shuttle.riding = false;
            return true;
        }
        int floor = shuttle.motion.path().get(shuttle.rideEnd).y();
        if (!car.arrivedFor(shuttle.id, floor)) {
            return false;
        }
        shuttle.motion.skipTo(shuttle.rideEnd);
        shuttle.riding = false;
        syncPending = true;
        return true;
    }

    // Sync

    private void send() {
        for (Garage garage : garages.values()) {
            PacketDistributor.sendToPlayersTrackingChunk(host.level(), new ChunkPos(garage.pos),
                    new ShuttleSyncPayload(garage.pos, views(garage.pos)));
        }
        for (Map.Entry<BlockPos, LiftCar> entry : lifts.entrySet()) {
            sendLift(entry.getKey(), entry.getValue());
        }
    }

    private void sendLift(BlockPos bottom, LiftCar car) {
        Integer target = car.target();
        float speed = car.moving() ? (float) host.liftSpeed() : 0;
        PacketDistributor.sendToPlayersTrackingChunk(host.level(), new ChunkPos(bottom), new LiftSyncPayload(bottom, car.topY(),
                (float) car.y(), target == null ? (int) Math.round(car.y()) : target, speed, car.carrying()));
    }

    /** Leaves the parked car's height on the lowest segment, for players who load the chunk later. */
    private void parkOnBlock(BlockPos bottom, LiftCar car) {
        if (host.level().getBlockEntity(bottom) instanceof ElevatorBlockEntity segment) {
            segment.parkLift((float) car.y());
        }
    }

    /** The shuttles of one garage as clients draw them: only those out on the rails, idle ones sit inside the garage. */
    public List<ShuttleView> views(BlockPos garagePos) {
        List<ShuttleView> list = new ArrayList<>();
        Garage garage = garages.get(garagePos);
        if (garage == null) {
            return list;
        }
        float speed = (float) host.shuttleSpeed();
        for (Shuttle shuttle : garage.shuttles) {
            ShuttleMotion motion = shuttle.motion;
            if (motion == null || (shuttle.job == null && motion.arrived())) {
                continue;
            }
            List<BlockPos> path = new ArrayList<>();
            for (NodePos node : motion.path()) {
                path.add(Conversions.toBlockPos(node));
            }
            float shown = shuttle.riding || motion.held() ? 0 : speed;
            Optional<BlockPos> lift = shuttle.riding && shuttle.lift != null ? Optional.of(shuttle.lift) : Optional.empty();
            list.add(new ShuttleView(path, motion.segment(), (float) motion.progress(), shown, shuttle.cargo.copy(), lift));
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

    /** Shuttles of one garage that are out on a job. */
    public int outOf(BlockPos pos) {
        Garage garage = garages.get(pos);
        if (garage == null) {
            return 0;
        }
        int out = 0;
        for (Shuttle shuttle : garage.shuttles) {
            if (shuttle.job != null || (shuttle.motion != null && !shuttle.motion.arrived())) {
                out++;
            }
        }
        return out;
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
        return garage.shuttles.size() + " shuttles, " + out + " out" + (garage.home == null ? ", no rail" : "") + (garage.active ? "" : ", paused");
    }

    /** A garage: its position, home rail and shuttles. */
    private static final class Garage {
        final BlockPos pos;
        NodePos home;
        boolean active = true;
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
        final int id = nextShuttleId++;
        /** Bottom of the shaft whose car the shuttle calls, rides or crosses; null when none. */
        BlockPos lift;
        /** Whether the shuttle crosses that shaft's deck rather than calling its car. */
        boolean crossing;
        /** Whether the shuttle has been on a segment of that shaft yet. */
        boolean inside;
        /** Whether the car carries the shuttle right now, towards path node rideEnd. */
        boolean riding;
        int rideEnd;

        Shuttle(Garage garage) {
            this.garage = garage;
        }
    }
}
