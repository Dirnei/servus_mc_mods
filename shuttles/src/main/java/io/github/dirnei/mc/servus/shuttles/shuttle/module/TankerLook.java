package io.github.dirnei.mc.servus.shuttles.shuttle.module;

/**
 * The look of the Tanker tool in shuttle model pixels, shared by its model,
 * the sight glass the shuttle renderer draws and the gantry spout that lowers
 * onto its cap. The shuttle is drawn at {@link #SCALE} around the block
 * centre with model y 0 on the rail heads at block pixel {@link #FLOOR}. Pure Java.
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

    private TankerLook() {
    }

    /** The cap's top in pixels of the rail's block. */
    public static float capTopInBlock() {
        return FLOOR + SCALE * CAP_TOP;
    }
}
