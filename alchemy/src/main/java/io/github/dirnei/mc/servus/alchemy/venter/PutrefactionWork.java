package io.github.dirnei.mc.servus.alchemy.venter;

/**
 * The speed of putrefaction, linear in the straw left: a batch takes {@code time} ticks in a full bed
 * and {@code coldTime} ticks in an empty one. Progress is counted in points so the speed can change
 * every tick without rounding drift. Pure Java.
 */
public final class PutrefactionWork {

    private static final long MILLI = 1000;

    private PutrefactionWork() {
    }

    /** The points one batch needs. */
    public static long total(int time, int coldTime) {
        int warm = warm(time);
        return (long) warm * cold(warm, coldTime) * MILLI;
    }

    /** The points one working tick adds with {@code levelMilli} thousandths of the straw left; never zero. */
    public static long perTick(int time, int coldTime, int levelMilli) {
        int warm = warm(time);
        int level = Math.clamp(levelMilli, 0, (int) MILLI);
        return warm * MILLI + (long) (cold(warm, coldTime) - warm) * level;
    }

    /** The ticks a whole batch takes at a steady straw level, rounded up. */
    public static int ticksAt(int time, int coldTime, int levelMilli) {
        long perTick = perTick(time, coldTime, levelMilli);
        return (int) ((total(time, coldTime) + perTick - 1) / perTick);
    }

    private static int warm(int time) {
        return Math.max(1, time);
    }

    private static int cold(int warm, int coldTime) {
        return Math.max(warm, coldTime);
    }
}
