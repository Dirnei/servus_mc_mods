package io.github.dirnei.mc.servus.delicacies.aquarium;

import java.util.Random;

/**
 * A smooth swimming path: per axis the sum of two sines with frequencies and phases from a seed, mapped into a box.
 * Pure Java; every client with the same seed draws the same path.
 */
public final class FishPath {

    /** A box to swim in, in any unit. */
    public record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

        /** This box shrunk by {@code margin} on every side and with its top at most {@code surface} minus the margin. */
        public Bounds inside(double margin, double surface) {
            double x0 = minX + margin;
            double x1 = Math.max(x0, maxX - margin);
            double z0 = minZ + margin;
            double z1 = Math.max(z0, maxZ - margin);
            double y0 = minY + margin;
            double y1 = Math.max(y0, Math.min(maxY, surface) - margin);
            return new Bounds(x0, y0, z0, x1, y1, z1);
        }
    }

    /** A point on the path and the heading there: yaw around the vertical axis and pitch, in radians. */
    public record Pose(double x, double y, double z, double yaw, double pitch) {
    }

    private static final double MAIN = 0.65;
    private static final double SIDE = 0.35;

    private final double[] slow = new double[3];
    private final double[] fast = new double[3];
    private final double[] slowPhase = new double[3];
    private final double[] fastPhase = new double[3];

    public FishPath(long seed) {
        Random random = new Random(seed);
        for (int axis = 0; axis < 3; axis++) {
            double vertical = axis == 1 ? 0.5 : 1.0;
            slow[axis] = (0.010 + random.nextDouble() * 0.012) * vertical;
            fast[axis] = (0.025 + random.nextDouble() * 0.020) * vertical;
            slowPhase[axis] = random.nextDouble() * Math.PI * 2;
            fastPhase[axis] = random.nextDouble() * Math.PI * 2;
        }
    }

    /** Where the fish is at {@code time} (ticks of swimming) in {@code bounds}, and where it heads. */
    public Pose at(double time, Bounds bounds) {
        double x = place(0, time, bounds.minX(), bounds.maxX());
        double y = place(1, time, bounds.minY(), bounds.maxY());
        double z = place(2, time, bounds.minZ(), bounds.maxZ());
        double dx = slope(0, time) * (bounds.maxX() - bounds.minX());
        double dy = slope(1, time) * (bounds.maxY() - bounds.minY());
        double dz = slope(2, time) * (bounds.maxZ() - bounds.minZ());
        double yaw = Math.atan2(dx, dz);
        double pitch = Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
        return new Pose(x, y, z, yaw, pitch);
    }

    /** The axis value from -1 to 1 mapped into {@code min} to {@code max}. */
    private double place(int axis, double time, double min, double max) {
        double wave = MAIN * Math.sin(slow[axis] * time + slowPhase[axis]) + SIDE * Math.sin(fast[axis] * time + fastPhase[axis]);
        return min + (wave + 1) / 2 * (max - min);
    }

    private double slope(int axis, double time) {
        return (MAIN * slow[axis] * Math.cos(slow[axis] * time + slowPhase[axis])
                + SIDE * fast[axis] * Math.cos(fast[axis] * time + fastPhase[axis])) / 2;
    }
}
