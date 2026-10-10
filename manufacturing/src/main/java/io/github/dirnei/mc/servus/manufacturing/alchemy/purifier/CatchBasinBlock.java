package io.github.dirnei.mc.servus.manufacturing.alchemy.purifier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** The Catch Basin below one of the Purifier's planks: the brine runs into it, and the Salt is taken from it. */
public class CatchBasinBlock extends PurifierSideBlock {

    public CatchBasinBlock(Properties properties) {
        super(properties);
    }

    /** An empty bucket fills with Salt at a formed Basin. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!state.getValue(FORMED) || stack.getCapability(Capabilities.FluidHandler.ITEM) == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        IFluidHandler salt = PurifierStructure.basinHandler(level, pos, state);
        if (salt != null && !level.isClientSide()) {
            FluidUtil.interactWithFluidHandler(player, hand, salt);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
