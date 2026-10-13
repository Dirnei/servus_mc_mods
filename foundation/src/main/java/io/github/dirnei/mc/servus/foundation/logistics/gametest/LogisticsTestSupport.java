package io.github.dirnei.mc.servus.foundation.logistics.gametest;

import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Shared setup and clicks of the logistics GameTests. */
final class LogisticsTestSupport {

    private LogisticsTestSupport() {
    }

    /**
     * Places a Utility Duct, lets {@code setup} add its lines, then refreshes the
     * connection flags from the neighbours, which {@code setBlock} skips.
     */
    static UtilityDuctBlockEntity duct(GameTestHelper helper, BlockPos pos, Consumer<UtilityDuctBlockEntity> setup) {
        helper.setBlock(pos, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos, UtilityDuctBlockEntity.class);
        setup.accept(duct);
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
        return duct;
    }

    /** Uses {@code stack} in the player's main hand on the centre of a face of the block at {@code pos}, like a right click. */
    static InteractionResult useOn(GameTestHelper helper, Player player, ItemStack stack, BlockPos pos, Direction face) {
        BlockPos absolute = helper.absolutePos(pos);
        Vec3 point = Vec3.atCenterOf(absolute).relative(face, 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(point, face, absolute, false)));
    }
}
