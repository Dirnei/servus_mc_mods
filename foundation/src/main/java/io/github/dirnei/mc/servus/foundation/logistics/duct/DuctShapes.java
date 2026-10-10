package io.github.dirnei.mc.servus.foundation.logistics.duct;

import java.util.List;

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
    /** Half width in pixels of a part carrying three lines. */
    public static final int THICKEST_RADIUS = 4;

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

    /** How much a junction cube of two or more lines grows beyond the run centre, so it reads as a fitting. */
    public static final int JUNCTION_EXTRA = 1;

    /** Radius by line count: one line is thin, two lines thick, three or more lines thickest. */
    public static int radius(int lineCount) {
        if (lineCount >= 3) {
            return THICKEST_RADIUS;
        }
        return lineCount == 2 ? THICK_RADIUS : THIN_RADIUS;
    }

    /** Whether arms in the six bit side mask lie on more than one axis, so the centre is a junction. */
    public static boolean isJunction(int armsMask) {
        int axes = 0;
        if ((armsMask & (1 << DOWN | 1 << UP)) != 0) {
            axes++;
        }
        if ((armsMask & (1 << NORTH | 1 << SOUTH)) != 0) {
            axes++;
        }
        if ((armsMask & (1 << WEST | 1 << EAST)) != 0) {
            axes++;
        }
        return axes > 1;
    }

    /** The centre radius: the line radius, one pixel more for a junction of two or more lines. */
    public static int centerRadius(int lineCount, int armsMask) {
        int r = radius(lineCount);
        return lineCount >= 2 && isJunction(armsMask) ? r + JUNCTION_EXTRA : r;
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

    /** Panel thickness in pixels. */
    public static final int PANEL_THICKNESS = 1;
    /** The hole of a hollow panel runs from this pixel to {@code 16 - HOLE_EDGE}, wide enough for the thickest arm. */
    public static final int HOLE_EDGE = 8 - THICKEST_RADIUS;

    /**
     * The boxes of a panel on a side: one full slab when solid, four boxes
     * framing the hole when hollow.
     */
    public static List<Box> panel(int side, boolean hollow) {
        if (!hollow) {
            return List.of(onSide(side, 0, 0, 16, 16));
        }
        int a = HOLE_EDGE;
        int b = 16 - HOLE_EDGE;
        return List.of(
                onSide(side, 0, 0, 16, a),
                onSide(side, 0, b, 16, 16),
                onSide(side, 0, a, a, b),
                onSide(side, b, a, 16, b));
    }

    /**
     * A one pixel box on a side covering {@code u0..u1} by {@code v0..v1} of
     * that face; u and v are the two other axes in x, y, z order.
     */
    private static Box onSide(int side, float u0, float v0, float u1, float v1) {
        float lo = (side & 1) == 0 ? 0 : 16 - PANEL_THICKNESS;
        float hi = lo + PANEL_THICKNESS;
        return switch (side) {
            case DOWN, UP -> new Box(u0, lo, v0, u1, hi, v1);
            case NORTH, SOUTH -> new Box(u0, v0, lo, u1, v1, hi);
            case WEST, EAST -> new Box(lo, u0, v0, hi, u1, v1);
            default -> throw new IllegalArgumentException("side " + side);
        };
    }
}
