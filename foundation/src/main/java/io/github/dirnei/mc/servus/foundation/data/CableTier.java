package io.github.dirnei.mc.servus.foundation.data;

/**
 * The stats of one data cable tier from datapack JSON. Pure, no Minecraft imports.
 *
 * @param itemsPerSecond how many items pass one duct per second
 * @param duplex         true when each direction has the full bandwidth, false when both share it
 */
public record CableTier(double itemsPerSecond, boolean duplex) {

    /** The base Data Cable: 32 items per second, shared by both directions. */
    public static final CableTier BASE = new CableTier(32, false);

    public CableTier {
        itemsPerSecond = Math.max(0, itemsPerSecond);
    }

    /**
     * What decides whether a tier is higher than another: the items per second
     * both directions together can carry.
     */
    public double capacity() {
        return duplex ? itemsPerSecond * 2 : itemsPerSecond;
    }
}
