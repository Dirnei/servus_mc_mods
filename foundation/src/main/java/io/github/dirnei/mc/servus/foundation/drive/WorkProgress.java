package io.github.dirnei.mc.servus.foundation.drive;

/**
 * Work collected toward a goal, such as one milling. Reaching the goal starts
 * over at zero and drops the rest, so a strong driver never banks work for the
 * next item. Pure Java.
 */
public final class WorkProgress {

    private int value;

    /** Adds {@code work} toward {@code goal}; true when the goal was reached, after which progress is zero. */
    public boolean add(int work, int goal) {
        value += Math.max(0, work);
        if (value < Math.max(1, goal)) {
            return false;
        }
        value = 0;
        return true;
    }

    public int value() {
        return value;
    }

    /** Restores saved progress. */
    public void set(int value) {
        this.value = Math.max(0, value);
    }

    public void reset() {
        value = 0;
    }
}
