package io.github.dirnei.mc.servus.shuttles.field;

/**
 * How far a field reaches to each side of its rails: the head's base radius
 * plus one per Range Upgrade, never past the maximum. Pure Java.
 */
public final class FieldRadius {

    private FieldRadius() {
    }

    /** The radius with that many upgrades; a maximum below the base counts as the base. */
    public static int of(int base, int max, int upgrades) {
        int floor = Math.max(0, base);
        return Math.min(Math.max(floor, max), floor + Math.max(0, upgrades));
    }

    /** How many upgrades a head may hold in all: enough to reach the maximum, none past it. */
    public static int maxUpgrades(int base, int max) {
        return Math.max(0, max - Math.max(0, base));
    }
}
