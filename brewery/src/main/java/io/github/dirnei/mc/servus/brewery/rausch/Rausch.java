package io.github.dirnei.mc.servus.brewery.rausch;

import java.util.Optional;

/**
 * One player's Rausch. Pure Java and immutable: every change returns a new value.
 *
 * @param points        the hidden counter
 * @param progress      ticks counted toward the next point wearing off
 * @param dueDay        the day on which the player reached the hangover threshold, or -1 when no Hangover is due
 * @param hangoverUntil game time at which the Hangover ends; at or before now there is none
 */
public record Rausch(int points, int progress, long dueDay, long hangoverUntil) {

    public static final Rausch SOBER = new Rausch(0, 0, -1, 0);

    /**
     * A beer of {@code amount} points. While hungover it is the Hair of the Dog: the Hangover ends and the points are
     * set to the Hair of the Dog value.
     */
    public Rausch drink(int amount, RauschStats stats, long day, long gameTime) {
        if (hungover(gameTime)) {
            return new Rausch(stats.hangover().hairOfTheDog(), 0, -1, 0);
        }
        int raised = Math.min(stats.max(), points + Math.max(0, amount));
        long due = dueDay < 0 && raised >= stats.hangover().from() ? day : dueDay;
        return new Rausch(raised, progress, due, hangoverUntil);
    }

    /** {@code ticks} spent in the game: one point wears off per decay period. */
    public Rausch pass(int ticks, RauschStats stats) {
        if (points == 0) {
            return progress == 0 ? this : new Rausch(0, 0, dueDay, hangoverUntil);
        }
        long counted = (long) progress + Math.max(0, ticks);
        long worn = counted / stats.decayTicks();
        int left = (int) Math.max(0, points - worn);
        int rest = left == 0 ? 0 : (int) (counted % stats.decayTicks());
        return new Rausch(left, rest, dueDay, hangoverUntil);
    }

    /** The day is {@code day} now: a Hangover due from an earlier day starts and clears the points. */
    public Rausch morning(long day, long gameTime, RauschStats stats) {
        if (dueDay < 0 || day <= dueDay) {
            return this;
        }
        return new Rausch(0, 0, -1, gameTime + stats.hangover().duration());
    }

    /** The current level, or empty when sober. */
    public Optional<RauschLevel> level(RauschStats stats) {
        return stats.levelAt(points);
    }

    /** Ticks until the points fall below the current level's threshold without another drink; 0 when sober. */
    public int ticksAtLevel(RauschStats stats) {
        return level(stats).map(level -> {
            int from = Math.max(1, stats.level(level).from());
            return (points - from) * stats.decayTicks() + stats.decayTicks() - progress;
        }).orElse(0);
    }

    public boolean hungover(long gameTime) {
        return hangoverUntil > gameTime;
    }

    /** Ticks of Hangover left at {@code gameTime}, 0 when there is none. */
    public int hangoverLeft(long gameTime) {
        return (int) Math.max(0, hangoverUntil - gameTime);
    }
}
