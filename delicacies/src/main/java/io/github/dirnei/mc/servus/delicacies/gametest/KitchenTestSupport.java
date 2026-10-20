package io.github.dirnei.mc.servus.delicacies.gametest;

import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.stove.StoveRow;
import io.github.dirnei.mc.servus.delicacies.stove.StoveTileBlock;
import io.github.dirnei.mc.servus.delicacies.stove.StoveTileBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Steps the kitchen GameTests share: building a stove and clicking kitchen blocks. */
final class KitchenTestSupport {

    /** Where the door tile of a test stove stands; the row runs east from it. */
    static final BlockPos DOOR = new BlockPos(2, 1, 2);

    private KitchenTestSupport() {
    }

    /** Places {@code length} loose Stove Tiles east of and including {@code start}. */
    static void tiles(GameTestHelper helper, BlockPos start, int length) {
        for (int i = 0; i < length; i++) {
            helper.setBlock(start.east(i), ServusDelicaciesBlocks.STOVE_TILE.get());
        }
    }

    /** A formed stove of {@code length} tiles from {@link #DOOR} east, door facing north; returns its master tile. */
    static StoveTileBlockEntity stove(GameTestHelper helper, int length) {
        tiles(helper, DOOR, length);
        StoveRow.Result result = StoveTileBlock.tryForm(helper.getLevel(), helper.absolutePos(DOOR), Direction.NORTH);
        helper.assertTrue(result.formed(), "the test stove forms, got " + result.outcome());
        return tile(helper, DOOR);
    }

    /** A formed stove with {@code fuel} in its firebox. */
    static StoveTileBlockEntity fuelledStove(GameTestHelper helper, int length, ItemStack fuel) {
        StoveTileBlockEntity master = stove(helper, length);
        ItemStack left = master.addFuel(fuel);
        helper.assertTrue(left.isEmpty(), "the fuel goes in");
        return master;
    }

    static StoveTileBlockEntity tile(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, StoveTileBlockEntity.class);
    }

    /**
     * A right click on the top of the block at {@code pos} holding {@code held} in the main hand, the way the server
     * handles it: the block's item use first, then the empty hand use when the hand is empty.
     */
    static InteractionResult click(GameTestHelper helper, Player player, BlockPos pos, ItemStack held) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        BlockState state = helper.getLevel().getBlockState(absolute);
        if (held.isEmpty()) {
            return state.useWithoutItem(helper.getLevel(), player, hit);
        }
        return state.useItemOn(held, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    }
}
