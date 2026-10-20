package io.github.dirnei.mc.servus.delicacies.gametest;

import io.github.dirnei.mc.servus.delicacies.board.CuttingBoardBlockEntity;
import io.github.dirnei.mc.servus.delicacies.board.CuttingInput;
import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesRecipes;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/** The Cutting Board cuts one item per knife click, takes only cuttable items, and is fed by hoppers. */
public final class CuttingBoardGameTests {

    private static final BlockPos BOARD = new BlockPos(2, 1, 2);

    private CuttingBoardGameTests() {
    }

    private static CuttingBoardBlockEntity board(GameTestHelper helper) {
        helper.setBlock(BOARD, ServusDelicaciesBlocks.CUTTING_BOARD.get());
        return helper.getBlockEntity(BOARD, CuttingBoardBlockEntity.class);
    }

    private static ItemStack onions(int count) {
        return new ItemStack(ServusDelicaciesItems.harvest(CropKind.ONION), count);
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void knifeCutsOneItemPerClick(GameTestHelper helper) {
        CuttingBoardBlockEntity board = board(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        KitchenTestSupport.click(helper, player, BOARD, onions(3));
        helper.assertTrue(board.stack(CuttingBoardBlockEntity.SLOT_ITEM).getCount() == 3, "three Onions lie on the board");
        ItemStack knife = new ItemStack(ServusDelicaciesItems.KITCHEN_KNIFE.get());
        KitchenTestSupport.click(helper, player, BOARD, knife);
        helper.assertTrue(board.stack(CuttingBoardBlockEntity.SLOT_ITEM).getCount() == 2, "one Onion is cut");
        ItemStack pieces = board.stack(CuttingBoardBlockEntity.SLOT_PIECES);
        helper.assertTrue(pieces.is(ServusDelicaciesItems.CHOPPED_ONION.get()) && pieces.getCount() == 2, "two Chopped Onion, got " + pieces);
        helper.assertTrue(knife.getDamageValue() == 1, "the knife wore by one");
        KitchenTestSupport.click(helper, player, BOARD, ItemStack.EMPTY);
        helper.assertTrue(player.getInventory().countItem(ServusDelicaciesItems.CHOPPED_ONION.get()) == 2, "an empty hand takes the pieces");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void cabbageIsShredded(GameTestHelper helper) {
        CuttingBoardBlockEntity board = board(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        KitchenTestSupport.click(helper, player, BOARD, new ItemStack(ServusDelicaciesItems.harvest(CropKind.CABBAGE)));
        KitchenTestSupport.click(helper, player, BOARD, new ItemStack(ServusDelicaciesItems.KITCHEN_KNIFE.get()));
        helper.assertTrue(board.stack(CuttingBoardBlockEntity.SLOT_PIECES).is(ServusDelicaciesItems.SHREDDED_CABBAGE.get()),
                "the Cabbage is shredded");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void noCutWithoutAKnife(GameTestHelper helper) {
        CuttingBoardBlockEntity board = board(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        KitchenTestSupport.click(helper, player, BOARD, onions(1));
        KitchenTestSupport.click(helper, player, BOARD, new ItemStack(Items.STICK));
        helper.assertTrue(board.stack(CuttingBoardBlockEntity.SLOT_ITEM).getCount() == 1
                && board.stack(CuttingBoardBlockEntity.SLOT_PIECES).isEmpty(), "a stick cuts nothing");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void cobblestoneStaysInTheHand(GameTestHelper helper) {
        CuttingBoardBlockEntity board = board(helper);
        ItemStack cobblestone = new ItemStack(Items.COBBLESTONE);
        KitchenTestSupport.click(helper, helper.makeMockPlayer(GameType.SURVIVAL), BOARD, cobblestone);
        helper.assertTrue(cobblestone.getCount() == 1 && board.stack(CuttingBoardBlockEntity.SLOT_ITEM).isEmpty(), "Cobblestone is refused");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 100)
    public static void hopperFillsTheBoard(GameTestHelper helper) {
        CuttingBoardBlockEntity board = board(helper);
        BlockPos hopper = BOARD.above();
        helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        helper.getBlockEntity(hopper, HopperBlockEntity.class).setItem(0, onions(2));
        helper.succeedWhen(() -> helper.assertTrue(board.stack(CuttingBoardBlockEntity.SLOT_ITEM).getCount() == 2,
                "the Onions wait on the board"));
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void cuttingRecipesShipped(GameTestHelper helper) {
        ItemStack knife = new ItemStack(ServusDelicaciesItems.KITCHEN_KNIFE.get());
        helper.assertTrue(ServusRecipes.first(helper.getLevel(), ServusDelicaciesRecipes.CUTTING.get(),
                new CuttingInput(onions(1), knife)).isPresent(), "Onion has a cutting recipe");
        helper.assertTrue(ServusRecipes.first(helper.getLevel(), ServusDelicaciesRecipes.CUTTING.get(),
                new CuttingInput(new ItemStack(ServusDelicaciesItems.harvest(CropKind.CABBAGE)), knife)).isPresent(),
                "Cabbage has a cutting recipe");
        helper.succeed();
    }
}
