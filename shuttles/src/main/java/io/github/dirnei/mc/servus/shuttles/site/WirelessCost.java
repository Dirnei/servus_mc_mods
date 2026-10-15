package io.github.dirnei.mc.servus.shuttles.site;

/** What a wireless transfer costs in FE and how many items one tick may move. */
public final class WirelessCost {

    private WirelessCost() {
    }

    /** FE for one item over this distance in blocks. */
    public static double perItem(double distance, double base, double perBlock) {
        return Math.max(0, base) + Math.max(0, perBlock) * Math.max(0, distance);
    }

    /**
     * The FE a station must hold to count as powered: one item at the smallest
     * cost (no distance), rounded up, and never less than 1 FE.
     */
    public static long minimumCharge(double base) {
        return Math.max(1, cost(1, Math.max(0, base)));
    }

    /** Whether a station holding this much FE is powered, that is, on the wireless line. */
    public static boolean powered(long energy, double base) {
        return energy >= minimumCharge(base);
    }

    /** FE for n items, rounded up to whole FE. */
    public static long cost(int items, double perItem) {
        if (items <= 0 || perItem <= 0) {
            return 0;
        }
        return (long) Math.ceil(items * perItem - 1e-9);
    }

    /** How many items a buffer holding this much FE pays for, at most {@code wanted}. */
    public static int affordable(long energy, double perItem, int wanted) {
        if (wanted <= 0 || energy <= 0 && perItem > 0) {
            return 0;
        }
        if (perItem <= 0) {
            return wanted;
        }
        int items = (int) Math.min(wanted, Math.floor(energy / perItem + 1e-9));
        while (items > 0 && cost(items, perItem) > energy) {
            items--;
        }
        return Math.max(0, items);
    }

    /**
     * Items one tick may move: the smallest of what waits, what the transfer
     * still misses, the sender's and the receiver's rate allowance, the room at
     * the receiver and what the receiver's FE pays for.
     */
    public static int movable(int waiting, int missing, int sendAllowance, int receiveAllowance, int room, int affordable) {
        int moved = Math.min(Math.min(waiting, missing), Math.min(sendAllowance, receiveAllowance));
        return Math.max(0, Math.min(moved, Math.min(room, affordable)));
    }
}
