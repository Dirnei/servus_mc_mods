package io.github.dirnei.mc.servus.lib.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * What a sneak click with an assisted item walks along: which blocks start a
 * walk, which blocks continue the run and which way the walk goes.
 */
public interface PlacementAssist {

    /** Whether a sneak click on the block at {@code pos} starts a walk. */
    default boolean isStart(Level level, BlockPos pos, ItemStack stack) {
        return isRun(level, pos, stack);
    }

    /** Whether the block at {@code pos} continues the run of this item. */
    boolean isRun(Level level, BlockPos pos, ItemStack stack);

    /** The walk direction for a click on a block that starts a walk. */
    Direction direction(BlockHitResult hit);
}
