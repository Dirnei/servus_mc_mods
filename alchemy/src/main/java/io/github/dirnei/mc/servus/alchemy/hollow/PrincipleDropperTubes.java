package io.github.dirnei.mc.servus.alchemy.hollow;

/** Where the Principle Dropper's three glass tubes stand, in pixels; shared by its model, shape and renderer. */
public final class PrincipleDropperTubes {

    /** Tube centres (x, z) by principle ordinal: Salt at the back, Sulfur and Mercury at the front corners. */
    public static final float[][] CENTRES = {{8, 12.2F}, {4.36F, 5.9F}, {11.64F, 5.9F}};
    /** Half the width of a tube's glass. */
    public static final float RADIUS = 1.75F;
    public static final float GLASS_BOTTOM = 5.75F;
    public static final float GLASS_TOP = 13;

    private PrincipleDropperTubes() {
    }
}
