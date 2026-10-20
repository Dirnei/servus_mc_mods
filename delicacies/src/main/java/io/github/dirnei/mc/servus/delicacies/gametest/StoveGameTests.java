package io.github.dirnei.mc.servus.delicacies.gametest;

import static io.github.dirnei.mc.servus.delicacies.gametest.KitchenTestSupport.DOOR;

import io.github.dirnei.mc.servus.delicacies.firebox.DelicaciesCapabilities;
import io.github.dirnei.mc.servus.delicacies.stove.StoveTileBlock;
import io.github.dirnei.mc.servus.delicacies.stove.StoveTileBlockEntity;
import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

/** The Tiled Stove forms from a row of tiles with the carrot, unforms when a tile goes, and burns fuel only on demand. */
public final class StoveGameTests {

    private StoveGameTests() {
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void carrotFormsTwoTiles(GameTestHelper helper) {
        KitchenTestSupport.tiles(helper, DOOR, 2);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GameTestSteps.useCarrot(helper, player, DOOR, Direction.NORTH);
        BlockState door = helper.getBlockState(DOOR);
        BlockState other = helper.getBlockState(DOOR.east());
        helper.assertTrue(door.getValue(StoveTileBlock.FORMED) && door.getValue(StoveTileBlock.DOOR)
                && door.getValue(StoveTileBlock.FACING) == Direction.NORTH, "the clicked tile has the door on the clicked face");
        helper.assertTrue(other.getValue(StoveTileBlock.FORMED) && !other.getValue(StoveTileBlock.DOOR), "the other tile is a plate");
        helper.assertTrue(helper.getLevel().getCapability(DelicaciesCapabilities.FIREBOX, helper.absolutePos(DOOR.east()), null) != null,
                "every tile offers the firebox");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void clickOnTopPutsTheDoorTowardThePlayer(GameTestHelper helper) {
        KitchenTestSupport.tiles(helper, DOOR.west(), 3);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0);
        GameTestSteps.useCarrot(helper, player, DOOR, Direction.UP);
        BlockState door = helper.getBlockState(DOOR);
        helper.assertTrue(door.getValue(StoveTileBlock.DOOR) && door.getValue(StoveTileBlock.FACING) == Direction.NORTH,
                "a player looking south gets the door on the north face, got " + door.getValue(StoveTileBlock.FACING));
        helper.assertTrue(helper.getBlockState(DOOR.west()).getValue(StoveTileBlock.FORMED)
                && helper.getBlockState(DOOR.east()).getValue(StoveTileBlock.FORMED), "all three tiles formed");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void singleTileIsRefused(GameTestHelper helper) {
        KitchenTestSupport.tiles(helper, DOOR, 1);
        GameTestSteps.useCarrot(helper, helper.makeMockPlayer(GameType.SURVIVAL), DOOR, Direction.NORTH);
        helper.assertFalse(helper.getBlockState(DOOR).getValue(StoveTileBlock.FORMED), "one tile does not form");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void tooLongRowIsRefused(GameTestHelper helper) {
        KitchenTestSupport.tiles(helper, DOOR, 5);
        GameTestSteps.useCarrot(helper, helper.makeMockPlayer(GameType.SURVIVAL), DOOR, Direction.NORTH);
        for (int i = 0; i < 5; i++) {
            helper.assertFalse(helper.getBlockState(DOOR.east(i)).getValue(StoveTileBlock.FORMED), "five tiles do not form");
        }
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 40)
    public static void breakingATileUnformsAndDropsFuel(GameTestHelper helper) {
        KitchenTestSupport.fuelledStove(helper, 3, new ItemStack(Items.COAL, 5));
        helper.destroyBlock(DOOR.east());
        helper.succeedWhen(() -> {
            helper.assertFalse(helper.getBlockState(DOOR).getValue(StoveTileBlock.FORMED), "the door tile is loose");
            helper.assertFalse(helper.getBlockState(DOOR.east(2)).getValue(StoveTileBlock.FORMED), "the far tile is loose");
            int coal = GameTestSteps.drops(helper, DOOR, 3).stream().map(ItemEntity::getItem)
                    .filter(stack -> stack.is(Items.COAL)).mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(coal == 5, "the fuel dropped, found " + coal);
        });
    }

    @ServusGameTest(timeoutTicks = 100)
    public static void hopperFeedsTheFirebox(GameTestHelper helper) {
        StoveTileBlockEntity master = KitchenTestSupport.stove(helper, 2);
        BlockPos above = DOOR.east().above();
        helper.setBlock(above, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        helper.getBlockEntity(above, HopperBlockEntity.class).setItem(0, new ItemStack(Items.CHARCOAL, 2));
        helper.succeedWhen(() -> helper.assertTrue(master.fuelStack().is(Items.CHARCOAL) && master.fuelStack().getCount() == 2,
                "the Charcoal moved into the firebox"));
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void fireboxRefusesWhatDoesNotBurn(GameTestHelper helper) {
        KitchenTestSupport.stove(helper, 2);
        ItemView fuel = ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(DOOR), Direction.NORTH));
        helper.assertTrue(fuel != null, "a formed tile takes fuel");
        helper.assertTrue(fuel.insertItem(new ItemStack(Items.COBBLESTONE), false).getCount() == 1, "Cobblestone is refused");
        helper.assertTrue(fuel.insertItem(new ItemStack(Items.COAL), false).isEmpty(), "Coal goes in");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 80)
    public static void noFuelBurnsWithoutSomethingToCook(GameTestHelper helper) {
        StoveTileBlockEntity master = KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 3));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(master.fuelStack().getCount() == 3, "no Coal was taken");
            helper.assertFalse(helper.getBlockState(DOOR).getValue(StoveTileBlock.LIT), "the fire stays out");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void looseTileOffersNoFirebox(GameTestHelper helper) {
        KitchenTestSupport.tiles(helper, DOOR, 1);
        helper.assertTrue(helper.getLevel().getCapability(DelicaciesCapabilities.FIREBOX, helper.absolutePos(DOOR), null) == null,
                "a loose tile heats nothing");
        helper.succeed();
    }
}
