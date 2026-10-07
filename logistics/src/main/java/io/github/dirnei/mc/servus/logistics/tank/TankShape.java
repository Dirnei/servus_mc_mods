package io.github.dirnei.mc.servus.logistics.tank;

import io.github.dirnei.mc.servus.lib.graph.NodePos;
import java.util.Collection;

/**
 * The bounding box of a connected group of tank blocks. The group forms one
 * tank when it fills its box completely. Pure Java, no Minecraft imports.
 *
 * @param count number of blocks in the group
 */
public record TankShape(int count, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    /** The shape of the given positions. The collection must not be empty. */
    public static TankShape of(Collection<NodePos> positions) {
        if (positions.isEmpty()) {
            throw new IllegalArgumentException("A tank shape needs at least one block");
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (NodePos pos : positions) {
            minX = Math.min(minX, pos.x());
            minY = Math.min(minY, pos.y());
            minZ = Math.min(minZ, pos.z());
            maxX = Math.max(maxX, pos.x());
            maxY = Math.max(maxY, pos.y());
            maxZ = Math.max(maxZ, pos.z());
        }
        return new TankShape(positions.size(), minX, minY, minZ, maxX, maxY, maxZ);
    }

    public int width() {
        return maxX - minX + 1;
    }

    public int height() {
        return maxY - minY + 1;
    }

    public int depth() {
        return maxZ - minZ + 1;
    }

    /** Blocks in one horizontal layer of the box. */
    public int blocksPerLayer() {
        return width() * depth();
    }

    public long volume() {
        return (long) blocksPerLayer() * height();
    }

    /** True when the blocks fill the box completely. Assumes distinct positions. */
    public boolean isBox() {
        return count == volume();
    }

    /** Layer of a position, 0 at the bottom. */
    public int layer(NodePos pos) {
        return pos.y() - minY;
    }

    /** Fixed index of a position within its layer: x first, then z. */
    public int indexInLayer(NodePos pos) {
        return (pos.x() - minX) + (pos.z() - minZ) * width();
    }
}
