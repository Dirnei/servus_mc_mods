package io.github.dirnei.mc.servus.storage.field;

/**
 * The harvest limit and fertilize level of a Field Head, counted in items of
 * its produce in stock. A value of 0 turns a level off, and while the produce
 * is unknown both do nothing. Pure Java.
 */
public final class StockLevels {

    /** The largest value a level takes. */
    public static final int MAX = 99_999;

    /** Which of the two levels a player sets. */
    public enum Level {
        HARVEST, FERTILIZE
    }

    /** A harvest limit and a fertilize level, each 0 for none. */
    public record Levels(int harvestLimit, int fertilizeBelow) {
    }

    private StockLevels() {
    }

    /** Whether the field stops harvesting: a produce is known, a limit is set and the stock reached it. */
    public static boolean harvestPaused(boolean produceKnown, long stock, int limit) {
        return produceKnown && limit > 0 && stock >= limit;
    }

    /**
     * Whether the field may call a Spreader: always while the produce is
     * unknown, else with no level or the stock below it, and never while
     * harvesting is paused.
     */
    public static boolean fertilizeAllowed(boolean produceKnown, long stock, int level, int limit) {
        if (!produceKnown) {
            return true;
        }
        return (level <= 0 || stock < level) && !harvestPaused(true, stock, limit);
    }

    /** The value kept between 0 and {@link #MAX}. */
    public static int clamp(long value) {
        return (int) Math.max(0, Math.min(MAX, value));
    }

    /**
     * Keeps the fertilize level at or below the harvest limit while both are
     * set: the level that was not moved follows the one that was.
     */
    public static Levels keepOrder(int limit, int level, Level moved) {
        if (limit <= 0 || level <= 0 || level <= limit) {
            return new Levels(limit, level);
        }
        return moved == Level.FERTILIZE ? new Levels(level, level) : new Levels(limit, limit);
    }
}
