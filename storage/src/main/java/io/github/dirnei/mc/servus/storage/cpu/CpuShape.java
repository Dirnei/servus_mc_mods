package io.github.dirnei.mc.servus.storage.cpu;

/**
 * The place classes of a Crafting CPU box. A formed block's part index
 * {@code x + 3z + 9y} names its place in the box per axis: 0 low, 1 middle,
 * 2 high (a side of 2 has no middle, see {@link CpuBox#placeClass}). Part 0
 * is the lowest corner, the master; it is the only block with that class.
 * Pure, no Minecraft imports.
 */
public final class CpuShape {

    public static final int SIZE = 3;
    public static final int PARTS = SIZE * SIZE * SIZE;

    /** A block position. */
    public record Vec(int x, int y, int z) {
    }

    private CpuShape() {
    }

    /** The place class per axis of a part (0 low, 1 middle, 2 high). */
    public static Vec offset(int part) {
        if (part < 0 || part >= PARTS) {
            throw new IllegalArgumentException("No CPU part " + part);
        }
        return new Vec(part % SIZE, part / (SIZE * SIZE), (part / SIZE) % SIZE);
    }

    /** The part index of a place class per axis (0 low, 1 middle, 2 high). */
    public static int part(int dx, int dy, int dz) {
        if (dx < 0 || dy < 0 || dz < 0 || dx >= SIZE || dy >= SIZE || dz >= SIZE) {
            throw new IllegalArgumentException("Place class outside 0 to 2: " + dx + "," + dy + "," + dz);
        }
        return dx + SIZE * dz + SIZE * SIZE * dy;
    }
}
