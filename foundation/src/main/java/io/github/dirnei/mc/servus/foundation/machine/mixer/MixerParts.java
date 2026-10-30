package io.github.dirnei.mc.servus.foundation.machine.mixer;

/**
 * The Mixer's parts in pixels, built facing north (front at z 0), as {x1, y1, z1, x2, y2, z2}: heavy iron base, an
 * open steel tub with a brass rim, a steel bridge across the rim, the paddle that turns about the vertical axis through
 * the block's centre, the chute out of the front and the fluid port on the back. Shape, models and renderer share them.
 */
public final class MixerParts {

    public static final float[] BASE = {0, 0, 0, 16, 4, 16};
    public static final float[] FLOOR = {2.5F, 4, 2.5F, 13.5F, 5, 13.5F};

    /** The front and back walls run the tub's full width; the side walls end at their inner faces. */
    public static final float[] WALL_FRONT = {1, 4, 1, 15, 13, 2.5F};
    public static final float[] WALL_BACK = {1, 4, 13.5F, 15, 13, 15};
    public static final float[] WALL_LEFT = {1, 4, 2.5F, 2.5F, 13, 13.5F};
    public static final float[] WALL_RIGHT = {13.5F, 4, 2.5F, 15, 13, 13.5F};

    /** The rim sits on the wall tops and overhangs them by half a pixel outside. */
    public static final float[] RIM_FRONT = {0.5F, 13, 0.5F, 15.5F, 14, 2.5F};
    public static final float[] RIM_BACK = {0.5F, 13, 13.5F, 15.5F, 14, 15.5F};
    public static final float[] RIM_LEFT = {0.5F, 13, 2.5F, 2.5F, 14, 13.5F};
    public static final float[] RIM_RIGHT = {13.5F, 13, 2.5F, 15.5F, 14, 13.5F};

    public static final float[] BRIDGE = {1.5F, 14, 7, 14.5F, 15, 9};

    /** The paddle turns about the vertical axis through this point. */
    public static final float AXIS_X = 8;
    public static final float AXIS_Z = 8;
    public static final float[] HUB = {6.5F, 15, 6.5F, 9.5F, 16, 9.5F};
    /** The shaft's top ends inside the bridge. */
    public static final float[] SHAFT = {7.25F, 6.5F, 7.25F, 8.75F, 14.9F, 8.75F};
    public static final float[] BLADE_A = {3.5F, 6, 7.5F, 12.5F, 10, 8.5F};
    /** The second blade, a hair lower at the top and higher at the bottom, so its faces never share a plane with the first's. */
    public static final float[] BLADE_B = {7.5F, 6.005F, 3.5F, 8.5F, 9.995F, 12.5F};

    public static final float[] CHUTE = {6, 0.5F, -2, 10, 3.5F, 0};
    /** A brass stub off centre on the back wall, clear of a crank flange or motor coupling on the back. */
    public static final float[] PORT = {11.5F, 5.5F, 15, 14, 8.5F, 16.5F};

    /** The height of the contents layer the renderer draws inside the tub, and its extent. */
    public static final float CONTENTS_Y = 9;
    public static final float[] CONTENTS = {2.5F, CONTENTS_Y, 2.5F, 13.5F, CONTENTS_Y, 13.5F};

    private MixerParts() {
    }

    /** The static parts of the block model. */
    public static float[][] staticParts() {
        return new float[][] {BASE, FLOOR, WALL_FRONT, WALL_BACK, WALL_LEFT, WALL_RIGHT, RIM_FRONT, RIM_BACK, RIM_LEFT, RIM_RIGHT, BRIDGE};
    }

    /** The parts inside the block, for its shape; the chute's and port's tips stay out, so the shape fits the block. */
    public static float[][] shapeParts() {
        return new float[][] {BASE, FLOOR, WALL_FRONT, WALL_BACK, WALL_LEFT, WALL_RIGHT, RIM_FRONT, RIM_BACK, RIM_LEFT, RIM_RIGHT, BRIDGE, HUB};
    }
}
