package io.github.dirnei.mc.servus.shuttles.rail;

import io.github.dirnei.mc.servus.foundation.placement.PlacementAssist;
import io.github.dirnei.mc.servus.foundation.placement.PlacementAssists;
import io.github.dirnei.mc.servus.foundation.placement.QuadrantDirection;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Sneak placement for rails: the run is the rails in a straight line. On a
 * straight run of rails and elevator segments the walk keeps that axis;
 * otherwise the direction is the clicked side face or, on the top or bottom,
 * the edge nearest to the hit.
 */
public final class RailAssist implements PlacementAssist {

    public static final RailAssist INSTANCE = new RailAssist();

    private RailAssist() {
    }

    @Override
    public boolean isRun(Level level, BlockPos pos, ItemStack stack) {
        return level.getBlockState(pos).getBlock() instanceof RailBlock;
    }

    @Override
    public Direction direction(Level level, BlockHitResult hit, ItemStack stack) {
        BlockPos clicked = hit.getBlockPos();
        int connections = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            Block neighbour = level.getBlockState(clicked.relative(side)).getBlock();
            if (neighbour instanceof RailBlock || neighbour instanceof ElevatorBlock) {
                connections |= 1 << side.ordinal();
            }
        }
        Optional<Direction> along = PlacementAssists.alongRun(connections, hit);
        if (along.isPresent()) {
            return along.get();
        }
        Direction face = hit.getDirection();
        if (face.getAxis().isHorizontal()) {
            return face;
        }
        BlockPos pos = hit.getBlockPos();
        return switch (QuadrantDirection.nearestEdge(hit.getLocation().x - pos.getX(), hit.getLocation().z - pos.getZ())) {
            case NORTH -> Direction.NORTH;
            case EAST -> Direction.EAST;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
        };
    }
}
