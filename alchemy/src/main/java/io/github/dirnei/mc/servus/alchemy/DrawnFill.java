package io.github.dirnei.mc.servus.alchemy;

/** The fill of each principle tank, or of one amount, in sixteenths, -1 for empty, as last sent to the clients that draw it. */
final class DrawnFill {

    private final int[] sixteenths = {-1, -1, -1};

    /** Takes the tanks' current fill; true when a level differs from the one taken before. */
    boolean update(PrincipleTanks tanks) {
        boolean changed = false;
        for (Principle principle : Principle.values()) {
            changed |= take(principle.ordinal(), tanks.amount(principle), tanks.capacity());
        }
        return changed;
    }

    /** Takes one amount out of {@code capacity}; true when its level differs from the one taken before. */
    boolean update(int amount, int capacity) {
        return take(0, amount, capacity);
    }

    private boolean take(int index, int amount, int capacity) {
        int fill = amount == 0 ? -1 : amount * 16 / Math.max(1, capacity);
        if (fill == sixteenths[index]) {
            return false;
        }
        sixteenths[index] = fill;
        return true;
    }
}
