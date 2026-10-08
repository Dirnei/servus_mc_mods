package io.github.dirnei.mc.servus.storage.cpu;

import java.util.ArrayList;
import java.util.List;

/**
 * The 3x3x3 cube of a Crafting CPU. A cube is named by its lowest corner;
 * every block has a part index {@code x + 3z + 9y} from that corner, part 0
 * being the master. Pure, no Minecraft imports.
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

    /** The offset of a part from the cube's lowest corner. */
    public static Vec offset(int part) {
        if (part < 0 || part >= PARTS) {
            throw new IllegalArgumentException("No CPU part " + part);
        }
        return new Vec(part % SIZE, part / (SIZE * SIZE), (part / SIZE) % SIZE);
    }

    /** The part index of an offset from the lowest corner. */
    public static int part(int dx, int dy, int dz) {
        if (dx < 0 || dy < 0 || dz < 0 || dx >= SIZE || dy >= SIZE || dz >= SIZE) {
            throw new IllegalArgumentException("Offset outside the cube: " + dx + "," + dy + "," + dz);
        }
        return dx + SIZE * dz + SIZE * SIZE * dy;
    }
}
