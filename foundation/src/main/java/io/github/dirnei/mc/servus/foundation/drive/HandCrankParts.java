package io.github.dirnei.mc.servus.foundation.drive;

/**
 * The Hand Crank's parts in pixels, built facing north: the driven block is
 * south of it (z 16), the handle points north. As {x1, y1, z1, x2, y2, z2}.
 * The flange reaches one pixel into the driven block, so it meets a housing
 * that is inset by a pixel. Shaft, arm and handle turn about the z axis
 * through {@link #AXIS_X}, {@link #AXIS_Y}. Pure Java.
 */
public final class HandCrankParts {

    public static final float AXIS_X = 8;
    public static final float AXIS_Y = 8;

    public static final float[] FLANGE = {5, 5, 15, 11, 11, 17};
    public static final float[] BEARING = {6.5F, 6.5F, 13, 9.5F, 9.5F, 15};
    public static final float[] SHAFT = {7.25F, 7.25F, 11, 8.75F, 8.75F, 13};
    public static final float[] ARM = {7, 7, 10, 9, 14, 11};
    public static final float[] HANDLE = {7.5F, 12.5F, 6, 8.5F, 13.5F, 10};

    /** The flange and bearing within the block, for the shape. */
    public static final float[] MOUNT_SPACE = {5, 5, 13, 11, 11, 16};
    /** The space the arm and handle sweep, for the shape. */
    public static final float[] SWEEP_SPACE = {2, 2, 6, 14, 14, 13};

    private HandCrankParts() {
    }

    /** The parts that do not turn, for the model. */
    public static float[][] staticParts() {
        return new float[][] {FLANGE, BEARING};
    }

    /** The parts that turn with the handle, for the model and the renderer. */
    public static float[][] turningParts() {
        return new float[][] {SHAFT, ARM, HANDLE};
    }

    /** The parts of the shape, all within the block. */
    public static float[][] shapeParts() {
        return new float[][] {MOUNT_SPACE, SWEEP_SPACE};
    }
}
