package io.github.dirnei.mc.servus.manufacturing.alchemy;

/** The fill of each principle tank in sixteenths, -1 for empty, as last sent to the clients that draw it. */
final class DrawnFill {

    private final int[] sixteenths = {-1, -1, -1};

    /** Takes the tanks' current fill; true when a level differs from the one taken before. */
    boolean update(PrincipleTanks tanks) {
        boolean changed = false;
        for (Principle principle : Principle.values()) {
            int amount = tanks.amount(principle);
            int fill = amount == 0 ? -1 : amount * 16 / Math.max(1, tanks.capacity());
            if (fill != sixteenths[principle.ordinal()]) {
                sixteenths[principle.ordinal()] = fill;
                changed = true;
            }
        }
        return changed;
    }
}
