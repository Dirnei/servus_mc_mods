package io.github.dirnei.mc.servus.foundation.machine.sink;

import java.util.List;

/**
 * The void rates of a Sink per chimney height. Entry {@code h} of each table is
 * the rate with {@code h} Chimney blocks; the first height without an entry in
 * every table is unlimited. Pure Java.
 *
 * @param itemsPerSecond items per second by height
 * @param fluidPerTick   mB per tick by height
 * @param energyPerTick  FE per tick by height
 */
public record SinkRates(List<Integer> itemsPerSecond, List<Integer> fluidPerTick, List<Integer> energyPerTick) {

    /** A per tick rate without limit. */
    public static final double UNLIMITED = Double.POSITIVE_INFINITY;
    /** The menu value of an unlimited rate. */
    public static final int UNLIMITED_DISPLAY = -1;

    public SinkRates {
        itemsPerSecond = List.copyOf(itemsPerSecond);
        fluidPerTick = List.copyOf(fluidPerTick);
        energyPerTick = List.copyOf(energyPerTick);
    }

    /** The first chimney height without a limit: the length of the shortest table. */
    public int unlimitedHeight() {
        return Math.min(itemsPerSecond.size(), Math.min(fluidPerTick.size(), energyPerTick.size()));
    }

    public boolean isUnlimited(int height) {
        return height >= unlimitedHeight();
    }

    /** Items per tick at a height, or {@link #UNLIMITED}. */
    public double items(int height) {
        return isUnlimited(height) ? UNLIMITED : entry(itemsPerSecond, height) / 20.0;
    }

    /** mB per tick at a height, or {@link #UNLIMITED}. */
    public double fluid(int height) {
        return isUnlimited(height) ? UNLIMITED : entry(fluidPerTick, height);
    }

    /** FE per tick at a height, or {@link #UNLIMITED}. */
    public double energy(int height) {
        return isUnlimited(height) ? UNLIMITED : entry(energyPerTick, height);
    }

    /** Items per second for display, or {@link #UNLIMITED_DISPLAY}. */
    public int itemsDisplay(int height) {
        return isUnlimited(height) ? UNLIMITED_DISPLAY : entry(itemsPerSecond, height);
    }

    /** mB per tick for display, or {@link #UNLIMITED_DISPLAY}. */
    public int fluidDisplay(int height) {
        return isUnlimited(height) ? UNLIMITED_DISPLAY : entry(fluidPerTick, height);
    }

    /** FE per tick for display, or {@link #UNLIMITED_DISPLAY}. */
    public int energyDisplay(int height) {
        return isUnlimited(height) ? UNLIMITED_DISPLAY : entry(energyPerTick, height);
    }

    private static int entry(List<Integer> table, int height) {
        return Math.max(0, table.get(Math.max(0, height)));
    }
}
