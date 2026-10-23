package io.github.dirnei.mc.servus.shuttles.garage;

/**
 * The doorway in the back of the garage and its roll-up shutter, in pixels,
 * 16 to a block, for a garage whose back faces south: the opening runs from
 * x {@link #DOOR_X0} to {@link #DOOR_X1} and from the floor to
 * {@link #DOOR_TOP}; the slats hang just inside it and stack up into the
 * lintel as the shutter opens. Shared by the block model and the renderer.
 */
public final class GarageDoorLook {

    public static final int DOOR_X0 = 2;
    public static final int DOOR_X1 = 14;
    public static final int DOOR_TOP = 12;
    /** Slats in the shutter, each this tall with a gap of {@link #SLAT_GAP} below the next. */
    public static final int SLATS = 8;
    public static final float SLAT_HEIGHT = 1.5F;
    public static final float SLAT_GAP = 0.1F;
    /** The slats' depth inside the back wall, which spans z 15 to 16. */
    public static final float SLAT_Z0 = 15.3F;
    public static final float SLAT_Z1 = 15.7F;
    /** How far the slats stay off the posts beside the opening and off the floor. */
    public static final float SLAT_INSET = 0.05F;

    private GarageDoorLook() {
    }

    /**
     * The bottom of each slat for a shutter that far open, 0 shut to 1 open:
     * the curtain's lowest edge rises from the floor to the lintel and the
     * slats above it stack in order; a slat at or above the lintel is hidden,
     * {@link Float#NaN}.
     */
    public static float[] slatBottoms(double openness) {
        double open = Math.clamp(openness, 0.0, 1.0);
        float[] bottoms = new float[SLATS];
        float lowest = SLAT_INSET + (float) (DOOR_TOP * open);
        for (int i = 0; i < SLATS; i++) {
            float bottom = lowest + i * SLAT_HEIGHT;
            bottoms[i] = bottom < DOOR_TOP - 0.01F ? bottom : Float.NaN;
        }
        return bottoms;
    }
}
