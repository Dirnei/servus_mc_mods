package io.github.dirnei.mc.servus.foundation.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Implemented by blocks that know what a wrench means to them. The wrench
 * hands the click over instead of rotating the block.
 */
public interface Wrenchable {

    /**
     * Called on both logical sides when the wrench is used on this block.
     * Check {@code context.getLevel().isClientSide()} and
     * {@code context.getPlayer().isShiftKeyDown()} as needed.
     */
    InteractionResult onWrench(UseOnContext context, BlockState state);
}
