package io.github.dirnei.mc.servus.foundation.network;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Converts between the pure graph types and their Minecraft counterparts. */
public final class Conversions {

    private static final Side[] SIDES = Side.values();
    private static final Direction[] DIRECTIONS = Direction.values();

    private Conversions() {
    }

    public static NodePos toNodePos(BlockPos pos) {
        return new NodePos(pos.getX(), pos.getY(), pos.getZ());
    }

    public static BlockPos toBlockPos(NodePos pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }

    /** Relies on both enums listing DOWN, UP, NORTH, SOUTH, WEST, EAST in that order. */
    public static Side toSide(Direction direction) {
        return SIDES[direction.ordinal()];
    }

    public static Direction toDirection(Side side) {
        return DIRECTIONS[side.ordinal()];
    }
}
