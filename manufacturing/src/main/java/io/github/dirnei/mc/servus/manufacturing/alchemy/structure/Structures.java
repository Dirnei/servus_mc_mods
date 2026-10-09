package io.github.dirnei.mc.servus.manufacturing.alchemy.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** The bridge from the pure build geometry to world positions and directions. */
public final class Structures {

    private Structures() {
    }

    public static Facing facing(Direction direction) {
        return switch (direction) {
            case EAST -> Facing.EAST;
            case SOUTH -> Facing.SOUTH;
            case WEST -> Facing.WEST;
            default -> Facing.NORTH;
        };
    }

    public static Direction direction(Facing facing) {
        return switch (facing) {
            case NORTH -> Direction.NORTH;
            case EAST -> Direction.EAST;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
        };
    }

    /** The position {@code right} and {@code up} from {@code origin} for a front facing {@code facing}. */
    public static BlockPos pos(BlockPos origin, int right, int up, Facing facing) {
        return origin.offset(right * facing.rightX(), up, right * facing.rightZ());
    }

    public static BlockPos pos(BlockPos origin, Offset offset) {
        return origin.offset(offset.x(), offset.y(), offset.z());
    }
}
