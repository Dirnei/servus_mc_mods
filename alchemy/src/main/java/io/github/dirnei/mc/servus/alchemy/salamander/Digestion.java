package io.github.dirnei.mc.servus.alchemy.salamander;

/** A salamander's stomach: one ingot at a time, counted down tick by tick. */
public final class Digestion {

    private int remaining;

    /** Whether an ingot is still being digested. */
    public boolean busy() {
        return remaining > 0;
    }

    /** Ticks left until the ingot is digested, 0 when idle. */
    public int remaining() {
        return remaining;
    }

    /** Starts digesting an ingot for {@code ticks}; the caller checks {@link #busy()} first. */
    public void start(int ticks) {
        if (ticks < 1) {
            throw new IllegalArgumentException("ticks must be at least 1, was " + ticks);
        }
        remaining = ticks;
    }

    /** Restores the ticks left from a save; negative values count as idle. */
    public void restore(int ticks) {
        remaining = Math.max(0, ticks);
    }

    /** Counts one tick down; true exactly on the tick the ingot is digested. */
    public boolean tick() {
        if (remaining <= 0) {
            return false;
        }
        remaining--;
        return remaining == 0;
    }
}
