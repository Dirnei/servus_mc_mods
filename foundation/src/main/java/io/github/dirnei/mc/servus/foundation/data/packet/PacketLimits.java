package io.github.dirnei.mc.servus.foundation.data.packet;

/**
 * Packet limits from datapack JSON. Pure, no Minecraft imports.
 *
 * @param size            items per packet
 * @param blocksPerSecond travel speed
 * @param retryTicks      how often items waiting at a full target are offered again
 */
public record PacketLimits(int size, double blocksPerSecond, int retryTicks) {

    public static final PacketLimits DEFAULTS = new PacketLimits(8, 4.0, 20);

    public PacketLimits {
        size = Math.max(1, size);
        blocksPerSecond = blocksPerSecond > 0 ? blocksPerSecond : DEFAULTS.blocksPerSecond;
        retryTicks = Math.max(1, retryTicks);
    }

    public double blocksPerTick() {
        return blocksPerSecond / 20.0;
    }
}
