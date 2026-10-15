package io.github.dirnei.mc.servus.alchemy.homunculus;

import java.util.function.BooleanSupplier;

/**
 * The bottle of Elixir a homunculus lives on: one bottle feeds it for a number of ticks, then the next bottle is
 * taken. Pure Java; the caller says how a bottle is taken.
 */
public final class ElixirUpkeep {

    private int ticksLeft;

    /** Ticks the bottle in use still feeds, 0 when none is in use. */
    public int ticksLeft() {
        return ticksLeft;
    }

    /**
     * Feeds one tick: counts down the bottle in use, or takes a new one through {@code takeBottle} when none is left.
     * Returns whether the homunculus is fed this tick.
     */
    public boolean feed(int elixirTicks, BooleanSupplier takeBottle) {
        if (ticksLeft <= 0) {
            if (!takeBottle.getAsBoolean()) {
                return false;
            }
            ticksLeft = Math.max(1, elixirTicks);
        }
        ticksLeft--;
        return true;
    }

    /** Restores the saved bottle in use. */
    public void load(int ticksLeft) {
        this.ticksLeft = Math.max(0, ticksLeft);
    }
}
