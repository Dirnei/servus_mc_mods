package io.github.dirnei.mc.servus.shuttles.packing;

/**
 * Whether a pick may bring items of one item into a Packing Station's To
 * network buffer: only as far as targets granted room for them, plus what
 * shipments to targets without an endpoint still need. A pick never waits
 * forever for room it can never get: with nothing of the item on its way or
 * waiting, any room at all lets one pick through. Pure, no Minecraft imports.
 */
public final class PickAdmission {

    private PickAdmission() {
    }

    /**
     * @param granted  room targets granted for the item and nothing took up yet
     * @param open     what shipments of the item to targets without an endpoint still need
     * @param admitted items of the item already in the buffer or on their way to it
     * @param amount   the pick's amount
     */
    public static boolean admits(int granted, int open, int admitted, int amount) {
        int allowance = granted + open - admitted;
        return allowance >= amount || allowance > 0 && admitted == 0;
    }
}
