package io.github.dirnei.mc.servus.shuttles.fluidstation;

/**
 * The look of a fluid station's lower half, the tank, in block pixels,
 * shared by its model and its renderer. Iron posts and bars {@link #FRAME}
 * thick run along its twelve edges; a glass pane stands in the middle of the
 * frame depth on each side, and the fluid {@link #FLUID_INSET} inside the
 * outer faces, behind the panes. The upper half is the item station's own
 * block model. Pure Java.
 */
public final class FluidStationLook {

    /** Width and depth of the frame posts and bars. */
    public static final float FRAME = 2;
    /** The plane of a glass pane, off the outer face it stands behind, and its half thickness. */
    public static final float GLASS_PLANE = 1;
    public static final float GLASS_HALF = 0.05F;
    /** How far the panes stand off the frame around them. */
    public static final float GLASS_INSET = 0.05F;
    /** How far the fluid's sides stand inside the outer faces: behind the panes. */
    public static final float FLUID_INSET = 1.15F;
    /** The fluid's bottom and highest top, off the iron floor and the top bars. */
    public static final float FLUID_Y0 = FRAME + 0.1F;
    public static final float FLUID_Y1 = 16 - FRAME - 0.1F;

    private FluidStationLook() {
    }

    /** The top of the fluid in a tank filled to {@code fill}, from 0 to 1. */
    public static float fluidTop(float fill) {
        return FLUID_Y0 + (FLUID_Y1 - FLUID_Y0) * fill;
    }
}
