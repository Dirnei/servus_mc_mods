package io.github.dirnei.mc.servus.foundation.data.packet;

/**
 * Data line limits from datapack JSON. The network is instant, so only the
 * retry interval is left; throughput comes from transfer gates.
 *
 * @param retryTicks how often a delivery whose target was full offers its items again
 */
public record PacketLimits(int retryTicks) {

    public static final PacketLimits DEFAULTS = new PacketLimits(20);

    public PacketLimits {
        retryTicks = Math.max(1, retryTicks);
    }
}
