package io.github.dirnei.mc.servus.foundation.data.packet;

/**
 * A rate in items per second turned into a whole number of items per tick.
 * Fractions carry over between ticks, so 8 per second allows 8 items over 20
 * ticks, not 0 per tick. The budget never grows beyond one second's worth
 * (at least one item), so a member that was idle cannot burst.
 */
public final class RateBudget {

    public static final int TICKS_PER_SECOND = 20;

    private double perSecond;
    private double available;

    public RateBudget(double perSecond) {
        setRate(perSecond);
    }

    /** Items per second; zero or less stops the budget. */
    public double rate() {
        return perSecond;
    }

    /** Changes the rate, for example after a datapack reload; what is saved up is capped again. */
    public void setRate(double perSecond) {
        this.perSecond = Math.max(0, perSecond);
        available = Math.min(available, cap());
    }

    /** Adds one tick's worth of items. */
    public void tick() {
        available = Math.min(cap(), available + perSecond / TICKS_PER_SECOND);
    }

    /** Saves up one second's worth at once, as a budget that was idle for a second would have. */
    public void fill() {
        available = cap();
    }

    /** How many whole items may pass right now. */
    public int allowance() {
        return (int) Math.floor(available + 1e-9);
    }

    /** Uses up {@code items} of the allowance. */
    public void consume(int items) {
        if (items > 0) {
            available = Math.max(0, available - items);
        }
    }

    private double cap() {
        return perSecond <= 0 ? 0 : Math.max(1, perSecond);
    }
}
