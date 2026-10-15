package io.github.dirnei.mc.servus.alchemy.hollow;

/**
 * How much faster a gnome works after a gift, and for how long. Pure Java: {@link #work()} is called once per tick
 * and says how many ticks of digging the gnome does in it.
 */
public final class GiftBoost {

    private double factor = 1;
    private int ticksLeft;
    private double carry;

    /** Starts a boost, replacing any running one: boosts refresh, they never stack. */
    public void give(double factor, int ticks) {
        this.factor = Math.max(1, factor);
        this.ticksLeft = Math.max(0, ticks);
    }

    public boolean active() {
        return ticksLeft > 0;
    }

    /** The running factor, 1 without a boost. */
    public double factor() {
        return active() ? factor : 1;
    }

    public int ticksLeft() {
        return ticksLeft;
    }

    /** Counts one tick down and returns the whole ticks of digging done in it; fractions carry to later ticks. */
    public int work() {
        carry += factor();
        if (ticksLeft > 0) {
            ticksLeft--;
        }
        int whole = (int) carry;
        carry -= whole;
        return whole;
    }

    /** Restores a saved boost. */
    public void load(double factor, int ticks) {
        give(factor, ticks);
        carry = 0;
    }
}
