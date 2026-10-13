package io.github.dirnei.mc.servus.storage.shuttle;

import java.util.function.IntPredicate;

/**
 * How fast a shuttle drives from tick to tick: it speeds up to its top speed
 * and brakes so it stands still where it has to stop, at the end of its path
 * and before a vertical step, which only the lift car makes. The server moves
 * shuttles with it and clients extrapolate them with it, so both agree.
 * Path segments are one block long. Pure Java.
 */
public final class ShuttleDrive {

    private ShuttleDrive() {
    }

    /**
     * The speed for the next tick, which is also the distance it covers: one
     * step faster, at most the top speed, slow enough to brake before the stop
     * and never past it.
     *
     * @param toStop blocks left to the next place the shuttle stops
     */
    public static double next(double speed, double top, double accel, double toStop) {
        if (top <= 0 || toStop <= 0) {
            return 0;
        }
        double v = Math.min(top, speed + accel);
        // Braking at accel per tick from v covers about v * v / (2 * accel).
        v = Math.min(v, Math.sqrt(2 * accel * toStop));
        // Crawl the last bit instead of creeping towards the stop forever.
        v = Math.max(v, Math.min(accel, toStop));
        return Math.min(v, toStop);
    }

    /**
     * Blocks from the position to the next stop: the start of the first
     * vertical step ahead, else the end of the path.
     *
     * @param nodes    nodes on the path
     * @param vertical whether the step from node i to node i + 1 goes up or down
     */
    public static double toStop(int nodes, int segment, double progress, IntPredicate vertical) {
        for (int i = segment; i < nodes - 1; i++) {
            if (vertical.test(i)) {
                return i == segment ? 0 : (i - segment) - progress;
            }
        }
        return Math.max(0, (nodes - 1 - segment) - progress);
    }

    /** Where a shuttle is after driving the given ticks: {segment, progress, speed}. Fractions of a tick move part of a step. */
    public static double[] glide(int nodes, int segment, double progress, double speed, double top, double accel, double ticks,
                                 IntPredicate vertical) {
        double v = speed;
        int whole = (int) Math.min(Math.floor(ticks), 400);
        double[] at = {segment, progress};
        for (int t = 0; t < whole; t++) {
            v = next(v, top, accel, toStop(nodes, (int) at[0], at[1], vertical));
            move(at, v, nodes);
        }
        double part = ticks - Math.floor(ticks);
        if (part > 0) {
            double step = next(v, top, accel, toStop(nodes, (int) at[0], at[1], vertical));
            move(at, step * part, nodes);
        }
        return new double[] {at[0], at[1], v};
    }

    private static void move(double[] at, double blocks, int nodes) {
        at[1] += blocks;
        while (at[0] < nodes - 1 && at[1] >= 1.0 - 1e-9) {
            at[1] = Math.max(0, at[1] - 1.0);
            at[0]++;
        }
        if (at[0] >= nodes - 1) {
            at[0] = nodes - 1;
            at[1] = 0;
        }
    }
}
