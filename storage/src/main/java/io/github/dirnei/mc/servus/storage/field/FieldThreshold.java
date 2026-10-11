package io.github.dirnei.mc.servus.storage.field;

/**
 * The ripe share a Field Head waits for before it calls a harvest, in whole
 * percent and steps of ten. Pure, no Minecraft imports.
 */
public final class FieldThreshold {

    public static final int STEP = 10;
    public static final int MIN = 10;
    public static final int MAX = 100;

    private FieldThreshold() {
    }

    /** The percent for a share from {@code field.json}, rounded to a step and kept in range. */
    public static int fromShare(double share) {
        return clamp((int) Math.round(share * 100 / STEP) * STEP);
    }

    /** One step up (positive steps) or down (negative), kept in range. */
    public static int step(int percent, int steps) {
        return clamp(percent + steps * STEP);
    }

    public static int clamp(int percent) {
        return Math.max(MIN, Math.min(MAX, percent));
    }
}
