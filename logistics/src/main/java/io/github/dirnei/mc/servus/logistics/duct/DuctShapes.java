package io.github.dirnei.mc.servus.logistics.duct;

/**
 * The boxes a duct is made of, in pixels of a 16 pixel block: a centre cube
 * whose radius grows with the number of lines, and one arm per side as thick
 * as the lines that connect there. Sides are numbered like
 * {@code Direction.ordinal()}: down, up, north, south, west, east. Pure Java.
 */
public final class DuctShapes {

    /** Half width in pixels of a part carrying one line. */
    public static final int THIN_RADIUS = 2;
    /** Half width in pixels of a part carrying two lines. */
    public static final int THICK_RADIUS = 3;

    public static final int DOWN = 0;
    public static final int UP = 1;
    public static final int NORTH = 2;
    public static final int SOUTH = 3;
    public static final int WEST = 4;
    public static final int EAST = 5;

    /** An axis aligned box in pixels, {@code x0 <= x1} and so on. */
    public record Box(float x0, float y0, float z0, float x1, float y1, float z1) {

        public boolean contains(double x, double y, double z) {
            return x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1;
        }
    }

    private DuctShapes() {
    }

    /** Radius by line count: one line is thin, two or more lines thick. */
    public static int radius(int lineCount) {
        return lineCount >= 2 ? THICK_RADIUS : THIN_RADIUS;
    }

    public static Box center(int radius) {
        return new Box(8 - radius, 8 - radius, 8 - radius, 8 + radius, 8 + radius, 8 + radius);
    }

    /** The arm on a side, from the block edge to the centre of the block. */
    public static Box arm(int side, int radius) {
        int lo = 8 - radius;
        int hi = 8 + radius;
        return switch (side) {
            case DOWN -> new Box(lo, 0, lo, hi, lo, hi);
            case UP -> new Box(lo, hi, lo, hi, 16, hi);
            case NORTH -> new Box(lo, lo, 0, hi, hi, lo);
            case SOUTH -> new Box(lo, lo, hi, hi, hi, 16);
            case WEST -> new Box(0, lo, lo, lo, hi, hi);
            case EAST -> new Box(hi, lo, lo, 16, hi, hi);
            default -> throw new IllegalArgumentException("side " + side);
        };
    }
}
