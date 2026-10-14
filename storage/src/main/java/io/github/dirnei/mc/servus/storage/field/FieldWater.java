package io.github.dirnei.mc.servus.storage.field;

/** Water a field uses, and how long its copper takes to age from it. Pure Java. */
public final class FieldWater {

    private static final int TICKS_PER_MINUTE = 1200;

    private FieldWater() {
    }

    /** The mB one interval uses for a field of that many rails; never below 1 for a field. */
    public static int use(int rails, int perRail) {
        if (rails <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (long) rails * perRail));
    }

    /** The mB per minute a watered field of that many rails uses, one {@link #use} every {@code interval} ticks. */
    public static int perMinute(int rails, int perRail, int interval) {
        return (int) Math.min(Integer.MAX_VALUE, (long) use(rails, perRail) * TICKS_PER_MINUTE / Math.max(1, interval));
    }

    /**
     * The ticks a watered field needs to use {@code water} more mB, in whole
     * intervals; -1 for a field that uses none.
     */
    public static long ticksToUse(long water, int rails, int perRail, int interval) {
        int use = use(rails, perRail);
        if (use <= 0) {
            return -1;
        }
        long intervals = (Math.max(0, water) + use - 1) / use;
        return intervals * Math.max(1, interval);
    }
}
