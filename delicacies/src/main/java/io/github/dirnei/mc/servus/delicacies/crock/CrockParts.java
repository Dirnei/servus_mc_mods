package io.github.dirnei.mc.servus.delicacies.crock;

/**
 * The parts of the Fermenting Crock in pixels, as {x1, y1, z1, x2, y2, z2}; the crock looks the same from every side.
 * Neck and lip stand on the belly; the channel between lip and neck is the water seal the lid's skirt dips into.
 * Shapes, models and the renderer share them.
 */
public final class CrockParts {

    public static final float[] BELLY = {2, 0, 2, 14, 8, 14};
    public static final float[] NECK_NORTH = {4, 8, 4, 12, 12, 5};
    public static final float[] NECK_SOUTH = {4, 8, 11, 12, 12, 12};
    public static final float[] NECK_WEST = {4, 8, 5, 5, 12, 11};
    public static final float[] NECK_EAST = {11, 8, 5, 12, 12, 11};
    public static final float[] LIP_NORTH = {2, 8, 2, 14, 10, 3};
    public static final float[] LIP_SOUTH = {2, 8, 13, 14, 10, 14};
    public static final float[] LIP_WEST = {2, 8, 3, 3, 10, 13};
    public static final float[] LIP_EAST = {13, 8, 3, 14, 10, 13};
    public static final float[] LID = {3, 12, 3, 13, 13, 13};
    /** The skirt keeps 0.05 px from the lip and the neck on both sides. */
    public static final float[] SKIRT_NORTH = {3.05F, 9, 3.05F, 12.95F, 12, 3.95F};
    public static final float[] SKIRT_SOUTH = {3.05F, 9, 12.05F, 12.95F, 12, 12.95F};
    public static final float[] SKIRT_WEST = {3.05F, 9, 3.95F, 3.95F, 12, 12.05F};
    public static final float[] SKIRT_EAST = {12.05F, 9, 3.95F, 12.95F, 12, 12.05F};
    public static final float[] KNOB = {7, 13, 7, 9, 14.5F, 9};

    public static final float[][] NECK = {NECK_NORTH, NECK_SOUTH, NECK_WEST, NECK_EAST};
    public static final float[][] LIP = {LIP_NORTH, LIP_SOUTH, LIP_WEST, LIP_EAST};

    /** The inside of the neck, where the fluid surface and the items are drawn. */
    public static final float[] INSIDE = {5, 8, 5, 11, 12, 11};
    /** The fluid surface's lowest and highest height. */
    public static final float FLUID_LOW = 8.5F;
    public static final float FLUID_HIGH = 11.5F;

    private CrockParts() {
    }

    /** The parts that make the open crock's shape; the lid adds nothing that matters for collision. */
    public static float[][] shape() {
        return new float[][] {BELLY, NECK_NORTH, NECK_SOUTH, NECK_WEST, NECK_EAST, LIP_NORTH, LIP_SOUTH, LIP_WEST, LIP_EAST};
    }

    /** The parts that make the sealed crock's shape. */
    public static float[][] sealedShape() {
        return new float[][] {BELLY, LIP_NORTH, LIP_SOUTH, LIP_WEST, LIP_EAST, LID, NECK_NORTH, NECK_SOUTH, NECK_WEST, NECK_EAST, KNOB};
    }
}
