package io.github.dirnei.mc.servus.shuttles.shuttle;

/**
 * A countdown that only runs while a condition holds and starts over when it
 * does not, so something waits until its surroundings have been ready for a
 * while. Pure Java.
 */
public final class Settle {

    private final int ticks;
    private int left;

    /** @param ticks how many ready ticks in a row it takes */
    public Settle(int ticks) {
        this.ticks = Math.max(1, ticks);
        this.left = this.ticks;
    }

    /**
     * One tick.
     *
     * @param ready whether the condition holds this tick
     * @return true once the condition has held for the whole countdown
     */
    public boolean tick(boolean ready) {
        if (!ready) {
            left = ticks;
            return false;
        }
        if (left > 0) {
            left--;
        }
        return left == 0;
    }
}
