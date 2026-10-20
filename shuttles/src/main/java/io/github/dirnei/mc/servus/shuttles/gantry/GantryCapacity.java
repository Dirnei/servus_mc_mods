package io.github.dirnei.mc.servus.shuttles.gantry;

/** The capacity of a gantry tank with its Capacity Upgrades. Pure Java. */
public final class GantryCapacity {

    private GantryCapacity() {
    }

    /** {@code base} plus {@code perUpgrade} for every upgrade, capped at the largest int. */
    public static int capacity(int base, int perUpgrade, int upgrades) {
        long capacity = (long) Math.max(0, base) + (long) Math.max(0, perUpgrade) * Math.max(0, upgrades);
        return (int) Math.min(Integer.MAX_VALUE, capacity);
    }

    /** Whether one upgrade may come out while the fullest tank holds {@code largestAmount}. */
    public static boolean canRemoveUpgrade(int base, int perUpgrade, int upgrades, int largestAmount) {
        return upgrades > 0 && largestAmount <= capacity(base, perUpgrade, upgrades - 1);
    }
}
