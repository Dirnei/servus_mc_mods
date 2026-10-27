package io.github.dirnei.mc.servus.shuttles.fluidrack;

import io.github.dirnei.mc.servus.shuttles.shuttle.module.TankerLook;

/**
 * The look of a Fluid Rack bay in block pixels, shared by its models and its
 * renderer, laid out for a rack facing north: the front at z 0, the back
 * board at z 15 to 16. Posts are 2 pixels square at the section's ends; the
 * glass front is inset 0.05 from every frame part; the fluid sits 0.05
 * behind the glass and 0.1 off the floor and the beams. Pure Java.
 */
public final class FluidRackLook {

    /** Width of a post. */
    public static final float POST = 2;
    /** Front of the back posts and back of the fluid space. */
    public static final float BACK_POST_Z = 13;
    /** Front face of the back board. */
    public static final float BOARD_Z = 15;
    /** Top of the floor beam and bottom of the top beam. */
    public static final float FLOOR_TOP = 1;
    public static final float TOP_BOTTOM = 15;
    /** The glass front pane. */
    public static final float GLASS_Z0 = 0.9F;
    public static final float GLASS_Z1 = 1.1F;
    public static final float GLASS_INSET = 0.05F;
    /** The fluid behind the glass. */
    public static final float FLUID_X_INSET = 0.1F;
    public static final float FLUID_Z0 = 1.2F;
    public static final float FLUID_Z1 = 14.9F;
    public static final float FLUID_Y0 = 1.1F;
    public static final float FLUID_Y1 = 14.9F;
    /**
     * The copper port flange: half its height, half its width and how far
     * into the bay it reaches. It is wide, since a tanker's coupling sits a
     * little toward its front and the tanker may stop facing either way.
     */
    public static final float PORT_HALF = 1.5F;
    public static final float PORT_HALF_WIDTH = 3;
    public static final float PORT_DEPTH = 1.5F;

    private FluidRackLook() {
    }

    /** Height of the port's centre, in block pixels: the height of a tanker's coupling. */
    public static float portY() {
        return TankerLook.couplingYInBlock();
    }
}
