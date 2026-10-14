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

    /** Gap in pixels between a resting duct and its floor or wall, so their faces never share a plane. */
    public static final float LIFT = 0.05F;
    /** Depth in pixels of the channel ring at the face end of an endpoint arm. */
    public static final float RING_DEPTH = 1;

    /**
     * How far the step and the segment of a stepping arm are drawn in from
     * their cross section, times one plus the arm axis, so stepping arms on
     * different axes never share a face plane where they cross.
     */
    public static final float STEP_INSET = 0.005F;

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
        return arm(side, radius, radius, DuctPose.CENTRED, 0).get(0);
    }

    /** The low end on one axis of a part of the given radius that sits at {@code side} of the block. */
    public static float low(int radius, DuctPose.Side side) {
        return switch (side) {
            case NEG -> LIFT;
            case MID -> 8 - radius;
            case POS -> 16 - LIFT - 2 * radius;
        };
    }

    /** The centre cube of a duct at its resting pose. */
    public static Box center(int radius, int pose) {
        float[] min = new float[3];
        float[] max = new float[3];
        for (int axis = DuctPose.X; axis <= DuctPose.Z; axis++) {
            min[axis] = low(radius, DuctPose.side(pose, axis));
            max[axis] = min[axis] + 2 * radius;
        }
        return box(min, max);
    }

    /**
     * The boxes of an arm: where the duct and the arm's face position line
     * up, one straight segment from the centre's face to the block edge;
     * otherwise a step against the centre that spans both cross sections and
     * a segment at the face position from the step to the block edge, both
     * drawn in by {@link #STEP_INSET}.
     */
    public record ArmBoxes(Box step, Box segment) {

        /** The step and the segment that are present, step first. */
        public List<Box> all() {
            if (step == null) {
                return List.of(segment);
            }
            return segment == null ? List.of(step) : List.of(step, segment);
        }
    }

    /**
     * The boxes of the arm on a side for a duct at {@code pose}.
     *
     * @param armRadius    radius of the lines connected on that side
     * @param centreRadius radius of the duct's centre cube
     * @param pose         the duct's resting pose, see {@link DuctPose}
     * @param faces        where the arms meet their faces, see {@link DuctPose#faces}
     */
    public static ArmBoxes armBoxes(int side, int armRadius, int centreRadius, int pose, int faces) {
        int along = DuctPose.axisOf(side);
        boolean positive = DuctPose.isPositive(side);
        float centreLow = low(centreRadius, DuctPose.side(pose, along));
        float inner = positive ? centreLow + 2 * centreRadius : centreLow;
        float edge = positive ? 16 : 0;
        float[] own = new float[3];
        float[] face = new float[3];
        boolean steps = false;
        for (int slot = 0; slot < 2; slot++) {
            int axis = DuctPose.acrossAxis(side, slot);
            own[axis] = low(armRadius, DuctPose.side(pose, axis));
            face[axis] = low(armRadius, DuctPose.faceSide(faces, side, axis));
            steps |= own[axis] != face[axis];
        }
        if (!steps) {
            return new ArmBoxes(null, armBox(along, inner, edge, face, armRadius, 0));
        }
        float room = Math.abs(edge - inner);
        float length = stepLength(armRadius, room);
        float stepEnd = positive ? inner + length : inner - length;
        float inset = STEP_INSET * (along + 1);
        float[] min = new float[3];
        float[] max = new float[3];
        for (int slot = 0; slot < 2; slot++) {
            int axis = DuctPose.acrossAxis(side, slot);
            min[axis] = Math.min(own[axis], face[axis]) + inset;
            max[axis] = Math.max(own[axis], face[axis]) + 2 * armRadius - inset;
        }
        min[along] = Math.min(inner, stepEnd);
        max[along] = Math.max(inner, stepEnd);
        Box segment = length < room ? armBox(along, stepEnd, edge, face, armRadius, inset) : null;
        return new ArmBoxes(box(min, max), segment);
    }

    /** {@link #armBoxes} as a list, step first. */
    public static List<Box> arm(int side, int armRadius, int centreRadius, int pose, int faces) {
        return armBoxes(side, armRadius, centreRadius, pose, faces).all();
    }

    /**
     * A step is as long as the arm is thick, but leaves the ring depth free at
     * the block edge so the step never shares the ring's outer face; with no
     * room for that it fills what is left.
     */
    private static float stepLength(int armRadius, float room) {
        return room <= RING_DEPTH ? room : Math.min(2 * armRadius, room - RING_DEPTH);
    }

    /**
     * A box between {@code a0} and {@code a1} on the arm axis, {@code radius}
     * wide from {@code low} on the other axes and drawn in there by {@code inset}.
     */
    private static Box armBox(int along, float a0, float a1, float[] low, int radius, float inset) {
        float[] min = new float[3];
        float[] max = new float[3];
        for (int axis = DuctPose.X; axis <= DuctPose.Z; axis++) {
            min[axis] = low[axis] + inset;
            max[axis] = low[axis] + 2 * radius - inset;
        }
        min[along] = Math.min(a0, a1);
        max[along] = Math.max(a0, a1);
        return box(min, max);
    }

    private static Box box(float[] min, float[] max) {
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
