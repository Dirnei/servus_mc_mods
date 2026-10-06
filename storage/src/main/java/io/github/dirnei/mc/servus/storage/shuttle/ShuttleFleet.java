package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.lib.graph.NodePos;
import io.github.dirnei.mc.servus.lib.graph.Side;
import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The shuttles of one warehouse: parked on the home rail, sent out for pick
 * and put-away jobs, drawn on clients. Everything here is data; nothing
 * scans blocks.
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

        double shuttleSpeed();
    }

    /** Where a shuttle is heading. */
    private enum Leg { TO_SOURCE, TO_TARGET, TO_STATION, HOME }

    private static final int SYNC_INTERVAL = 5;

    private final Host host;
    private final List<Shuttle> shuttles = new ArrayList<>();
    private final ArrayDeque<ShuttleJob> jobs = new ArrayDeque<>();
    private BlockPos controller;
    private NodePos home;
    private boolean syncPending;
    private boolean wasMoving;
    private int syncTimer;

    public ShuttleFleet(Host host) {
        this.host = host;
    }

    // Fleet shape

    public int size() {
        return shuttles.size();
    }

    public int busy() {
        int count = 0;
        for (Shuttle shuttle : shuttles) {
            if (shuttle.job != null) {
                count++;
            }
        }
        return count;
    }

    public boolean hasHome() {
        return home != null;
    }

    /** The controller this fleet belongs to, or null while no controller is in the network. */
    public BlockPos controller() {
        return controller;
    }

    /** Called when a controller joins: adopt its shuttle count. */
    public void adopt(BlockPos controllerPos, int count) {
        controller = controllerPos;
        refreshHome();
        while (shuttles.size() < count) {
            shuttles.add(new Shuttle());
        }
        while (shuttles.size() > count) {
            Shuttle removed = shuttles.remove(shuttles.size() - 1);
            dropCargo(removed);
        }
        syncPending = true;
    }

    /** Called when the controller leaves: cargo drops there and the shuttles are forgotten. */
    public void release() {
        for (Shuttle shuttle : shuttles) {
            dropCargo(shuttle);
        }
        shuttles.clear();
        for (ShuttleJob job : jobs) {
            if (job instanceof PutAwayJob putAway) {
                host.releaseClaim(putAway.station(), putAway.slot());
            }
        }
        jobs.clear();
        controller = null;
        home = null;
    }

    /** Takes over another fleet's shuttles and queue, for network merges. */
    public void absorb(ShuttleFleet other) {
        if (controller == null) {
            controller = other.controller;
        }
        shuttles.addAll(other.shuttles);
        jobs.addAll(other.jobs);
        other.shuttles.clear();
        other.jobs.clear();
        refreshHome();
        syncPending = true;
    }

    /** Recomputes the home rail; call after membership changes. */
    public void refreshHome() {
        home = null;
        if (controller == null) {
            return;
        }
        for (NodePos neighbour : host.travelNeighbours(Conversions.toNodePos(controller))) {
            home = neighbour;
            break;
        }
        for (Shuttle shuttle : shuttles) {
            if (shuttle.motion == null && home != null) {
                shuttle.motion = ShuttleMotion.parked(home);
            }
        }
    }

    // Jobs

    /** Whether a pull of this key should ride a shuttle instead of being instant. */
    public boolean canShuttle(ItemKey key) {
        if (shuttles.isEmpty() || home == null) {
            return false;
        }
        for (BlockPos source : host.sourcesHolding(key)) {
            if (!railsBeside(Conversions.toNodePos(source)).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Whether a put-away from the station to the target rides a shuttle: both reachable from home. */
    public boolean canCarry(BlockPos station, BlockPos target) {
        if (shuttles.isEmpty() || home == null) {
            return false;
        }
        Set<NodePos> stationRails = railsBeside(Conversions.toNodePos(station));
        Set<NodePos> targetRails = railsBeside(Conversions.toNodePos(target));
        if (stationRails.isEmpty() || targetRails.isEmpty()) {
            return false;
        }
        return !RailPaths.find(home, stationRails::contains, host::travelNeighbours).isEmpty()
                && !RailPaths.find(home, targetRails::contains, host::travelNeighbours).isEmpty();
    }

    public void requestPick(PickJob job) {
        jobs.add(job);
        syncPending = true;
    }

    public void requestPutAway(PutAwayJob job) {
        jobs.add(job);
        syncPending = true;
    }

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

    /** Travel nodes beside at least one source holding the key, as a goal test. */
    private Set<NodePos> goals(ItemKey key) {
        Set<NodePos> goals = new HashSet<>();
        for (BlockPos source : host.sourcesHolding(key)) {
            goals.addAll(railsBeside(Conversions.toNodePos(source)));
        }
        return goals;
    }

    // Ticking

    public void tick() {
        if (home == null) {
            serveInstantly();
            return;
        }
        boolean moving = false;
        for (Shuttle shuttle : shuttles) {
            if (shuttle.job == null && !jobs.isEmpty()) {
                assign(shuttle, jobs.poll());
            }
            if (shuttle.motion != null && !shuttle.motion.arrived()) {
                moving = true;
                if (shuttle.motion.advance(host.shuttleSpeed())) {
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

    private void serveInstantly() {
        while (!jobs.isEmpty()) {
            serveInstantly(jobs.poll());
        }
    }

    /** Does a job without a shuttle: picks extract anywhere, put-aways move station to target at once. */
    private void serveInstantly(ShuttleJob job) {
        if (job instanceof PickJob pick) {
            deliver(pick, extractAnywhere(pick));
        } else if (job instanceof PutAwayJob putAway) {
            ItemStack taken = host.takeFromStation(putAway.station(), putAway.slot(), putAway.key(), putAway.amount());
            if (!taken.isEmpty()) {
                host.returnToStation(putAway.station(), host.insertAt(putAway.target(), taken));
            }
        }
    }

    private void assign(Shuttle shuttle, ShuttleJob job) {
        Set<NodePos> goals = job instanceof PickJob pick ? goals(pick.key())
                : railsBeside(Conversions.toNodePos(((PutAwayJob) job).station()));
        NodePos from = shuttle.motion == null ? home : shuttle.motion.currentNode();
        List<NodePos> path = goals.isEmpty() ? List.of() : RailPaths.find(from, goals::contains, host::travelNeighbours);
        if (path.isEmpty()) {
            serveInstantly(job);
            return;
        }
        shuttle.job = job;
        shuttle.leg = Leg.TO_SOURCE;
        shuttle.motion = new ShuttleMotion(path);
        syncPending = true;
    }

    private void arrive(Shuttle shuttle) {
        if (shuttle.job == null) {
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
        if (shuttle.leg == Leg.TO_SOURCE) {
            shuttle.cargo = extractBeside(shuttle.motion.end(), job);
            goHome(shuttle);
            return;
        }
        deliver(job, shuttle.cargo);
        shuttle.cargo = ItemStack.EMPTY;
        park(shuttle);
    }

    private void arrivePutAway(Shuttle shuttle, PutAwayJob job) {
        switch (shuttle.leg) {
            case TO_SOURCE -> {
                shuttle.cargo = host.takeFromStation(job.station(), job.slot(), job.key(), job.amount());
                if (shuttle.cargo.isEmpty()) {
                    goHome(shuttle);
                } else if (!go(shuttle, Leg.TO_TARGET, railsBeside(Conversions.toNodePos(job.target())))) {
                    // No way to the target any more: the stack goes straight back, we are standing at the station.
                    host.returnToStation(job.station(), shuttle.cargo);
                    shuttle.cargo = ItemStack.EMPTY;
                    goHome(shuttle);
                }
            }
            case TO_TARGET -> {
                shuttle.cargo = host.insertAt(job.target(), shuttle.cargo);
                if (shuttle.cargo.isEmpty()) {
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
        List<NodePos> homePath = RailPaths.find(shuttle.motion.end(), home::equals, host::travelNeighbours);
        shuttle.motion = homePath.isEmpty() ? shuttle.motion.reverse() : new ShuttleMotion(homePath);
    }

    private void park(Shuttle shuttle) {
        shuttle.job = null;
        shuttle.leg = Leg.HOME;
        shuttle.motion = ShuttleMotion.parked(home);
    }

    private ItemStack extractBeside(NodePos rail, PickJob job) {
        ItemStack result = ItemStack.EMPTY;
        int wanted = job.amount();
        for (BlockPos source : host.sourcesHolding(job.key())) {
            NodePos node = Conversions.toNodePos(source);
            boolean beside = false;
            for (Side side : Side.ALL) {
                if (node.relative(side).equals(rail)) {
                    beside = true;
                    break;
                }
            }
            if (!beside || wanted <= 0) {
                continue;
            }
            ItemStack taken = host.extractFrom(source, job.key(), wanted);
            if (taken.isEmpty()) {
                continue;
            }
            if (result.isEmpty()) {
                result = taken;
            } else {
                result.grow(taken.getCount());
            }
            wanted -= taken.getCount();
        }
        return result;
    }

    private ItemStack extractAnywhere(PickJob job) {
        ItemStack result = ItemStack.EMPTY;
        int wanted = job.amount();
        for (BlockPos source : host.sourcesHolding(job.key())) {
            if (wanted <= 0) {
                break;
            }
            ItemStack taken = host.extractFrom(source, job.key(), wanted);
            if (taken.isEmpty()) {
                continue;
            }
            if (result.isEmpty()) {
                result = taken;
            } else {
                result.grow(taken.getCount());
            }
            wanted -= taken.getCount();
        }
        return result;
    }

    private void deliver(PickJob job, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (job.requester() != null && job.requester().isAlive() && !job.requester().isRemoved()) {
            job.requester().getInventory().add(stack);
        }
        if (!stack.isEmpty()) {
            Block.popResource(host.level(), job.terminal(), stack);
        }
    }

    private void dropCargo(Shuttle shuttle) {
        if (!shuttle.cargo.isEmpty() && controller != null) {
            Block.popResource(host.level(), controller, shuttle.cargo);
            shuttle.cargo = ItemStack.EMPTY;
        }
        if (shuttle.job != null) {
            jobs.addFirst(shuttle.job);
            shuttle.job = null;
        }
    }

    // Sync

    private void send() {
        if (controller == null) {
            return;
        }
        PacketDistributor.sendToPlayersTrackingChunk(host.level(), new ChunkPos(controller),
                new ShuttleSyncPayload(controller, views()));
    }

    public List<ShuttleView> views() {
        List<ShuttleView> list = new ArrayList<>();
        float speed = (float) host.shuttleSpeed();
        for (Shuttle shuttle : shuttles) {
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

    public String summary() {
        if (shuttles.isEmpty()) {
            return "no shuttles";
        }
        String rail = home == null ? ", parked without a rail" : "";
        return shuttles.size() + " shuttles, " + busy() + " busy, " + jobs.size() + " queued (" + queuedPutAways() + " put-aways)" + rail;
    }

    /** One shuttle: where it is, what it carries, what it is doing. */
    private static final class Shuttle {
        ShuttleMotion motion;
        ItemStack cargo = ItemStack.EMPTY;
        ShuttleJob job;
        Leg leg = Leg.HOME;
    }
}
