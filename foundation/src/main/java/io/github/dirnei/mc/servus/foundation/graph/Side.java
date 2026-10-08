package io.github.dirnei.mc.servus.foundation.graph;

import java.util.List;

/**
 * One of the six block sides. The order matches Minecraft's Direction enum so
 * the two can be mapped by ordinal, but this type has no Minecraft dependency.
 */
public enum Side {
    DOWN(0, -1, 0),
    UP(0, 1, 0),
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    EAST(1, 0, 0);

    public static final List<Side> ALL = List.of(values());

    private final int dx;
    private final int dy;
    private final int dz;

    Side(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }

    public int dz() {
        return dz;
    }

    public Side opposite() {
        return switch (this) {
            case DOWN -> UP;
            case UP -> DOWN;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case EAST -> WEST;
        };
    }
}
