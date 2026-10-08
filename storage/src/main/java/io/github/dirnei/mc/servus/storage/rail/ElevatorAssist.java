package io.github.dirnei.mc.servus.storage.rail;

import io.github.dirnei.mc.servus.lib.placement.PlacementAssist;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Sneak placement for elevators: a click on any face of a segment walks up
 * the shaft. Segments need support from below, so up is the only way to build.
 */
public final class ElevatorAssist implements PlacementAssist {

    public static final ElevatorAssist INSTANCE = new ElevatorAssist();

    private ElevatorAssist() {
    }

    @Override
    public boolean isRun(Level level, BlockPos pos, ItemStack stack) {
        return level.getBlockState(pos).getBlock() instanceof ElevatorBlock;
    }

    @Override
    public Direction direction(Level level, BlockHitResult hit, ItemStack stack) {
        return Direction.UP;
    }
}
