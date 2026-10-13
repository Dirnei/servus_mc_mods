package io.github.dirnei.mc.servus.foundation.machine.pump;

/**
 * An open belt over two pulleys in one plane, in pixels: a small driving
 * pulley and a larger driven one. Gives the two straight strands from tangent
 * point to tangent point, the driving pulley's angle for a number of turns of
 * the driven one, and where a lacing mark on each strand sits as the belt
 * runs. Both pulleys turn the same way, counterclockwise for positive turns
 * with x to the right and y up. The radii are the belt's middle line around
 * each pulley. Look only, no balance.
 */
public final class BeltDrive {

    private final float driverX;
    private final float driverY;
    private final float driverR;
    private final float drivenX;
    private final float drivenY;
    private final float drivenR;
    private final float[][] strands = new float[2][];

    public BeltDrive(float driverX, float driverY, float driverR, float drivenX, float drivenY, float drivenR) {
        this.driverX = driverX;
        this.driverY = driverY;
        this.driverR = driverR;
        this.drivenX = drivenX;
        this.drivenY = drivenY;
        this.drivenR = drivenR;
        double dx = drivenX - driverX;
        double dy = drivenY - driverY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double along = Math.atan2(dy, dx);
        // The normal n of an outer tangent satisfies n . (driven - driver) = driverR - drivenR.
        double spread = Math.acos((driverR - drivenR) / distance);
        for (int side = 0; side < 2; side++) {
            double normal = along + (side == 0 ? -spread : spread);
            double nx = Math.cos(normal);
            double ny = Math.sin(normal);
            strands[side] = new float[] {
                    (float) (driverX + driverR * nx), (float) (driverY + driverR * ny),
                    (float) (drivenX + drivenR * nx), (float) (drivenY + drivenR * ny)};
        }
    }

    /** Strand 0 or 1 as {x1, y1, x2, y2}, from its tangent point on the driving pulley to the one on the driven pulley. */
    public float[] strand(int side) {
        return strands[side].clone();
    }

    /** The length of a strand between its tangent points. */
    public float length(int side) {
        float[] s = strands[side];
        return (float) Math.hypot(s[2] - s[0], s[3] - s[1]);
    }

    /** How many times faster the driving pulley turns than the driven one. */
    public float ratio() {
        return drivenR / driverR;
    }

    /** The driving pulley's angle in degrees, 0 to below 360, after {@code drivenTurns} turns of the driven pulley. */
    public float driverAngle(double drivenTurns) {
        double turns = drivenTurns * ratio();
        return (float) (360.0 * (turns - Math.floor(turns)));
    }

    /** How far the belt has run, in pixels, after {@code drivenTurns} turns of the driven pulley. */
    public double travel(double drivenTurns) {
        return drivenTurns * 2 * Math.PI * drivenR;
    }

    /**
     * True if the belt on this strand runs from the driving pulley toward the
     * driven one while the pulleys turn forward.
     */
    public boolean runsUp(int side) {
        float[] s = strands[side];
        // A counterclockwise pulley moves its rim point at offset n along (-ny, nx).
        double nx = s[0] - driverX;
        double ny = s[1] - driverY;
        return -ny * (s[2] - s[0]) + nx * (s[3] - s[1]) > 0;
    }

    /**
     * Where the lacing mark on a strand sits after {@code drivenTurns} turns,
     * as the distance from the strand's start on the driving pulley, 0 to below
     * its length; it runs with the belt and wraps at the strand's end.
     */
    public float markAlong(double drivenTurns, int side) {
        float length = length(side);
        double run = travel(drivenTurns) % length;
        if (run < 0) {
            run += length;
        }
        float at = (float) (runsUp(side) ? run : length - run);
        return at >= length || at < 0 ? 0 : at;
    }

    /** The point {x, y} at a distance along a strand from its start. */
    public float[] pointAlong(int side, float distance) {
        float[] s = strands[side];
        float t = distance / length(side);
        return new float[] {s[0] + (s[2] - s[0]) * t, s[1] + (s[3] - s[1]) * t};
    }
}
