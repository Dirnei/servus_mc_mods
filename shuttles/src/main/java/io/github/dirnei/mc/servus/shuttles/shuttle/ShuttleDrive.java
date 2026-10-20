package io.github.dirnei.mc.servus.shuttles.shuttle;

import java.util.function.IntPredicate;

/**
 * How fast a shuttle drives from tick to tick: it speeds up to its top speed
 * and brakes so it stands still where it has to stop, at the end of its path
 * and before a vertical step, which only the lift car makes, and slows to its
 * corner speed where the path turns. The server moves
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

    /**
     * The highest speed allowed now for the next corner: the corner speed from
     * half a block before the corner to half a block after it, and before that
     * the speed from which braking at accel still gets down to the corner speed
     * in time. All corners share one corner speed, so the nearest is the
     * tightest. Without a corner ahead, or with a corner speed of 0, there is no limit.
     *
     * @param corner whether the path turns at node i
     */
    public static double cornerLimit(int nodes, int segment, double progress, IntPredicate corner, double cornerTop, double accel) {
        if (cornerTop <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        for (int i = segment; i < nodes - 1; i++) {
            double ahead = (i - segment) - progress;
            if (ahead < -0.5 || !corner.test(i)) {
                continue;
            }
            if (ahead <= 0.5) {
                return cornerTop;
            }
            // The step v itself must leave room to brake: v * v + 2 * accel * v <= cornerTop^2 + 2 * accel * gap,
            // so a step that ends inside the corner zone is never faster than cornerTop.
            return -accel + Math.sqrt(accel * accel + cornerTop * cornerTop + 2 * accel * (ahead - 0.5));
        }
        return Double.POSITIVE_INFINITY;
    }

    /** Whether a path turns at b: the steps a to b and b to c are both horizontal and point in different directions. */
    public static boolean corner(int ax, int ay, int az, int bx, int by, int bz, int cx, int cy, int cz) {
        return ay == by && by == cy && (bx - ax != cx - bx || bz - az != cz - bz);
    }

    /** Where a shuttle is after driving the given ticks on a path without corners: {segment, progress, speed}. */
    public static double[] glide(int nodes, int segment, double progress, double speed, double top, double accel, double ticks,
                                 IntPredicate vertical) {
        return glide(nodes, segment, progress, speed, top, accel, ticks, vertical, i -> false, 0);
    }

    /** Where a shuttle is after driving the given ticks: {segment, progress, speed}. Fractions of a tick move part of a step. */
    public static double[] glide(int nodes, int segment, double progress, double speed, double top, double accel, double ticks,
                                 IntPredicate vertical, IntPredicate corner, double cornerTop) {
        double v = speed;
        int whole = (int) Math.min(Math.floor(ticks), 400);
        double[] at = {segment, progress};
        for (int t = 0; t < whole; t++) {
            v = step(nodes, at, v, top, accel, vertical, corner, cornerTop);
            move(at, v, nodes);
        }
        double part = ticks - Math.floor(ticks);
        if (part > 0) {
            double step = step(nodes, at, v, top, accel, vertical, corner, cornerTop);
            move(at, step * part, nodes);
        }
        return new double[] {at[0], at[1], v};
    }

    private static double step(int nodes, double[] at, double v, double top, double accel, IntPredicate vertical, IntPredicate corner,
                               double cornerTop) {
        double limit = Math.min(top, cornerLimit(nodes, (int) at[0], at[1], corner, cornerTop, accel));
        return next(v, limit, accel, toStop(nodes, (int) at[0], at[1], vertical));
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
