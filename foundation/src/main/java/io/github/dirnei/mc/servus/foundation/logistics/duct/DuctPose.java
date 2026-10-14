package io.github.dirnei.mc.servus.foundation.logistics.duct;

/**
 * The resting pose of a duct: per axis, whether it sits flush with the low
 * side, in the middle or flush with the high side of its block, and how firmly
 * it holds that position. A pose packs into an int, {@link #CENTRED} being the
 * middle on every axis with nothing holding it. Axes are {@link #X},
 * {@link #Y}, {@link #Z}; sides are numbered like {@link DuctShapes}. Pure Java.
 */
public final class DuctPose {

    /** Where a duct sits on one axis. */
    public enum Side {
        MID, NEG, POS
    }

    /** How firmly a duct holds its side on one axis; a later constant beats an earlier one. */
    public enum Rank {
        /** Nothing holds it, so it sits in the middle. */
        FREE,
        /** Copied from the neighbour ducts that rest on something. */
        DRAPED,
        /** Rests on a floor or wall, or a panel keeps it in the middle. */
        ANCHORED
    }

    public static final int X = 0;
    public static final int Y = 1;
    public static final int Z = 2;

    /** The middle of the block on every axis, free. */
    public static final int CENTRED = 0;

    private static final int RANK_SHIFT = 6;
    private static final Side[] SIDES = Side.values();
    private static final Rank[] RANKS = Rank.values();

    private DuctPose() {
    }

    public static Side side(int pose, int axis) {
        return SIDES[(pose >> 2 * axis) & 3];
    }

    public static Rank rank(int pose, int axis) {
        return RANKS[(pose >> RANK_SHIFT + 2 * axis) & 3];
    }

    /** The pose with one axis replaced. */
    public static int with(int pose, int axis, Side side, Rank rank) {
        int cleared = pose & ~(3 << 2 * axis) & ~(3 << RANK_SHIFT + 2 * axis);
        return cleared | side.ordinal() << 2 * axis | rank.ordinal() << RANK_SHIFT + 2 * axis;
    }

    /** The pose without its ranks: only what decides where the parts are. */
    public static int sides(int pose) {
        return pose & (1 << RANK_SHIFT) - 1;
    }

    /** The axis a side lies on. */
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

    private static int negativeSide(int axis) {
        return switch (axis) {
            case X -> DuctShapes.WEST;
            case Y -> DuctShapes.DOWN;
            case Z -> DuctShapes.NORTH;
            default -> throw new IllegalArgumentException("axis " + axis);
        };
    }

    private static boolean has(int mask, int side) {
        return (mask & 1 << side) != 0;
    }

    /**
     * The pose a duct takes from its own block and the six blocks around it.
     * A sturdy block below lies under it as floor. With an arm up or down, a
     * sturdy block beside it is a wall it moves against; walls on both sides
     * of an axis keep it in the middle. A side the duct has an arm into is
     * neither floor nor wall. Any panel keeps it in the middle on every axis.
     *
     * @param arms   six bit mask of the sides with an arm of any line
     * @param sturdy six bit mask of the sides whose neighbour shows a full sturdy face toward the duct; ducts never count
     * @param panels whether the duct carries any panel
     */
    public static int base(int arms, int sturdy, boolean panels) {
        if (panels) {
            int pose = CENTRED;
            for (int axis = X; axis <= Z; axis++) {
                pose = with(pose, axis, Side.MID, Rank.ANCHORED);
            }
            return pose;
        }
        int support = sturdy & ~arms;
        int pose = CENTRED;
        if (has(support, DuctShapes.DOWN)) {
            pose = with(pose, Y, Side.NEG, Rank.ANCHORED);
        }
        if (has(arms, DuctShapes.DOWN) || has(arms, DuctShapes.UP)) {
            pose = wall(pose, X, support);
            pose = wall(pose, Z, support);
        }
        return pose;
    }

    /**
     * The pose a duct shows: its base pose, where each free axis takes the
     * side that its neighbour ducts anchored on that axis ask for, as long as
     * they agree. A neighbour across a face on that axis does not ask.
     *
     * @param base          the duct's own base pose
     * @param ductArms      six bit mask of the sides whose arm leads to another duct
     * @param neighbourBase base pose of the duct across each side, read only where {@code ductArms} has the side
     */
    public static int resting(int base, int ductArms, int[] neighbourBase) {
        int pose = base;
        for (int axis = X; axis <= Z; axis++) {
            if (rank(base, axis) != Rank.FREE) {
                continue;
            }
            Side request = agreedRequest(axis, ductArms, neighbourBase);
            if (request != null) {
                pose = with(pose, axis, request, Rank.DRAPED);
            }
        }
        return pose;
    }

    /** The side every anchored neighbour asks for on the axis, null without requests or when they disagree. */
    private static Side agreedRequest(int axis, int ductArms, int[] neighbourBase) {
        Side request = null;
        for (int side = 0; side < 6; side++) {
            if (!has(ductArms, side) || axisOf(side) == axis || rank(neighbourBase[side], axis) != Rank.ANCHORED) {
                continue;
            }
            Side asked = side(neighbourBase[side], axis);
            if (request != null && request != asked) {
                return null;
            }
            request = asked;
        }
        return request;
    }

    private static int wall(int pose, int axis, int support) {
        int negative = negativeSide(axis);
        boolean low = has(support, negative);
        boolean high = has(support, negative + 1);
        if (low && high) {
            return with(pose, axis, Side.MID, Rank.ANCHORED);
        }
        if (low) {
            return with(pose, axis, Side.NEG, Rank.ANCHORED);
        }
        return high ? with(pose, axis, Side.POS, Rank.ANCHORED) : pose;
    }
}
