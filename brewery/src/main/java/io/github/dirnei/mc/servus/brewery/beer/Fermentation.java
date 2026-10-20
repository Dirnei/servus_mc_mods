package io.github.dirnei.mc.servus.brewery.beer;

/**
 * The timing of one batch in a Fermenting Vat, in game time ticks. Pure Java.
 *
 * @param startedAt game time the batch started
 */
public record Fermentation(long startedAt) {

    /** A batch that starts now. */
    public static Fermentation start(long now) {
        return new Fermentation(now);
    }

    /** Ticks fermented so far; never below 0, even when the game time lies before the start. */
    public long elapsed(long now) {
        return Math.max(0, now - startedAt);
    }

    public boolean isDone(long now, long time) {
        return elapsed(now) >= time;
    }

    /** Ticks until the batch is done, never below 0. */
    public long remaining(long now, long time) {
        return Math.max(0, time - elapsed(now));
    }

    /** How far the batch is, from 0 to 100. */
    public int percent(long now, long time) {
        if (time <= 0) {
            return 100;
        }
        return (int) Math.min(100, elapsed(now) * 100 / time);
    }
}
