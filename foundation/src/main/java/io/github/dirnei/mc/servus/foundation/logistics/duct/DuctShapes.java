package io.github.dirnei.mc.servus.foundation.logistics.duct;

import java.util.List;

/**
 * The boxes a duct is made of, in pixels of a 16 pixel block: a centre cube
 * whose radius grows with the number of lines, and one arm per side as thick
 * as the lines that connect there, all placed by the duct's resting pose
 * (see {@link DuctPose}). Sides are numbered like
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
        return armBoxes(side, radius, radius, DuctPose.CENTRED, 0).segment();
    }

    /** The bottom in pixels of a part of the given radius at a height. */
    public static float low(int radius, DuctPose.Height height) {
        return height == DuctPose.Height.LOW ? LIFT : 8 - radius;
    }

    /** The centre cube of a duct at its resting pose, in the middle across the block. */
    public static Box center(int radius, int pose) {
        float y = low(radius, DuctPose.height(pose));
        return new Box(8 - radius, y, 8 - radius, 8 + radius, y + 2 * radius, 8 + radius);
    }

    /**
     * The boxes of an arm: where the duct and the arm's face height line up,
     * one straight segment from the centre's face to the block edge;
     * otherwise a step against the centre that spans both heights, and the
     * segment at the face height from the step to the block edge.
     *
     * @param step    the step, null for a straight arm
     * @param segment the segment that reaches the block edge
     */
    public record ArmBoxes(Box step, Box segment) {

        /** The step if present, then the segment. */
        public List<Box> all() {
            return step == null ? List.of(segment) : List.of(step, segment);
        }
    }

    /**
     * The boxes of the arm on a side for a duct at {@code pose}. Only a
     * horizontal arm can step, and only up or down.
     *
     * @param armRadius    radius of the lines connected on that side
     * @param centreRadius radius of the duct's centre cube
     * @param pose         the duct's resting pose, see {@link DuctPose}
     * @param faces        where the arms meet their faces, see {@link DuctPose#faces}
     */
    public static ArmBoxes armBoxes(int side, int armRadius, int centreRadius, int pose, int faces) {
        int along = DuctPose.axisOf(side);
        boolean positive = DuctPose.isPositive(side);
        float centreLow = along == DuctPose.Y ? low(centreRadius, DuctPose.height(pose)) : 8 - centreRadius;
        float inner = positive ? centreLow + 2 * centreRadius : centreLow;
        float edge = positive ? 16 : 0;
        float own = low(armRadius, DuctPose.height(pose));
        float face = low(armRadius, DuctPose.faceHeight(faces, side));
        if (DuctPose.isVertical(side) || own == face) {
            return new ArmBoxes(null, armBox(along, inner, edge, armRadius, face, face + 2 * armRadius));
        }
        float stepEnd = positive ? inner + stepLength(armRadius, centreRadius) : inner - stepLength(armRadius, centreRadius);
        Box step = armBox(along, inner, stepEnd, armRadius, Math.min(own, face), Math.max(own, face) + 2 * armRadius);
        return new ArmBoxes(step, armBox(along, stepEnd, edge, armRadius, face, face + 2 * armRadius));
    }

    /** {@link #armBoxes} as a list, step first. */
    public static List<Box> arm(int side, int armRadius, int centreRadius, int pose, int faces) {
        return armBoxes(side, armRadius, centreRadius, pose, faces).all();
    }

    /**
     * A step is as long as the arm is thick, but leaves the ring depth free at
     * the block edge so the step never reaches the ring's outer face.
     */
    private static float stepLength(int armRadius, int centreRadius) {
        return Math.min(2 * armRadius, 8 - centreRadius - RING_DEPTH);
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
