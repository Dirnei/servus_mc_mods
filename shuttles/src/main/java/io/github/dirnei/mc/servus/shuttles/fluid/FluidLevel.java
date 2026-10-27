package io.github.dirnei.mc.servus.shuttles.fluid;

/** The fill step in which clients learn how full a tank is. Pure Java. */
public final class FluidLevel {

    private FluidLevel() {
    }

    /**
     * The step of a tank: 0 when empty, {@code steps} only when full, any
     * fluid in between at least 1 and below {@code steps}.
     */
    public static int step(int amount, int capacity, int steps) {
        if (amount <= 0 || capacity <= 0 || steps <= 0) {
            return 0;
        }
        if (amount >= capacity) {
            return steps;
        }
        int level = (int) ((long) amount * steps / capacity);
        return Math.max(1, Math.min(steps - 1, level));
    }
}
