package io.github.dirnei.mc.servus.delicacies.firebox;

/** Where the thermometer's needle points. Pure Java. */
public final class GaugeNeedle {

    /** Degrees the needle sweeps from cold to full heat, half of it to either side of straight up. */
    public static final float SWEEP = 240;

    private GaugeNeedle() {
    }

    /** The heat to show {@code ticksSince} ticks after a reading of {@code heat} that changed by {@code rate} per tick. */
    public static float shown(float heat, float rate, float ticksSince) {
        return Math.max(0, Math.min(1, heat + rate * Math.max(0, ticksSince)));
    }

    /** The needle's turn from straight up in degrees, negative to the cold side, for a shown heat. */
    public static float angle(float shown) {
        return (Math.max(0, Math.min(1, shown)) - 0.5F) * SWEEP;
    }
}
