package io.github.dirnei.mc.servus.lib.side;

import io.github.dirnei.mc.servus.lib.graph.Side;

/**
 * A side relative to a block's horizontal facing. Left and right are the
 * block's own left and right (facing north, left is west).
 */
public enum RelativeSide {
    FRONT,
    BACK,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM;

    private static final RelativeSide[] VALUES = values();

    public static RelativeSide byOrdinal(int ordinal) {
        return VALUES[Math.floorMod(ordinal, VALUES.length)];
    }

    /** The absolute side this relative side points to for a block facing {@code facing}. */
    public Side toSide(Side facing) {
        return switch (this) {
            case FRONT -> facing;
            case BACK -> facing.opposite();
            case LEFT -> counterClockwise(facing);
            case RIGHT -> clockwise(facing);
            case TOP -> Side.UP;
            case BOTTOM -> Side.DOWN;
        };
    }

    /** The relative side that points to {@code absolute} for a block facing {@code facing}. */
    public static RelativeSide fromSide(Side facing, Side absolute) {
        for (RelativeSide relative : VALUES) {
            if (relative.toSide(facing) == absolute) {
                return relative;
            }
        }
        throw new IllegalArgumentException("No relative side for " + absolute + " with facing " + facing);
    }

    static Side clockwise(Side side) {
        return switch (side) {
            case NORTH -> Side.EAST;
            case EAST -> Side.SOUTH;
            case SOUTH -> Side.WEST;
            case WEST -> Side.NORTH;
            default -> side;
        };
    }

    static Side counterClockwise(Side side) {
        return switch (side) {
            case NORTH -> Side.WEST;
            case WEST -> Side.SOUTH;
            case SOUTH -> Side.EAST;
            case EAST -> Side.NORTH;
            default -> side;
        };
    }
}
