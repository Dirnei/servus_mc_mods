package io.github.dirnei.mc.servus.manufacturing.alchemy.hollow;

import io.github.dirnei.mc.servus.manufacturing.alchemy.Principles;

/** How a vein in a Gnome Hollow ripens and yields. Pure Java: one call per tick. */
public final class VeinGrowth {

    /**
     * A vein's numbers.
     *
     * @param steps      ripening steps until it is ripe
     * @param stepTicks  ticks per step
     * @param stepCost   principles each step takes
     * @param yieldTicks ticks per yielded item once a gnome works it
     */
    public record Vein(int steps, int stepTicks, Principles stepCost, int yieldTicks) {

        public Vein {
            if (steps < 1 || stepTicks < 1 || yieldTicks < 1) {
                throw new IllegalArgumentException("Vein numbers must be at least 1");
            }
        }
    }

    /** A vein's progress. */
    public static final class State {

        public int ripeness;
        public boolean bound;
        public int stepTimer;
        public int yieldTimer;

        public boolean ripe(Vein vein) {
            return ripeness >= vein.steps();
        }
    }

    /** What one tick did. */
    public enum Outcome {
        NONE,
        /** The vein grew a step; take {@link Vein#stepCost()}. */
        RIPENED,
        /** The gnome dug one item. */
        YIELD
    }

    private VeinGrowth() {
    }

    /**
     * Advances the vein by one tick.
     *
     * @param held          what the Hollow's tanks hold now
     * @param outputHasRoom whether a yielded item would fit
     */
    public static Outcome tick(Vein vein, State state, Principles held, boolean outputHasRoom) {
        if (!state.ripe(vein)) {
            if (!held.covers(vein.stepCost())) {
                return Outcome.NONE;
            }
            if (++state.stepTimer < vein.stepTicks()) {
                return Outcome.NONE;
            }
            state.stepTimer = 0;
            state.ripeness++;
            return Outcome.RIPENED;
        }
        if (!state.bound || !outputHasRoom) {
            return Outcome.NONE;
        }
        if (++state.yieldTimer < vein.yieldTicks()) {
            return Outcome.NONE;
        }
        state.yieldTimer = 0;
        return Outcome.YIELD;
    }
}
