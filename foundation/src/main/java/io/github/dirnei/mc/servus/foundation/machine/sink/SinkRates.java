package io.github.dirnei.mc.servus.foundation.machine.sink;

import java.util.List;

/**
 * The void rates of a Sink per chimney height. Entry {@code h} of each table is
 * the rate with {@code h} Chimney blocks; the first height without an entry in
 * every table is unlimited. Items and fluid are voided in batches every
 * {@link #BATCH_TICKS} ticks, energy at once. Pure Java.
 *
 * @param itemsPerSecond items per second by height
 * @param fluidPerSecond mB per second by height
 * @param energyPerTick  FE per tick by height
 */
public record SinkRates(List<Integer> itemsPerSecond, List<Integer> fluidPerSecond, List<Integer> energyPerTick) {

    /** A rate without limit. */
    public static final double UNLIMITED = Double.POSITIVE_INFINITY;
    /** The menu value of an unlimited rate. */
    public static final int UNLIMITED_DISPLAY = -1;
    /** Ticks between two batches: one second. */
    public static final int BATCH_TICKS = 20;

    public SinkRates {
        itemsPerSecond = List.copyOf(itemsPerSecond);
        fluidPerSecond = List.copyOf(fluidPerSecond);
        energyPerTick = List.copyOf(energyPerTick);
    }

    /** The first chimney height without a limit: the length of the shortest table. */
    public int unlimitedHeight() {
        return Math.min(itemsPerSecond.size(), Math.min(fluidPerSecond.size(), energyPerTick.size()));
    }

    public boolean isUnlimited(int height) {
        return height >= unlimitedHeight();
    }

    /** Items one batch voids at a height, or {@link #UNLIMITED}. */
    public double itemsPerBatch(int height) {
        return isUnlimited(height) ? UNLIMITED : entry(itemsPerSecond, height) * (BATCH_TICKS / 20.0);
    }

    /** mB one batch voids at a height, or {@link #UNLIMITED}. */
    public double fluidPerBatch(int height) {
        return isUnlimited(height) ? UNLIMITED : entry(fluidPerSecond, height) * (BATCH_TICKS / 20.0);
    }

    /** FE per tick at a height, or {@link #UNLIMITED}. */
    public double energy(int height) {
        return isUnlimited(height) ? UNLIMITED : entry(energyPerTick, height);
    }

    /** Items per second for display, or {@link #UNLIMITED_DISPLAY}. */
    public int itemsDisplay(int height) {
        return isUnlimited(height) ? UNLIMITED_DISPLAY : entry(itemsPerSecond, height);
    }

    /** mB per second for display, or {@link #UNLIMITED_DISPLAY}. */
    public int fluidDisplay(int height) {
        return isUnlimited(height) ? UNLIMITED_DISPLAY : entry(fluidPerSecond, height);
    }

    /** FE per tick for display, or {@link #UNLIMITED_DISPLAY}. */
    public int energyDisplay(int height) {
        return isUnlimited(height) ? UNLIMITED_DISPLAY : entry(energyPerTick, height);
    }

    private static int entry(List<Integer> table, int height) {
        return Math.max(0, table.get(Math.max(0, height)));
    }
}
