package io.github.dirnei.mc.servus.foundation.data;

import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.placement.PlacementAssist;
import io.github.dirnei.mc.servus.foundation.placement.PlacementAssists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Sneak placement for Data Cables: any cable starts and continues the run. On
 * a straight run the walk keeps its axis; otherwise it follows the clicked face.
 * Arms toward members do not count as the run.
 */
public final class DataCableAssist implements PlacementAssist {

    @Override
    public boolean isRun(Level level, BlockPos pos, ItemStack stack) {
        return level.getBlockState(pos).getBlock() instanceof DataCableBlock;
    }

    @Override
    public Direction direction(Level level, BlockHitResult hit, ItemStack stack) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        int connections = 0;
        if (state.getBlock() instanceof DataCableBlock) {
            for (Direction side : Direction.values()) {
                if (state.getValue(ConnectableBlock.property(side))
                        && level.getBlockState(pos.relative(side)).getBlock() instanceof DataCableBlock) {
                    connections |= 1 << side.ordinal();
                }
            }
        }
        return PlacementAssists.alongRun(connections, hit).orElse(hit.getDirection());
    }
}
