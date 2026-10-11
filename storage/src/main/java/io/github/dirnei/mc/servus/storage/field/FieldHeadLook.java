package io.github.dirnei.mc.servus.storage.field;

/**
 * What the Field Head shows on its block: the step of the ripe share bar on
 * its front and the level of the water in its windows. Pure, no Minecraft imports.
 */
public final class FieldHeadLook {

    /** The bar is empty. */
    public static final int EMPTY = 0;
    /** The last gold step; gold never fills the bar. */
    public static final int GOLD_STEPS = 4;
    /** Full and green: a harvest is due. */
    public static final int READY = GOLD_STEPS + 1;
    /** Window levels; the top one means a full buffer. */
    public static final int WATER_LEVELS = 8;

    private FieldHeadLook() {
    }

    /**
     * The bar step for the field's counts.
     *
     * @param planted plots holding a crop
     * @param ripe ripe plots among them
     * @param thresholdPercent the ripe percent that calls a harvest
     * @param due whether a harvest is due now
     * @return {@link #EMPTY}, a gold step from 1 to {@link #GOLD_STEPS}, or {@link #READY}
     */
    public static int ripeStep(int planted, int ripe, int thresholdPercent, boolean due) {
        if (due) {
            return READY;
        }
        if (planted <= 0 || ripe <= 0) {
            return EMPTY;
        }
        double threshold = Math.max(1, thresholdPercent) / 100.0;
        double progress = (double) ripe / planted / threshold;
        int step = 1 + (int) Math.floor(progress * GOLD_STEPS);
        return Math.max(1, Math.min(GOLD_STEPS, step));
    }

    /** Gold columns of the 8 wide bar for a gold step: 1, 3, 5, 7. */
    public static int goldColumns(int step) {
        return step <= EMPTY || step >= READY ? 0 : 2 * step - 1;
    }

    /**
     * The water level in the windows: 0 when empty, {@link #WATER_LEVELS} only
     * when full, any water in between at least 1.
     */
    public static int waterLevel(int amount, int capacity) {
        if (amount <= 0 || capacity <= 0) {
            return 0;
        }
        if (amount >= capacity) {
            return WATER_LEVELS;
        }
        int level = (int) ((long) amount * WATER_LEVELS / capacity);
        return Math.max(1, Math.min(WATER_LEVELS - 1, level));
    }
}
