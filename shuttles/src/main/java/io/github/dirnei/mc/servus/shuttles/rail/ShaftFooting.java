package io.github.dirnei.mc.servus.shuttles.rail;

/**
 * Whether an entity stands on an elevator segment, like on scaffolding: only
 * from above and only while it is not descending. Pure Java.
 */
public final class ShaftFooting {

    private ShaftFooting() {
    }

    /**
     * @param above      the entity's feet are at or above the top of the segment
     * @param descending the entity is descending (a sneaking player)
     */
    public static boolean stands(boolean above, boolean descending) {
        return above && !descending;
    }
}
