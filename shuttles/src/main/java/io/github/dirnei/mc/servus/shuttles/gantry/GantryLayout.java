package io.github.dirnei.mc.servus.shuttles.gantry;

import java.util.EnumMap;
import java.util.Map;

/**
 * Where the five parts of a Fluid Gantry stand around its rail. The rail runs
 * along {@link Run}; the legs stand on both sides of it, at right angles to the
 * run, two blocks tall, and the beam fills the space above the rail. Left is
 * the negative side across the run: west of a north to south rail, north of
 * an east to west rail. The master part is {@link Part#LEFT_LOW}. Pure Java.
 */
public final class GantryLayout {

    /** The axis the rail runs along. */
    public enum Run { X, Z }

    /** The parts of a gantry; {@link #LEFT_LOW} is the master that keeps the block entity. */
    public enum Part {
        LEFT_LOW(-1, 0), LEFT_HIGH(-1, 1), RIGHT_LOW(1, 0), RIGHT_HIGH(1, 1), BEAM(0, 1);

        /** Steps across the run from the rail: -1 left, 1 right, 0 over it. */
        private final int across;
        private final int up;

        Part(int across, int up) {
            this.across = across;
            this.up = up;
        }

        public boolean isLeg() {
            return this != BEAM;
        }

        /** Which tank the leg holds: 0 left, 1 right; -1 for the beam. */
        public int tank() {
            return across < 0 ? 0 : across > 0 ? 1 : -1;
        }
    }

    /** A block position as plain coordinates. */
    public record Vec(int x, int y, int z) {

        public Vec plus(Vec other) {
            return new Vec(x + other.x, y + other.y, z + other.z);
        }

        public Vec minus(Vec other) {
            return new Vec(x - other.x, y - other.y, z - other.z);
        }
    }

    private GantryLayout() {
    }

    /** The offset of a part from the rail it stands over. */
    public static Vec offset(Part part, Run run) {
        return run == Run.Z ? new Vec(part.across, part.up, 0) : new Vec(0, part.up, part.across);
    }

    /** Every part's position for a gantry over the rail at {@code rail}. */
    public static Map<Part, Vec> parts(Vec rail, Run run) {
        Map<Part, Vec> parts = new EnumMap<>(Part.class);
        for (Part part : Part.values()) {
            parts.put(part, rail.plus(offset(part, run)));
        }
        return parts;
    }

    /** The rail position of the gantry the part at {@code pos} belongs to. */
    public static Vec rail(Part part, Vec pos, Run run) {
        return pos.minus(offset(part, run));
    }

    /** The master's position of the gantry the part at {@code pos} belongs to. */
    public static Vec master(Part part, Vec pos, Run run) {
        return rail(part, pos, run).plus(offset(Part.LEFT_LOW, run));
    }
}
