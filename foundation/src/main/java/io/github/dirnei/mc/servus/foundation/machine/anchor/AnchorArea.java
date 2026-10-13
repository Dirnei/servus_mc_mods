package io.github.dirnei.mc.servus.foundation.machine.anchor;

import java.util.ArrayList;
import java.util.List;

/**
 * The square of chunks an Ender Anchor holds: radius equal to its upgrades,
 * shifted by an offset that keeps the anchor's own chunk inside. Pure; chunks
 * are packed like vanilla {@code ChunkPos#pack} (x in the low 32 bits, z in
 * the high 32 bits).
 */
public record AnchorArea(int radius, int offsetX, int offsetZ) {

    /** A shift of the offset by one chunk; the ordinal is the menu button id. */
    public enum Shift {
        NORTH(0, -1), SOUTH(0, 1), EAST(1, 0), WEST(-1, 0);

        private final int dx;
        private final int dz;

        Shift(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }

        public int dx() {
            return dx;
        }

        public int dz() {
            return dz;
        }

        /** The shift for a menu button id, or null for any other id. */
        public static Shift byId(int id) {
            Shift[] values = values();
            return id >= 0 && id < values.length ? values[id] : null;
        }
    }

    public AnchorArea {
        radius = Math.max(0, radius);
        offsetX = clamp(offsetX, radius);
        offsetZ = clamp(offsetZ, radius);
    }

    /** The area of an anchor holding {@code upgrades} upgrades, the offset clamped to the radius. */
    public static AnchorArea of(int upgrades, int offsetX, int offsetZ) {
        return new AnchorArea(upgrades, offsetX, offsetZ);
    }

    private static int clamp(int value, int radius) {
        return Math.max(-radius, Math.min(radius, value));
    }

    /** Whether the offset may move one chunk that way. */
    public boolean canShift(Shift shift) {
        return Math.abs(offsetX + shift.dx()) <= radius && Math.abs(offsetZ + shift.dz()) <= radius;
    }

    /** The area shifted one chunk, or this area when that would leave the limit. */
    public AnchorArea shift(Shift shift) {
        if (!canShift(shift)) {
            return this;
        }
        return new AnchorArea(radius, offsetX + shift.dx(), offsetZ + shift.dz());
    }

    /** The same offset clamped to another radius, for when upgrades go in or out. */
    public AnchorArea withRadius(int newRadius) {
        return new AnchorArea(newRadius, offsetX, offsetZ);
    }

    /** Chunks along one side of the square. */
    public int side() {
        return 2 * radius + 1;
    }

    /** How many chunks the square holds. */
    public int size() {
        return side() * side();
    }

    /** The square's smallest chunk x for an anchor in chunk {@code centreX}. */
    public int minX(int centreX) {
        return centreX + offsetX - radius;
    }

    public int maxX(int centreX) {
        return centreX + offsetX + radius;
    }

    public int minZ(int centreZ) {
        return centreZ + offsetZ - radius;
    }

    public int maxZ(int centreZ) {
        return centreZ + offsetZ + radius;
    }

    /** Whether the chunk lies in the square of an anchor in chunk ({@code centreX}, {@code centreZ}). */
    public boolean contains(int centreX, int centreZ, int x, int z) {
        return x >= minX(centreX) && x <= maxX(centreX) && z >= minZ(centreZ) && z <= maxZ(centreZ);
    }

    /** Every chunk of the square of an anchor in chunk ({@code centreX}, {@code centreZ}), packed. */
    public List<Long> chunks(int centreX, int centreZ) {
        List<Long> result = new ArrayList<>(size());
        for (int z = minZ(centreZ); z <= maxZ(centreZ); z++) {
            for (int x = minX(centreX); x <= maxX(centreX); x++) {
                result.add(pack(x, z));
            }
        }
        return result;
    }

    public static long pack(int x, int z) {
        return (x & 0xFFFFFFFFL) | ((z & 0xFFFFFFFFL) << 32);
    }

    public static int unpackX(long packed) {
        return (int) packed;
    }

    public static int unpackZ(long packed) {
        return (int) (packed >>> 32);
    }
}
