package io.github.dirnei.mc.servus.foundation.logistics.tank;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    // Full boxes

    /** A completely filled box with the given corners, inclusive. */
    public static TankShape box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        return new TankShape((int) volume, minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** The one block box at a position. */
    public static TankShape single(NodePos pos) {
        return box(pos.x(), pos.y(), pos.z(), pos.x(), pos.y(), pos.z());
    }

    public boolean contains(NodePos pos) {
        return pos.x() >= minX && pos.x() <= maxX && pos.y() >= minY && pos.y() <= maxY
                && pos.z() >= minZ && pos.z() <= maxZ;
    }

    /** Every position of the box. */
    public List<NodePos> positions() {
        List<NodePos> list = new ArrayList<>((int) Math.min(volume(), 1 << 16));
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    list.add(new NodePos(x, y, z));
                }
            }
        }
        return list;
    }

    /**
     * The box both boxes fill together, when they touch with matching faces:
     * neighbours along one axis with the same extent on the other two. Else empty.
     */
    public Optional<TankShape> mergeWith(TankShape other) {
        boolean sameX = minX == other.minX && maxX == other.maxX;
        boolean sameY = minY == other.minY && maxY == other.maxY;
        boolean sameZ = minZ == other.minZ && maxZ == other.maxZ;
        boolean touchX = maxX + 1 == other.minX || other.maxX + 1 == minX;
        boolean touchY = maxY + 1 == other.minY || other.maxY + 1 == minY;
        boolean touchZ = maxZ + 1 == other.minZ || other.maxZ + 1 == minZ;
        if ((sameY && sameZ && touchX) || (sameX && sameZ && touchY) || (sameX && sameY && touchZ)) {
            return Optional.of(box(Math.min(minX, other.minX), Math.min(minY, other.minY), Math.min(minZ, other.minZ),
                    Math.max(maxX, other.maxX), Math.max(maxY, other.maxY), Math.max(maxZ, other.maxZ)));
        }
        return Optional.empty();
    }
}
