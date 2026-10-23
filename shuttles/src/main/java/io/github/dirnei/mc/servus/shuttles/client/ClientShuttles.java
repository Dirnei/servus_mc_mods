package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleSyncPayload;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Client side: the last known shuttles per garage and when they arrived, the
 * shuttles that drove into the garage and are still drawn there until the
 * shutter is down, and the shutter's openness.
 */
public final class ClientShuttles {

    /** A shuttle whose last view ended inside its garage, with the client tick that view arrived. */
    public record Parked(ShuttleView view, long receivedTick) {
    }

    /** One garage's shuttles as last sent, the ones parking inside and its shutter. */
    public static final class Fleet {

        private final List<ShuttleView> views;
        private final long receivedTick;
        private final List<Parked> parked;
        /** How far the shutter is open, 0 shut to 1 open, and the client time it was last moved; NaN before. */
        double openness;
        double shutterTime = Double.NaN;

        Fleet(List<ShuttleView> views, long receivedTick, List<Parked> parked) {
            this.views = views;
            this.receivedTick = receivedTick;
            this.parked = parked;
        }

        public List<ShuttleView> views() {
            return views;
        }

        public long receivedTick() {
            return receivedTick;
        }

        /** Shuttles that came home and are drawn inside until the shutter is down. */
        public List<Parked> parked() {
            return parked;
        }
    }

    /** Blocks before the garage within which a shuttle whose view ends is taken as parked rather than sent elsewhere. */
    private static final double PARKING_REACH = 2.5;
    private static final Map<BlockPos, Fleet> FLEETS = new HashMap<>();

    private ClientShuttles() {
    }

    /**
     * Takes the garage's new views. A shuttle that was drawn on its last
     * blocks into the garage and is no longer sent has parked: it stays drawn
     * inside until the shutter is down, unless a shuttle now waits inside to leave.
     */
    public static void update(ShuttleSyncPayload payload) {
        long now = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
        BlockPos garage = payload.garage();
        Fleet old = FLEETS.get(garage);
        List<Parked> parked = new ArrayList<>();
        if (old != null) {
            parked.addAll(old.parked);
            List<ShuttleView> unmatched = new ArrayList<>(payload.shuttles());
            for (ShuttleView view : old.views) {
                if (!takeSamePath(unmatched, view) && drivingIn(view, garage, now - old.receivedTick)) {
                    parked.add(new Parked(view, old.receivedTick));
                }
            }
        }
        if (payload.shuttles().stream().anyMatch(view -> view.path().getFirst().equals(garage) && view.segment() == 0)) {
            parked.clear();
        }
        Fleet fleet = new Fleet(payload.shuttles(), now, parked);
        if (old != null) {
            fleet.openness = old.openness;
            fleet.shutterTime = old.shutterTime;
        }
        FLEETS.put(garage, fleet);
    }

    /** Removes the first view on the same path as {@code view}, the same trip sent again; false when there is none. */
    private static boolean takeSamePath(List<ShuttleView> views, ShuttleView view) {
        for (int i = 0; i < views.size(); i++) {
            if (views.get(i).path().equals(view.path())) {
                views.remove(i);
                return true;
            }
        }
        return false;
    }

    /** Whether the view's path ends inside the garage and the shuttle is drawn within the last blocks before it by now. */
    private static boolean drivingIn(ShuttleView view, BlockPos garage, double elapsed) {
        List<BlockPos> path = view.path();
        if (path.size() < 2 || !path.getLast().equals(garage)) {
            return false;
        }
        double[] at = ShuttleGlide.at(view, elapsed);
        return path.size() - 1 - (at[0] + at[1]) <= PARKING_REACH;
    }

    public static Fleet fleet(BlockPos garage) {
        return FLEETS.get(garage);
    }

    public static void forget(BlockPos garage) {
        FLEETS.remove(garage);
    }

    public static void clear() {
        FLEETS.clear();
    }
}
