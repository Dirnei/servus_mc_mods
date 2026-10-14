package io.github.dirnei.mc.servus.foundation.logistics.duct;

import java.util.List;

/**
 * The boxes a duct is made of, in pixels of a 16 pixel block: a centre box
 * whose radius grows with the number of lines, and one straight arm per side
 * as thick as the lines that connect there, all placed by the duct's resting
 * pose (see {@link DuctPose}). Where an arm meets its face at another height
 * than the duct sits, the centre grows up or down to reach it, so the bend
 * happens in the middle of the block. Sides are numbered like
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
            return contains(x, y, z, 0);
        }

        /** Whether the point lies in the box grown by {@code tolerance} on every side. */
        public boolean contains(double x, double y, double z, double tolerance) {
            return x >= x0 - tolerance && x <= x1 + tolerance
                    && y >= y0 - tolerance && y <= y1 + tolerance
                    && z >= z0 - tolerance && z <= z1 + tolerance;
        }
    }

    /** How much a junction cube of two or more lines grows beyond the run centre, so it reads as a fitting. */
    public static final int JUNCTION_EXTRA = 1;

    /** Gap in pixels between a duct and its floor, so their faces never share a plane. */
    public static final float LIFT = 0.05F;

    /** Depth in pixels of the channel ring at the face end of an endpoint arm. */
    public static final float RING_DEPTH = 1;

    /** Panel thickness in pixels. */
    public static final int PANEL_THICKNESS = 1;
    /** The hole of a hollow panel runs from this pixel to {@code 16 - HOLE_EDGE}, wide enough for the thickest arm. */
    public static final int HOLE_EDGE = 8 - THICKEST_RADIUS;

    private DuctShapes() {
    }

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

    /** The centre cube in the middle of the block. */
    public static Box center(int radius) {
        return center(radius, DuctPose.CENTRED);
    }

    /** The arm on a side of a centred duct, as thick as its centre, from the centre's face to the block edge. */
    public static Box arm(int side, int radius) {
        return arm(side, radius, center(radius), 0);
    }

    /**
     * The radius of the arm on each side, zero where no line has an arm.
     *
     * @param armMasks per line, the six bit mask of the sides with an arm of that line
     */
    public static int[] armRadii(int[] armMasks) {
        int[] radii = new int[6];
        for (int side = 0; side < 6; side++) {
            int lines = 0;
            for (int mask : armMasks == null ? new int[0] : armMasks) {
                if ((mask & 1 << side) != 0) {
                    lines++;
                }
            }
            radii[side] = lines == 0 ? 0 : radius(lines);
        }
        return radii;
    }

    /** The bottom in pixels of a part of the given radius at a height. */
    public static float low(int radius, DuctPose.Height height) {
        return height == DuctPose.Height.LOW ? LIFT : 8 - radius;
    }

    /** The centre box of a duct at its resting pose without arms, in the middle across the block. */
    public static Box center(int radius, int pose) {
        float y = low(radius, DuctPose.height(pose));
        return new Box(8 - radius, y, 8 - radius, 8 + radius, y + 2 * radius, 8 + radius);
    }

    /**
     * The centre box of a duct at its resting pose, in the middle across the
     * block, grown up or down until it spans every horizontal arm at the
     * height where that arm meets its face.
     *
     * @param pose      the duct's resting pose, see {@link DuctPose}
     * @param faces     where the arms meet their faces, see {@link DuctPose#faces}
     * @param armRadius radius of the arm per side, zero where there is no arm
     */
    public static Box center(int radius, int pose, int faces, int[] armRadius) {
        Box own = center(radius, pose);
        float y0 = own.y0();
        float y1 = own.y1();
        for (int side = NORTH; side <= EAST && side < armRadius.length; side++) {
            if (armRadius[side] > 0) {
                float face = low(armRadius[side], DuctPose.faceHeight(faces, side));
                y0 = Math.min(y0, face);
                y1 = Math.max(y1, face + 2 * armRadius[side]);
            }
        }
        return new Box(own.x0(), y0, own.z0(), own.x1(), y1, own.z1());
    }

    /**
     * The arm on a side: one straight box from the centre's face to the block
     * edge, at the height where the arm meets its face for a horizontal arm.
     *
     * @param armRadius radius of the lines connected on that side
     * @param centre    the duct's centre box, see {@link #center(int, int, int, int[])}
     * @param faces     where the arms meet their faces, see {@link DuctPose#faces}
     */
    public static Box arm(int side, int armRadius, Box centre, int faces) {
        int along = DuctPose.axisOf(side);
        boolean positive = DuctPose.isPositive(side);
        float inner = positive ? max(centre, along) : min(centre, along);
        float edge = positive ? 16 : 0;
        float face = low(armRadius, DuctPose.faceHeight(faces, side));
        return armBox(along, inner, edge, armRadius, face, face + 2 * armRadius);
    }

    /** The lower bound of a box on an axis, {@link DuctPose#X} to {@link DuctPose#Z}. */
    public static float min(Box box, int axis) {
        return switch (axis) {
            case DuctPose.X -> box.x0();
            case DuctPose.Y -> box.y0();
            default -> box.z0();
        };
    }

    /** The upper bound of a box on an axis, {@link DuctPose#X} to {@link DuctPose#Z}. */
    public static float max(Box box, int axis) {
        return switch (axis) {
            case DuctPose.X -> box.x1();
            case DuctPose.Y -> box.y1();
            default -> box.z1();
        };
    }

    /**
     * A box between {@code a0} and {@code a1} on the arm axis, between
     * {@code y0} and {@code y1} on y unless that is the arm axis, and
     * {@code radius} around the middle on the remaining axis.
     */
    private static Box armBox(int along, float a0, float a1, int radius, float y0, float y1) {
        float[] min = {8 - radius, y0, 8 - radius};
        float[] max = {8 + radius, y1, 8 + radius};
        min[along] = Math.min(a0, a1);
        max[along] = Math.max(a0, a1);
        return new Box(min[0], min[1], min[2], max[0], max[1], max[2]);
    }

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
