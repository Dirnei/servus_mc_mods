package io.github.dirnei.mc.servus.alchemy.homunculus;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the homunculus reaches: the 5x5 area centred on its stack, on the Receptaculum's level, without the
 * Receptaculum itself. Pure Java.
 */
public final class HomunculusReach {

    /** How far the reach goes from the stack along x and z. */
    public static final int RADIUS = 2;

    private static final List<Offset> OFFSETS = buildOffsets();

    /** A horizontal offset from the Receptaculum. */
    public record Offset(int dx, int dz) {
    }

    private HomunculusReach() {
    }

    /** Every offset in reach, row by row from the north west corner; never the centre. */
    public static List<Offset> offsets() {
        return OFFSETS;
    }

    /** Whether a block at this offset from the Receptaculum is in reach; {@code dy} must be 0, the same level. */
    public static boolean inReach(int dx, int dy, int dz) {
        return dy == 0 && (dx != 0 || dz != 0) && Math.abs(dx) <= RADIUS && Math.abs(dz) <= RADIUS;
    }

    private static List<Offset> buildOffsets() {
        List<Offset> offsets = new ArrayList<>();
        for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                if (inReach(dx, 0, dz)) {
                    offsets.add(new Offset(dx, dz));
                }
            }
        }
        return List.copyOf(offsets);
    }
}
