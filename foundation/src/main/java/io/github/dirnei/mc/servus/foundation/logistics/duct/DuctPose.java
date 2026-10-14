package io.github.dirnei.mc.servus.foundation.logistics.duct;

/**
 * The resting pose of a duct: how high it sits in its block, and how firmly it
 * holds that height. Across the block (X and Z) a duct always stays in the
 * middle. A pose packs into an int, {@link #CENTRED} being the middle with
 * nothing holding it. Sides are numbered like {@link DuctShapes}. Pure Java.
 */
public final class DuctPose {

    /** How high a duct sits in its block. */
    public enum Height {
        /** In the middle of the block. */
        MID,
        /** On the floor of the block. */
        LOW
    }

    /** How firmly a duct holds its height; a later constant beats an earlier one. */
    public enum Rank {
        /** Nothing holds it, so it sits in the middle. */
        FREE,
        /** Copied from the neighbour ducts that rest on something. */
        DRAPED,
        /** Rests on a floor, or a panel keeps it in the middle. */
        ANCHORED
    }

    public static final int X = 0;
    public static final int Y = 1;
    public static final int Z = 2;

    /** The middle of the block, free. */
    public static final int CENTRED = 0;

    private static final int RANK_SHIFT = 2;
    private static final Height[] HEIGHTS = Height.values();
    private static final Rank[] RANKS = Rank.values();

    private DuctPose() {
    }

    public static Height height(int pose) {
        return HEIGHTS[pose & 1];
    }

    public static Rank rank(int pose) {
        return RANKS[(pose >> RANK_SHIFT) & 3];
    }

    public static int of(Height height, Rank rank) {
        return height.ordinal() | rank.ordinal() << RANK_SHIFT;
    }

    /** The axis a side lies on: {@link #X}, {@link #Y} or {@link #Z}. */
    public static int axisOf(int side) {
        return switch (side) {
            case DuctShapes.DOWN, DuctShapes.UP -> Y;
            case DuctShapes.NORTH, DuctShapes.SOUTH -> Z;
            case DuctShapes.WEST, DuctShapes.EAST -> X;
            default -> throw new IllegalArgumentException("side " + side);
        };
    }

    /** Whether a side points toward the high end of its axis (up, south, east). */
    public static boolean isPositive(int side) {
        return (side & 1) == 1;
    }

    /** Whether a side is up or down. */
    public static boolean isVertical(int side) {
        return side == DuctShapes.DOWN || side == DuctShapes.UP;
    }

    private static boolean has(int mask, int side) {
        return (mask & 1 << side) != 0;
    }

    /**
     * The pose a duct takes from its own block and the block below. Any panel
     * keeps it in the middle; otherwise a sturdy block below that the duct
     * has no arm into is a floor it lies on, unless the duct has an arm up,
     * which keeps it in the middle so the run goes straight up.
     *
     * @param arms        six bit mask of the sides with an arm of any line
     * @param sturdyBelow whether the block below shows a full sturdy face toward the duct; ducts never count
     * @param panels      whether the duct carries any panel
     */
    public static int base(int arms, boolean sturdyBelow, boolean panels) {
        if (panels) {
            return of(Height.MID, Rank.ANCHORED);
        }
        if (sturdyBelow && !has(arms, DuctShapes.DOWN) && !has(arms, DuctShapes.UP)) {
            return of(Height.LOW, Rank.ANCHORED);
        }
        return CENTRED;
    }

    /**
     * The pose a duct shows: its base pose, or when that is free and the duct
     * has no arm up, the height its anchored neighbour ducts beside it ask
     * for, as long as they agree. Ducts above or below do not ask.
     *
     * @param base          the duct's own base pose
     * @param arms          six bit mask of the sides with an arm of any line
     * @param ductArms      six bit mask of the sides whose arm leads to another duct
     * @param neighbourBase base pose of the duct across each side, read only where {@code ductArms} has the side
     */
    public static int resting(int base, int arms, int ductArms, int[] neighbourBase) {
        if (rank(base) != Rank.FREE || has(arms, DuctShapes.UP)) {
            return base;
        }
        Height request = null;
        for (int side = 0; side < 6; side++) {
            if (!has(ductArms, side) || isVertical(side) || rank(neighbourBase[side]) != Rank.ANCHORED) {
                continue;
            }
            Height asked = height(neighbourBase[side]);
            if (request != null && request != asked) {
                return base;
            }
            request = asked;
        }
        return request == null ? base : of(request, Rank.DRAPED);
    }

    /**
     * The sides whose horizontal arm meets its face low, as a six bit mask.
     * Between two ducts the higher rank wins and a tie goes to the duct on the
     * negative side of the arm axis, so both ducts compute the same face. An
     * arm into an inventory or machine and a vertical arm meet their face in
     * the middle.
     *
     * @param resting          the duct's own resting pose
     * @param arms             six bit mask of the sides with an arm of any line
     * @param endpoints        six bit mask of the arms that end at an inventory or machine
     * @param neighbourResting resting pose of the duct across each side, read only for arms to ducts
     */
    public static int faces(int resting, int arms, int endpoints, int[] neighbourResting) {
        int faces = 0;
        for (int side = 0; side < 6; side++) {
            if (has(arms, side) && !has(endpoints, side) && !isVertical(side)
                    && meet(resting, neighbourResting[side], side) == Height.LOW) {
                faces |= 1 << side;
            }
        }
        return faces;
    }

    /** The height at which the arm on {@code side} meets its face. */
    public static Height faceHeight(int faces, int side) {
        return has(faces, side) ? Height.LOW : Height.MID;
    }

    private static Height meet(int own, int other, int side) {
        Rank ownRank = rank(own);
        Rank otherRank = rank(other);
        if (ownRank != otherRank) {
            return height(ownRank.compareTo(otherRank) > 0 ? own : other);
        }
        return height(isPositive(side) ? own : other);
    }
}
