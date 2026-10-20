package io.github.dirnei.mc.servus.foundation.machine.mill;

/**
 * The Mill's parts in pixels, built facing north (front at z 0), as
 * {x1, y1, z1, x2, y2, z2}: heavy iron base, steel housing, the grinding wheel
 * that turns about the vertical axis through the block's centre, the hopper
 * with its brass rim and the chute out of the front. Shape, models and
 * renderer share them.
 */
public final class MillParts {

    public static final float[] BASE = {0, 0, 0, 16, 5, 16};
    public static final float[] HOUSING = {1, 5, 1, 15, 10, 15};

    /** The wheel turns about the vertical axis through this point. */
    public static final float AXIS_X = 8;
    public static final float AXIS_Z = 8;
    /** The wheel rests on the housing top. */
    public static final float WHEEL_Y = HOUSING[4];
    /**
     * How far each further rim segment's top sits lower, in pixels: tops of
     * overlapping neighbours in one plane would flicker.
     */
    public static final float DEPTH_STEP = 0.005F;
    /** Segments of the rim; the renderer turns one segment by 360 / this each time. */
    public static final int RIM_SEGMENTS = 12;
    /** One rim segment at angle 0, along +x: tangent to the wheel's edge. */
    public static final float[] RIM_SEGMENT = {AXIS_X + 5.2F, WHEEL_Y, AXIS_Z - 1.8F, AXIS_X + 6.6F, WHEEL_Y + 1.5F, AXIS_Z + 1.8F};
    /** Spokes from the hub to the rim; the renderer turns one spoke by 360 / this each time. */
    public static final int SPOKES = 6;
    /** One spoke at angle 0, from inside the hub into the rim, lower than both. */
    public static final float[] SPOKE = {AXIS_X + 1.5F, WHEEL_Y, AXIS_Z - 0.4F, AXIS_X + 5.4F, WHEEL_Y + 1, AXIS_Z + 0.4F};
    /** The brass hub, drawn twice with the second turned by 45 degrees so it reads round. */
    public static final float[] HUB = {AXIS_X - 1.8F, WHEEL_Y, AXIS_Z - 1.8F, AXIS_X + 1.8F, WHEEL_Y + 2, AXIS_Z + 1.8F};
    /** The hub's second box, a hair lower so the two tops never share a plane. */
    public static final float[] HUB_TURNED = {HUB[0], HUB[1], HUB[2], HUB[3], HUB[4] - 0.05F, HUB[5]};

    public static final float[] HOPPER = {4.5F, HUB[4], 4.5F, 11.5F, 15, 11.5F};
    public static final float[] HOPPER_RIM = {4, 15, 4, 12, 16, 12};
    /** The chute sticks out of the front like a spout. */
    public static final float[] CHUTE = {6, 1.5F, -2, 10, 4.5F, 0};
    /** The space the wheel turns in: for the shape only. */
    public static final float[] WHEEL_SPACE = {AXIS_X - 6.6F, WHEEL_Y, AXIS_Z - 6.6F, AXIS_X + 6.6F, RIM_SEGMENT[4], AXIS_Z + 6.6F};

    private MillParts() {
    }

    /** The parts inside the block, for its shape; the chute's tip stays out, so the shape fits the block. */
    public static float[][] shapeParts() {
        return new float[][] {BASE, HOUSING, WHEEL_SPACE, HOPPER, HOPPER_RIM};
    }

    /** One rim segment's box with its top lowered for copy {@code index}. */
    public static float[] rimSegment(int index) {
        float[] segment = RIM_SEGMENT.clone();
        segment[4] -= index * DEPTH_STEP;
        return segment;
    }
}
