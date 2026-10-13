package io.github.dirnei.mc.servus.storage.power;

import java.util.Arrays;

/**
 * The accounting of one site's FE buffer: draws are paid in full or not at
 * all, a refused draw holds the no power state for a second, and the FE used
 * per tick is kept for the last second. The buffer itself lives elsewhere;
 * every method gets the FE stored and the game tick.
 */
public final class PowerBudget {

    /** Ticks a refusal keeps the no power state and ticks the use readout covers. */
    public static final int WINDOW = 20;

    public enum State { POWERED, NO_POWER }

    private final long[] used = new long[WINDOW];
    private final long[] usedTick = new long[WINDOW];
    private long lastRefusal = Long.MIN_VALUE;

    public PowerBudget() {
        Arrays.fill(usedTick, Long.MIN_VALUE);
    }

    /** Whether a draw of that amount is paid from what is stored: all or nothing, nothing for a free draw. */
    public static boolean paid(long stored, long amount) {
        return amount <= 0 || Math.max(0, stored) >= amount;
    }

    /**
     * Decides a draw and records it: a paid draw counts as used this tick, a
     * refused one as the last refusal. The caller takes the FE when paid.
     *
     * @return whether the draw is paid
     */
    public boolean draw(long tick, long stored, long amount) {
        if (amount <= 0) {
            return true;
        }
        boolean paid = paid(stored, amount);
        if (paid) {
            use(tick, amount);
        } else {
            observe(tick, false);
        }
        return paid;
    }

    /** Remembers a refused draw at that tick; a paid one changes nothing here. */
    public void observe(long tick, boolean paid) {
        if (!paid) {
            lastRefusal = Math.max(lastRefusal, tick);
        }
    }

    /** Counts FE spent at that tick into the readout. */
    public void use(long tick, long amount) {
        if (amount <= 0) {
            return;
        }
        int slot = (int) Math.floorMod(tick, (long) WINDOW);
        if (usedTick[slot] != tick) {
            usedTick[slot] = tick;
            used[slot] = 0;
        }
        used[slot] += amount;
    }

    /** No power while the buffer is empty or a draw was refused within the last second. */
    public State state(long tick, long stored) {
        if (stored <= 0) {
            return State.NO_POWER;
        }
        return refusedRecently(tick) ? State.NO_POWER : State.POWERED;
    }

    /** Whether a draw was refused within the last {@link #WINDOW} ticks. */
    private boolean refusedRecently(long tick) {
        return lastRefusal != Long.MIN_VALUE && tick - lastRefusal < WINDOW;
    }

    /** FE used during the last {@link #WINDOW} ticks, this one included. */
    public long usedLastSecond(long tick) {
        long sum = 0;
        for (int i = 0; i < WINDOW; i++) {
            long at = usedTick[i];
            if (at != Long.MIN_VALUE && at <= tick && tick - at < WINDOW) {
                sum += used[i];
            }
        }
        return sum;
    }
}
