package io.github.dirnei.mc.servus.storage.cpu;

import java.util.ArrayList;
import java.util.List;

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

    /** The lowest corners of every cube that contains the block, lowest corner first (by y, then z, then x). */
    public static List<Vec> origins(int x, int y, int z) {
        List<Vec> list = new ArrayList<>(PARTS);
        for (int dy = SIZE - 1; dy >= 0; dy--) {
            for (int dz = SIZE - 1; dz >= 0; dz--) {
                for (int dx = SIZE - 1; dx >= 0; dx--) {
                    list.add(new Vec(x - dx, y - dy, z - dz));
                }
            }
        }
        return list;
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
            throw new IllegalArgumentException("Offset outside the cube: " + dx + "," + dy + "," + dz);
        }
        return dx + SIZE * dz + SIZE * SIZE * dy;
    }
}
