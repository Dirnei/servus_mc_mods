package io.github.dirnei.mc.servus.shuttles.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Places block items in GameTests the way a right click with the item does:
 * a mock player looking a given way, optionally sneaking, uses the item on a
 * face of a block. Positions are relative to the test.
 */
final class Placing {

    private Placing() {
    }

    /** A survival mock player looking {@code looking}, sneaking or not, holding {@code stack}. */
    static Player player(GameTestHelper helper, Direction looking, boolean sneaking, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(looking.toYRot());
        player.setShiftKeyDown(sneaking);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    /** Uses {@code stack} on the centre of {@code face} of the block at {@code clicked}. */
    static InteractionResult use(GameTestHelper helper, ItemStack stack, BlockPos clicked, Direction face, Direction looking, boolean sneaking) {
        return use(helper, player(helper, looking, sneaking, stack), clicked, face, new Vec3(0.5, 0.5, 0.5).relative(face, 0.5));
    }

    /** Uses the held item on the top of the block at {@code below}, placing on the spot above it. */
    static InteractionResult onTop(GameTestHelper helper, ItemStack stack, BlockPos below, Direction looking, boolean sneaking) {
        return use(helper, stack, below, Direction.UP, looking, sneaking);
    }

    /**
     * The player uses its held item on {@code face} of the block at {@code clicked}, hitting it at
     * {@code inBlock} (0 to 1 on each axis from the block's lower corner).
     */
    static InteractionResult use(GameTestHelper helper, Player player, BlockPos clicked, Direction face, Vec3 inBlock) {
        BlockPos absolute = helper.absolutePos(clicked);
        Vec3 hit = Vec3.atLowerCornerOf(absolute).add(inBlock);
        ItemStack stack = player.getMainHandItem();
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, absolute, false)));
    }
}
