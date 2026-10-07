package io.github.dirnei.mc.servus.logistics.tank;

/**
 * How a formed tank's content is laid out over its blocks: the lowest layer
 * fills first, every block of a layer holds the same amount, and the amount
 * of a partly filled layer that does not divide evenly goes one unit each to
 * the first blocks of that layer, so the shares always add up to the total.
 * Pure Java, no Minecraft imports.
 *
 * @param blocksPerLayer   blocks in one horizontal layer, at least 1
 * @param layers           number of layers, at least 1
 * @param capacityPerBlock capacity of one block in mB
 * @param total            content in mB, already clamped to the capacity
 */
public record TankLayers(int blocksPerLayer, int layers, int capacityPerBlock, long total) {

    public TankLayers {
        if (blocksPerLayer < 1 || layers < 1 || capacityPerBlock < 0) {
            throw new IllegalArgumentException("Invalid tank layout " + blocksPerLayer + "x" + layers + " at " + capacityPerBlock);
        }
        total = Math.max(0, Math.min(total, (long) blocksPerLayer * layers * capacityPerBlock));
    }

    /** Lays out {@code total} mB, clamped to the box's capacity. */
    public static TankLayers of(int blocksPerLayer, int layers, int capacityPerBlock, long total) {
        return new TankLayers(blocksPerLayer, layers, capacityPerBlock, total);
    }

    private long layerCapacity() {
        return (long) blocksPerLayer * capacityPerBlock;
    }

    /** Layers that are completely full. */
    public int fullLayers() {
        long perLayer = layerCapacity();
        return perLayer == 0 ? 0 : (int) (total / perLayer);
    }

    /** The amount of the block at {@code index} (0 based, fixed order) in {@code layer} (0 at the bottom). */
    public int amount(int layer, int index) {
        int full = fullLayers();
        if (layer < full) {
            return capacityPerBlock;
        }
        if (layer > full) {
            return 0;
        }
        long partial = total - full * layerCapacity();
        int even = (int) (partial / blocksPerLayer);
        int remainder = (int) (partial % blocksPerLayer);
        return even + (index < remainder ? 1 : 0);
    }
}
