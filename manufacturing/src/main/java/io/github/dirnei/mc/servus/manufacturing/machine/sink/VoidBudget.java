package io.github.dirnei.mc.servus.manufacturing.machine.sink;

/**
 * A token bucket for one resource of a Sink. Tokens refill lazily from the
 * game time elapsed since the last call, so nothing has to tick. Unused tokens
 * carry over for at most {@link #CARRY_TICKS} ticks of rate. Pure Java.
 */
public final class VoidBudget {

    /** How many ticks of rate the bucket holds: one second. */
    public static final int CARRY_TICKS = 20;
    private static final double EPSILON = 1e-9;

    private double tokens;
    private long lastTime;
    private boolean started;

    /** How much of {@code requested} the budget would grant now, without spending it. */
    public long peek(long requested, long gameTime, double ratePerTick) {
        if (requested <= 0) {
            return 0;
        }
        if (Double.isInfinite(ratePerTick)) {
            return requested;
        }
        refill(gameTime, ratePerTick);
        return Math.min(requested, (long) Math.floor(tokens + EPSILON));
    }

    /** Grants as much of {@code requested} as the budget allows and spends it. */
    public long take(long requested, long gameTime, double ratePerTick) {
        long granted = peek(requested, gameTime, ratePerTick);
        if (granted > 0 && !Double.isInfinite(ratePerTick)) {
            tokens = Math.max(0, tokens - granted);
        }
        return granted;
    }

    private void refill(long gameTime, double ratePerTick) {
        double rate = Math.max(0, ratePerTick);
        double capacity = rate * CARRY_TICKS;
        if (!started) {
            // An idle bucket is full: one second of rate at once.
            started = true;
            tokens = capacity;
        } else {
            long elapsed = Math.max(0, gameTime - lastTime);
            tokens = tokens + elapsed * rate;
        }
        tokens = Math.min(tokens, capacity);
        // Game time going backwards (a world reload edge) counts as zero elapsed from the new time on.
        lastTime = gameTime;
    }
}
