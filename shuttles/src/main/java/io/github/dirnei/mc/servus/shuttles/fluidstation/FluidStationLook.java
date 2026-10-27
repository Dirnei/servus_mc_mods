package io.github.dirnei.mc.servus.shuttles.fluidstation;

import io.github.dirnei.mc.servus.shuttles.shuttle.module.TankerLook;

/**
 * The look of a fluid station in block pixels, shared by its models and its
 * renderer, laid out for a station facing north: the front at z 0. The
 * upper half carries one sight glass per tank in a row of deep window slots
 * between {@link #SLOT_X0} and {@link #SLOT_X1}, separated by steel fins
 * {@link #FIN} wide; each glass sits {@link #GLASS_INSET} off the slot's
 * walls and the fluid {@link #FLUID_INSET} off them, behind the glass. Pure Java.
 */
public final class FluidStationLook {

    /** Width of the corner posts on both halves. */
    public static final float POST = 1;
    /** Depth of the window slots: the upper body starts behind them. */
    public static final float SLOT_DEPTH = 3;
    /** Front of the lower body, behind its posts and plinth. */
    public static final float LOWER_FRONT = 1;
    /** The row of window slots on the upper half. */
    public static final float SLOT_X0 = POST;
    public static final float SLOT_X1 = 16 - POST;
    /** Top of the sill and bottom of the header around the windows. */
    public static final float SILL_TOP = 1;
    public static final float HEADER_BOTTOM = 12;
    /** Bottom of the teal band at the top of the upper half. */
    public static final float BAND_BOTTOM = 13;
    /** Width of the steel fins between and beside the windows. */
    public static final float FIN = 1;
    /** The glass pane of a window. */
    public static final float GLASS_Z0 = 0.45F;
    public static final float GLASS_Z1 = 0.55F;
    public static final float GLASS_INSET = 0.05F;
    /** The fluid behind the glass, off the slot's walls by this much. */
    public static final float FLUID_INSET = 0.1F;
    public static final float FLUID_Z0 = 0.7F;
    public static final float FLUID_Z1 = SLOT_DEPTH - FLUID_INSET;
    public static final float FLUID_Y0 = SILL_TOP + FLUID_INSET;
    public static final float FLUID_Y1 = HEADER_BOTTOM - FLUID_INSET;
    /** The copper port flange on the lower half: half its height and width, and how far into the body it reaches. */
    public static final float PORT_HALF = 1.5F;
    public static final float PORT_HALF_WIDTH = 3;
    public static final float PORT_DEPTH = 2;
    /** The two lamps on the lower front: connection left, activity right, in iron sockets. */
    public static final float LAMP_Y0 = 5;
    public static final float LAMP_Y1 = 7;
    public static final float LAMP_WIDTH = 3;
    public static final float CONNECTION_LAMP_X = 3;
    public static final float ACTIVITY_LAMP_X = 10;
    public static final float SOCKET_RIM = 0.5F;
    public static final float SOCKET_Z = 0.5F;
    public static final float LENS_Z = 0.3F;

    private FluidStationLook() {
    }

    /** Height of the port's centre: the height of a tanker's coupling. */
    public static float portY() {
        return TankerLook.couplingYInBlock();
    }

    /** The block model name of the window row of a station with that many tanks, drawn by the renderer. */
    public static String windowModel(int tanks) {
        return "fluid_station_windows_" + tanks;
    }

    /** Width of one window of a station with that many tanks. */
    public static float windowWidth(int tanks) {
        return (SLOT_X1 - SLOT_X0 - (tanks + 1) * FIN) / tanks;
    }

    /** Left edge of a window of a station with that many tanks. */
    public static float windowX0(int tanks, int window) {
        return SLOT_X0 + FIN + window * (windowWidth(tanks) + FIN);
    }

    /** Right edge of a window of a station with that many tanks. */
    public static float windowX1(int tanks, int window) {
        return windowX0(tanks, window) + windowWidth(tanks);
    }
}
