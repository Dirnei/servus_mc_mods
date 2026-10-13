package io.github.dirnei.mc.servus.foundation.machine.pump;

/**
 * An open belt over two pulleys in one plane, in pixels: a small driving
 * pulley and a larger driven one. Gives the two straight strands from tangent
 * point to tangent point, the arcs where the belt wraps round the outer side
 * of each pulley, the driving pulley's angle for a number of turns of the
 * driven one, and where a lacing mark sits on the closed loop as the belt
 * runs. Both pulleys turn the same way, counterclockwise for positive turns
 * with x to the right and y up. The radii are the belt's middle line around
 * each pulley. Look only, no balance.
 *
 * <p>The loop runs forward from the start of strand 0 on the driving pulley:
 * up strand 0, round the driven pulley's arc, down strand 1 and round the
 * driving pulley's arc back to the start.
 */
public final class BeltDrive {

    private final float driverX;
    private final float driverY;
    private final float driverR;
    private final float drivenX;
    private final float drivenY;
    private final float drivenR;
    private final float[][] strands = new float[2][];
    /** The angle in radians from either pulley's centre to strand 0's and strand 1's tangent points. */
    private final double[] normals = new double[2];

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
            normals[side] = normal;
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

    /** The driving pulley (0) or the driven pulley (1): {centre x, centre y, radius}. */
    private float[] pulley(int pulley) {
        return pulley == 0 ? new float[] {driverX, driverY, driverR} : new float[] {drivenX, drivenY, drivenR};
    }

    /**
     * Where the belt's arc round a pulley starts, in radians from its centre:
     * on the driven pulley (1) at strand 0's end, on the driving pulley (0) at
     * strand 1's end. The arc runs counterclockwise from there.
     */
    public double arcStart(int pulley) {
        return pulley == 1 ? normals[0] : normals[1];
    }

    /** How far the belt wraps round a pulley's outer side, in radians: over the driven pulley's top, under the driving one. */
    public double arcSpan(int pulley) {
        double over = normals[1] - normals[0];
        return pulley == 1 ? over : 2 * Math.PI - over;
    }

    /** The length of the belt's arc round a pulley. */
    public float arcLength(int pulley) {
        return (float) (arcSpan(pulley) * pulley(pulley)[2]);
    }

    /** The point {x, y} on a pulley's arc, a fraction 0 to 1 of the way along it. */
    public float[] arcPoint(int pulley, double fraction) {
        float[] p = pulley(pulley);
        double angle = arcStart(pulley) + arcSpan(pulley) * fraction;
        return new float[] {(float) (p[0] + p[2] * Math.cos(angle)), (float) (p[1] + p[2] * Math.sin(angle))};
    }

    /** The whole closed loop: both strands and both arcs. */
    public float loopLength() {
        return length(0) + arcLength(1) + length(1) + arcLength(0);
    }

    /**
     * Where a lacing mark sits on the loop after {@code drivenTurns} turns, as
     * the distance from the loop's start, 0 to below its length. The mark
     * starts {@code offset} pixels along the loop and runs with the belt.
     */
    public float markOnLoop(double drivenTurns, double offset) {
        float loop = loopLength();
        double at = (travel(drivenTurns) + offset) % loop;
        if (at < 0) {
            at += loop;
        }
        return at >= loop ? 0 : (float) at;
    }

    /**
     * The point on the loop at a distance from its start, wrapped onto the
     * loop, as {x, y, heading}: the heading is the belt's running direction
     * there, in radians from the x axis.
     */
    public float[] pointOnLoop(double distance) {
        double loop = loopLength();
        double at = distance % loop;
        if (at < 0) {
            at += loop;
        }
        float up = length(0);
        if (at < up) {
            return onStrand(0, (float) at, false);
        }
        at -= up;
        float over = arcLength(1);
        if (at < over) {
            return onArc(1, at / over);
        }
        at -= over;
        float down = length(1);
        if (at < down) {
            return onStrand(1, (float) (down - at), true);
        }
        at -= down;
        return onArc(0, Math.min(1, at / arcLength(0)));
    }

    private float[] onStrand(int side, float distance, boolean back) {
        float[] s = strands[side];
        float[] point = pointAlong(side, distance);
        double heading = Math.atan2(s[3] - s[1], s[2] - s[0]) + (back ? Math.PI : 0);
        return new float[] {point[0], point[1], (float) heading};
    }

    private float[] onArc(int pulley, double fraction) {
        float[] point = arcPoint(pulley, fraction);
        double heading = arcStart(pulley) + arcSpan(pulley) * fraction + Math.PI / 2;
        return new float[] {point[0], point[1], (float) heading};
    }

    /** The point {x, y} at a distance along a strand from its start. */
    public float[] pointAlong(int side, float distance) {
        float[] s = strands[side];
        float t = distance / length(side);
        return new float[] {s[0] + (s[2] - s[0]) * t, s[1] + (s[3] - s[1]) * t};
    }
}
