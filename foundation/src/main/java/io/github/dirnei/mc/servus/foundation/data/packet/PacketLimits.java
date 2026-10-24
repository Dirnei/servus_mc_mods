package io.github.dirnei.mc.servus.foundation.data.packet;

/**
 * Data line limits from datapack JSON. The network is instant, so only
 * timings are left; throughput comes from transfer gates.
 *
 * @param retryTicks     how often a delivery whose target was full offers its items again
 * @param grantHoldTicks how long a grant no delivery took up stays open without taking items
 */
public record PacketLimits(int retryTicks, int grantHoldTicks) {

    public static final PacketLimits DEFAULTS = new PacketLimits(20, 1200);

    public PacketLimits {
        retryTicks = Math.max(1, retryTicks);
        grantHoldTicks = Math.max(1, grantHoldTicks);
    }
}
