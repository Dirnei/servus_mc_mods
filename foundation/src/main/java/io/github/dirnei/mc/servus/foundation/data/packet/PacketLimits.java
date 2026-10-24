package io.github.dirnei.mc.servus.foundation.data.packet;

/**
 * Data line limits from datapack JSON. The network is instant, so only
 * timings are left; throughput comes from transfer gates.
 *
 * @param retryTicks     how often a delivery whose target was full offers its items again
 * @param grantHoldTicks how long a grant no delivery took up stays open without taking items
 * @param stallTicks     how long a delivery without a grant may move nothing into a full target before it ends
 */
public record PacketLimits(int retryTicks, int grantHoldTicks, int stallTicks) {

    public static final PacketLimits DEFAULTS = new PacketLimits(20, 1200, 600);

    public PacketLimits {
        retryTicks = Math.max(1, retryTicks);
        grantHoldTicks = Math.max(1, grantHoldTicks);
        stallTicks = Math.max(1, stallTicks);
    }
}
