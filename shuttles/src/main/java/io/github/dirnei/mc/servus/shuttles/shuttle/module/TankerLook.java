package io.github.dirnei.mc.servus.shuttles.shuttle.module;

/**
 * The look of the Tanker tool in shuttle model pixels, shared by its model,
 * the sight glass and coupling the shuttle renderer draws, the gantry spout
 * that lowers onto its cap and the Fluid Rack port its coupling docks into.
 * The shuttle is drawn at {@link #SCALE} around the block centre with model
 * y 0 on the rail heads at block pixel {@link #FLOOR}. Pure Java.
 */
public final class TankerLook {

    /** Block pixel of model y 0: the top of the rail heads. */
    public static final float FLOOR = 2;
    /** The scale the shuttle is drawn at. */
    public static final float SCALE = 0.8F;
    /** Top of the copper filler cap, in model pixels. */
    public static final float CAP_TOP = 17.3F;
    /** The sight glass window on both sides of the tank: its outer face and its extent. */
    public static final float GLASS_LEFT = 4.2F;
    public static final float GLASS_RIGHT = 11.8F;
    public static final float GLASS_Y0 = 12;
    public static final float GLASS_Y1 = 14;
    public static final float GLASS_Z0 = 6.4F;
    public static final float GLASS_Z1 = 9.6F;

    /** Centre of the coupling on both tank sides, between the front band and the sight glass frame. */
    public static final float COUPLING_Y = 13;
    public static final float COUPLING_Z = 5.5F;
    /** The blackened iron collar the coupling is stowed in, on the left side; the right side mirrors it. */
    public static final float COLLAR_X0 = 3.9F;
    public static final float COLLAR_X1 = 4.7F;
    public static final float COLLAR_HALF = 0.7F;
    /** The copper head, 0.05 inside the collar when stowed. */
    public static final float HEAD_X0 = COLLAR_X0 + 0.05F;
    public static final float HEAD_X1 = COLLAR_X1 - 0.05F;
    public static final float HEAD_HALF = COLLAR_HALF - 0.05F;
    /** The steel tube between collar and head; its inner end stays inside the collar. */
    public static final float TUBE_HALF = 0.45F;
    public static final float TUBE_INNER = 4.4F;
    /** Ticks the coupling takes to slide out or back in. */
    public static final int COUPLING_TICKS = 10;
    /** How far short of the block edge the extended head stops, in block pixels. */
    public static final float COUPLING_GAP = 0.005F;

    private TankerLook() {
    }

    /** The cap's top in pixels of the rail's block. */
    public static float capTopInBlock() {
        return FLOOR + SCALE * CAP_TOP;
    }

    /** The coupling's centre height in pixels of the rail's block, where a Fluid Rack's port sits. */
    public static float couplingYInBlock() {
        return FLOOR + SCALE * COUPLING_Y;
    }

    /** How far the head slides out, in model pixels, to end {@link #COUPLING_GAP} short of the block edge. */
    public static float reach() {
        float edge = 8 - (8 + COUPLING_GAP) / SCALE;
        return HEAD_X0 - edge;
    }

    /**
     * How far out the coupling is, from 0 stowed to 1 at the port, a stop of
     * {@code ticks} ticks after {@code elapsed} of them: it eases out over
     * {@link #COUPLING_TICKS}, holds, and eases back in over the last ones;
     * a short stop splits its time between the two.
     */
    public static float extension(float ticks, float elapsed) {
        if (ticks <= 0) {
            return 0;
        }
        float slide = Math.min(COUPLING_TICKS, ticks / 2);
        float out = Math.min(elapsed, ticks - elapsed) / slide;
        float clamped = Math.max(0, Math.min(1, out));
        return clamped * clamped * (3 - 2 * clamped);
    }
}
