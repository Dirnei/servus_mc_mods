package io.github.dirnei.mc.servus.shuttles.field;

/** How far a felled tree has tipped over: slowly at first and fastest just before it lands, like falling. Pure Java. */
public final class ToppleCurve {

    /** The angle of a tree lying flat. */
    public static final double FLAT = 90;

    private ToppleCurve() {
    }

    /** The tip angle in degrees at {@code t}, the share of the fall time gone, clamped to 0 to 1. */
    public static double angle(double t) {
        double share = Math.clamp(t, 0, 1);
        return FLAT * share * share;
    }
}
