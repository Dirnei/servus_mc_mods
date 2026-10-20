package io.github.dirnei.mc.servus.delicacies.cooking;

/**
 * The parts of the Pot, the Pan, the Baking Oven and the Cutting Board in pixels, built facing north, as {x1, y1, z1, x2, y2, z2}.
 * Shapes, models and renderers share them. Walls and rims stand on the bottom; the side walls end at the inner faces
 * of the front and back walls, so no two faces share a plane.
 */
public final class VesselParts {

    public static final float[] POT_BOTTOM = {2, 0, 2, 14, 1, 14};
    public static final float[] POT_NORTH = {2, 1, 2, 14, 10, 3};
    public static final float[] POT_SOUTH = {2, 1, 13, 14, 10, 14};
    public static final float[] POT_WEST = {2, 1, 3, 3, 10, 13};
    public static final float[] POT_EAST = {13, 1, 3, 14, 10, 13};
    public static final float[] POT_HANDLE_WEST = {0.5F, 7, 7, 2, 8.5F, 9};
    public static final float[] POT_HANDLE_EAST = {14, 7, 7, 15.5F, 8.5F, 9};
    /** The Pot's walls, without the bottom. */
    public static final float[][] POT_WALLS = {POT_NORTH, POT_SOUTH, POT_WEST, POT_EAST};
    /** The inside of the Pot, where the fluid surface and the items are drawn. */
    public static final float[] POT_INSIDE = {3, 1, 3, 13, 10, 13};
    /** The fluid surface's lowest and highest height. */
    public static final float POT_FLUID_LOW = 1.5F;
    public static final float POT_FLUID_HIGH = 9.5F;

    public static final float[] PAN_BOTTOM = {1, 0, 2, 12, 1, 14};
    public static final float[] PAN_NORTH = {1, 1, 2, 12, 2.5F, 3};
    public static final float[] PAN_SOUTH = {1, 1, 13, 12, 2.5F, 14};
    public static final float[] PAN_WEST = {1, 1, 3, 2, 2.5F, 13};
    public static final float[] PAN_EAST = {11, 1, 3, 12, 2.5F, 13};
    public static final float[] PAN_HANDLE = {12, 1.5F, 7.25F, 16, 2.5F, 8.75F};
    /** The Pan's rims, without the bottom. */
    public static final float[][] PAN_RIMS = {PAN_NORTH, PAN_SOUTH, PAN_WEST, PAN_EAST};
    /** The inside of the Pan, where the items lie. */
    public static final float[] PAN_INSIDE = {2, 1, 3, 11, 2.5F, 13};

    public static final float[] OVEN_BODY = {2, 0, 3, 14, 9, 14};
    /** The door stands in front of the body and ends where the body starts. */
    public static final float[] OVEN_DOOR = {3, 1, 2, 13, 8, 3};
    /** The handle stands in front of the door and ends where the door starts. */
    public static final float[] OVEN_HANDLE = {5, 6, 1.2F, 11, 6.8F, 2};

    public static final float[] BOARD = {2, 0, 3, 14, 1, 13};

    private VesselParts() {
    }

    /** The parts that make the Pot's shape. */
    public static float[][] potShape() {
        return new float[][] {POT_BOTTOM, POT_NORTH, POT_SOUTH, POT_WEST, POT_EAST, POT_HANDLE_WEST, POT_HANDLE_EAST};
    }

    /** The parts that make the Baking Oven's shape. */
    public static float[][] ovenShape() {
        return new float[][] {OVEN_BODY, OVEN_DOOR, OVEN_HANDLE};
    }

    /** The parts that make the Pan's shape. */
    public static float[][] panShape() {
        return new float[][] {PAN_BOTTOM, PAN_NORTH, PAN_SOUTH, PAN_WEST, PAN_EAST, PAN_HANDLE};
    }
}
