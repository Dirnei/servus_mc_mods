package io.github.dirnei.mc.servus.shuttles.gantry;

/**
 * The look of a Fluid Gantry in pixels, shared by its block models and its
 * renderer, and the fill step of a tank column as clients learn it. Pure
 * Java. Coordinates are for a rail running north to south, in the block of
 * the part (16 to a block); a leg's tank column runs from its lower block's
 * plinth up to its upper block's cap.
 */
public final class GantryLook {

    /** Top of the steel plinth in the lower leg block. */
    public static final float PLINTH_TOP = 2;
    /** Bottom of the steel cap in the upper leg block. */
    public static final float CAP_BOTTOM = 13;
    /** How far the glass sits inside the outer faces of a leg. */
    public static final float GLASS_INSET = 0.3F;
    /** How far the fluid sits inside the outer faces of a leg, well inside the glass. */
    public static final float FLUID_INSET = 1.5F;
    /** Gap between the fluid and the plinth below and the cap above. */
    public static final float FLUID_GAP = 0.05F;
    /** The lock plates on the upper leg block: 6 by 6 px, centred across, on both faces along the rail. */
    public static final float PLATE_X0 = 5;
    public static final float PLATE_X1 = 11;
    public static final float PLATE_Y0 = 3;
    public static final float PLATE_Y1 = 9;
    /** The plate's outer face beyond the block face, and its inner face just outside the glass. */
    public static final float PLATE_OUT = 0.175F;
    public static final float PLATE_IN = 0.275F;
    /** The window of the lock plate texture, in texels (16 across the plate): where the locked fluid shows. */
    public static final int WINDOW_U0 = 3;
    public static final int WINDOW_U1 = 13;
    public static final int WINDOW_V0 = 3;
    public static final int WINDOW_V1 = 10;

    private GantryLook() {
    }

    /**
     * The step of the column: 0 when empty, {@code steps} only when full, any
     * fluid in between at least 1 and below {@code steps}.
     */
    public static int level(int amount, int capacity, int steps) {
        if (amount <= 0 || capacity <= 0 || steps <= 0) {
            return 0;
        }
        if (amount >= capacity) {
            return steps;
        }
        int level = (int) ((long) amount * steps / capacity);
        return Math.max(1, Math.min(steps - 1, level));
    }
}
