package io.github.dirnei.mc.servus.storage.rail;

import io.github.dirnei.mc.servus.lib.placement.PlacementAssist;
import io.github.dirnei.mc.servus.lib.placement.QuadrantDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Sneak placement for rails: the run is the rails in a straight line, the
 * direction is the clicked side face or, on the top or bottom, the edge
 * nearest to the hit.
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
    public Direction direction(BlockHitResult hit) {
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
