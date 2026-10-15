package io.github.dirnei.mc.servus.alchemy.calciner;

/** How long a calcination takes over the eternal flame. */
public final class EternalFireSpeed {

    private EternalFireSpeed() {
    }

    /** The recipe time divided by {@code speed}, rounded down; a speed below 1 counts as 1 and the result is at least 1 tick. */
    public static int cycleTicks(int recipeTicks, int speed) {
        return Math.max(1, recipeTicks / Math.max(1, speed));
    }
}
