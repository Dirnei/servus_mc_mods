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
        /** The end of a climb, copied from a draped neighbour duct beside it; no duct copies this. */
        FOLLOWED,
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

    private static final int HORIZONTAL = 1 << DuctShapes.NORTH | 1 << DuctShapes.SOUTH
            | 1 << DuctShapes.WEST | 1 << DuctShapes.EAST;
    private static final int VERTICAL = 1 << DuctShapes.DOWN | 1 << DuctShapes.UP;
    private static final int NO_REQUEST = -1;
    private static final int DISAGREE = -2;
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
     * Whether the duct has an arm up and a horizontal arm into an inventory
     * or machine. Such a duct stays in the middle, so the run goes straight
     * up from the connection instead of lying down first.
     *
     * @param arms      six bit mask of the sides with an arm of any line
     * @param endpoints six bit mask of the arms that end at an inventory or machine
     */
    public static boolean risesFromEndpoint(int arms, int endpoints) {
        return has(arms, DuctShapes.UP) && (endpoints & HORIZONTAL) != 0;
    }

    /**
     * The pose a duct takes from its own block and the block below. Any panel
     * keeps it in the middle; otherwise a sturdy block below that the duct
     * has no arm into is a floor it lies on, unless the duct
     * {@linkplain #risesFromEndpoint rises from an endpoint}.
     *
     * @param arms        six bit mask of the sides with an arm of any line
     * @param endpoints   six bit mask of the arms that end at an inventory or machine
     * @param sturdyBelow whether the block below shows a full sturdy face toward the duct; ducts never count
     * @param panels      whether the duct carries any panel
     */
    public static int base(int arms, int endpoints, boolean sturdyBelow, boolean panels) {
        if (panels) {
            return of(Height.MID, Rank.ANCHORED);
        }
        if (sturdyBelow && !has(arms, DuctShapes.DOWN) && !risesFromEndpoint(arms, endpoints)) {
            return of(Height.LOW, Rank.ANCHORED);
        }
        return CENTRED;
    }

    /**
     * The pose a duct shows: its base pose, or when that is free and the duct
     * does not {@linkplain #risesFromEndpoint rise from an endpoint}, the
     * height its anchored neighbour ducts beside it ask for, as long as they
     * agree. Without such a neighbour, a duct at the end of a climb (an arm up
     * or down to a duct) follows its draped neighbours beside it the same way,
     * so the climb meets the run at the run's height. Ducts above or below do
     * not ask.
     *
     * @param base             the duct's own base pose
     * @param arms             six bit mask of the sides with an arm of any line
     * @param ductArms         six bit mask of the sides whose arm leads to another duct; the other arms are endpoints
     * @param neighbourBase    base pose of the duct across each side, read only where {@code ductArms} has the side
     * @param neighbourResting resting pose of the duct across each side, read only where {@code ductArms} has the side
     */
    public static int resting(int base, int arms, int ductArms, int[] neighbourBase, int[] neighbourResting) {
        if (rank(base) != Rank.FREE || risesFromEndpoint(arms, arms & ~ductArms)) {
            return base;
        }
        int anchored = request(ductArms, neighbourBase, Rank.ANCHORED);
        if (anchored != NO_REQUEST) {
            return anchored == DISAGREE ? base : of(HEIGHTS[anchored], Rank.DRAPED);
        }
        if ((ductArms & VERTICAL) == 0) {
            return base;
        }
        int draped = request(ductArms, neighbourResting, Rank.DRAPED);
        return draped >= 0 ? of(HEIGHTS[draped], Rank.FOLLOWED) : base;
    }

    /**
     * The height the horizontal neighbour ducts of that rank ask for, as a
     * {@link Height} ordinal, or {@link #NO_REQUEST} or {@link #DISAGREE}.
     */
    private static int request(int ductArms, int[] neighbourPoses, Rank rank) {
        int request = NO_REQUEST;
        for (int side = 0; side < 6; side++) {
            if (!has(ductArms, side) || isVertical(side) || rank(neighbourPoses[side]) != rank) {
                continue;
            }
            int asked = height(neighbourPoses[side]).ordinal();
            if (request != NO_REQUEST && request != asked) {
                return DISAGREE;
            }
            request = asked;
        }
        return request;
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
