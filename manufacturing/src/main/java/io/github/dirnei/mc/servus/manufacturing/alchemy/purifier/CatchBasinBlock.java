package io.github.dirnei.mc.servus.manufacturing.alchemy.purifier;

import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/** The Catch Basin beside the Purifier's log: the brine runs into it, and the Salt is taken from it. */
public class CatchBasinBlock extends PurifierSideBlock {

    public CatchBasinBlock(Properties properties) {
        super(properties);
    }

    /** An empty bucket fills with Salt at a formed Basin. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!state.getValue(FORMED) || !Transfer.isFluidContainer(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        LegacyFluidHandler salt = PurifierStructure.basinHandler(level, pos, state);
        if (salt != null && !level.isClientSide()) {
            FluidUtil.interactWithFluidHandler(player, hand, pos, salt, null);
        }
        return InteractionResult.SUCCESS;
    }
}
