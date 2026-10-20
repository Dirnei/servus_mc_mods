package io.github.dirnei.mc.servus.delicacies.gametest;

import static io.github.dirnei.mc.servus.delicacies.gametest.KitchenTestSupport.DOOR;

import io.github.dirnei.mc.servus.delicacies.cooking.PanBlockEntity;
import io.github.dirnei.mc.servus.delicacies.cooking.PotBlockEntity;
import io.github.dirnei.mc.servus.delicacies.cooking.VesselBlock;
import io.github.dirnei.mc.servus.delicacies.cooking.VesselBlockEntity;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.delicacies.stove.StoveTileBlock;
import io.github.dirnei.mc.servus.delicacies.stove.StoveTileBlockEntity;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/** Pot and Pan cook over a burning stove, only there, and work by hand and through capabilities; the foods are eaten from data. */
public final class CookingGameTests {

    private static final BlockPos ON_DOOR = DOOR.above();
    private static final BlockPos ON_PLATE = DOOR.east().above();

    private CookingGameTests() {
    }

    private static PotBlockEntity pot(GameTestHelper helper, BlockPos pos, int potatoes, int water) {
        helper.setBlock(pos, ServusDelicaciesBlocks.POT.get());
        PotBlockEntity pot = helper.getBlockEntity(pos, PotBlockEntity.class);
        if (potatoes > 0) {
            pot.insertInputs(new ItemStack(Items.POTATO, potatoes));
        }
        if (water > 0) {
            fluid(helper, pos).fill(new FluidStack(Fluids.WATER, water), false);
        }
        return pot;
    }

    private static PanBlockEntity pan(GameTestHelper helper, BlockPos pos, ItemStack... inputs) {
        helper.setBlock(pos, ServusDelicaciesBlocks.PAN.get());
        PanBlockEntity pan = helper.getBlockEntity(pos, PanBlockEntity.class);
        for (ItemStack input : inputs) {
            helper.assertTrue(pan.insertInputs(input).isEmpty(), "the Pan takes " + input);
        }
        return pan;
    }

    private static FluidView fluid(GameTestHelper helper, BlockPos pos) {
        return FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(pos), Direction.UP));
    }

    private static ItemView items(GameTestHelper helper, BlockPos pos, Direction side) {
        return ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(pos), side));
    }

    @ServusGameTest(timeoutTicks = 300)
    public static void potCooksBoiledPotatoesOverCoal(GameTestHelper helper) {
        StoveTileBlockEntity master = KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 2));
        PotBlockEntity pot = pot(helper, ON_DOOR, 2, 1000);
        helper.succeedWhen(() -> {
            helper.assertTrue(pot.output().is(ServusDelicaciesItems.BOILED_POTATOES.get()) && pot.output().getCount() == 2,
                    "two Boiled Potatoes, got " + pot.output());
            helper.assertTrue(pot.inputs().stream().allMatch(ItemStack::isEmpty), "the Potatoes are used");
            helper.assertTrue(pot.fluid().getAmount() == 750, "250 mB water used, left " + pot.fluid().getAmount());
            helper.assertTrue(master.fuelStack().getCount() == 1, "one Coal burns");
            helper.assertTrue(helper.getBlockState(DOOR).getValue(StoveTileBlock.LIT), "the door burns");
        });
    }

    @ServusGameTest(timeoutTicks = 300)
    public static void everyTileIsACookingSpot(GameTestHelper helper) {
        KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 2));
        PotBlockEntity pot = pot(helper, ON_PLATE, 2, 1000);
        helper.succeedWhen(() -> helper.assertTrue(pot.output().is(ServusDelicaciesItems.BOILED_POTATOES.get()),
                "the Pot on the plate without the door cooks too"));
    }

    @ServusGameTest(timeoutTicks = 300)
    public static void noCookingWithoutFuel(GameTestHelper helper) {
        KitchenTestSupport.stove(helper, 2);
        PotBlockEntity pot = pot(helper, ON_DOOR, 2, 1000);
        helper.runAfterDelay(240, () -> {
            helper.assertTrue(pot.output().isEmpty() && pot.progress() == 0, "nothing cooks without fuel");
            helper.assertFalse(helper.getBlockState(ON_DOOR).getValue(VesselBlock.COOKING), "no steam");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 300)
    public static void noCookingOnALooseTile(GameTestHelper helper) {
        KitchenTestSupport.tiles(helper, DOOR, 1);
        PotBlockEntity pot = pot(helper, ON_DOOR, 2, 1000);
        helper.runAfterDelay(240, () -> {
            helper.assertTrue(pot.output().isEmpty(), "a loose tile does not heat");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 200)
    public static void leftoverIngredientBlocksTheRecipe(GameTestHelper helper) {
        KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL));
        PanBlockEntity pan = pan(helper, ON_DOOR, new ItemStack(Items.EGG), new ItemStack(ServusDelicaciesItems.CHOPPED_ONION.get()));
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(pan.output().isEmpty(), "an Egg with a Chopped Onion matches no recipe");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 400)
    public static void panFriesTheBatchOneByOne(GameTestHelper helper) {
        KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 2));
        PanBlockEntity pan = pan(helper, ON_DOOR, new ItemStack(Items.EGG, 3));
        helper.succeedWhen(() -> helper.assertTrue(pan.output().is(ServusDelicaciesItems.FRIED_EGG.get()) && pan.output().getCount() == 3,
                "three Fried Eggs, got " + pan.output()));
    }

    @ServusGameTest(timeoutTicks = 400)
    public static void friedPotatoesFromBoiledPotatoesAndOnion(GameTestHelper helper) {
        KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 2));
        PanBlockEntity pan = pan(helper, ON_DOOR, new ItemStack(ServusDelicaciesItems.BOILED_POTATOES.get(), 2),
                new ItemStack(ServusDelicaciesItems.CHOPPED_ONION.get()));
        helper.succeedWhen(() -> helper.assertTrue(pan.output().is(ServusDelicaciesItems.FRIED_POTATOES.get()) && pan.output().getCount() == 2,
                "two Fried Potatoes, got " + pan.output()));
    }

    @ServusGameTest(timeoutTicks = 400)
    public static void hopperFeedsThePot(GameTestHelper helper) {
        KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 2));
        PotBlockEntity pot = pot(helper, ON_DOOR, 0, 1000);
        BlockPos hopper = ON_DOOR.above();
        helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        helper.getBlockEntity(hopper, HopperBlockEntity.class).setItem(0, new ItemStack(Items.POTATO, 2));
        helper.succeedWhen(() -> helper.assertTrue(pot.output().is(ServusDelicaciesItems.BOILED_POTATOES.get()),
                "the hopper's Potatoes are boiled"));
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void automationTakesOnlyTheResultAndRefusesStrangers(GameTestHelper helper) {
        pan(helper, ON_DOOR, new ItemStack(Items.EGG));
        ItemView view = items(helper, ON_DOOR, Direction.DOWN);
        helper.assertTrue(view.insertItem(new ItemStack(Items.DIRT), false).getCount() == 1, "Dirt is refused");
        for (int slot = 0; slot < VesselBlockEntity.INPUTS; slot++) {
            helper.assertTrue(view.extractItem(slot, 64, false).isEmpty(), "inputs stay in");
        }
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 300)
    public static void emptyHandTakesTheResult(GameTestHelper helper) {
        KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL));
        PanBlockEntity pan = pan(helper, ON_DOOR, new ItemStack(Items.EGG));
        helper.succeedWhen(() -> {
            helper.assertTrue(pan.output().is(ServusDelicaciesItems.FRIED_EGG.get()), "the Egg is fried");
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            KitchenTestSupport.click(helper, player, ON_DOOR, ItemStack.EMPTY);
            helper.assertTrue(player.getInventory().countItem(ServusDelicaciesItems.FRIED_EGG.get()) == 1, "the player has the Fried Egg");
            helper.assertTrue(pan.output().isEmpty(), "the Pan is empty");
        });
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void ingredientsAndWaterByHand(GameTestHelper helper) {
        PotBlockEntity pot = pot(helper, ON_DOOR, 0, 0);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        KitchenTestSupport.click(helper, player, ON_DOOR, new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(pot.fluid().getAmount() == 1000, "a Water Bucket fills 1000 mB, got " + pot.fluid().getAmount());
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "the player keeps an empty Bucket");
        KitchenTestSupport.click(helper, player, ON_DOOR, new ItemStack(Items.POTATO, 5));
        helper.assertTrue(pot.inputs().getFirst().getCount() == 5, "the Potatoes go in");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "the hand is empty");
        ItemStack dirt = new ItemStack(Items.DIRT);
        InteractionResult result = KitchenTestSupport.click(helper, player, ON_DOOR, dirt);
        helper.assertTrue(dirt.getCount() == 1 && !result.consumesAction(), "Dirt stays in the hand");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void foodValuesComeFromData(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(4);
        ItemStack food = new ItemStack(ServusDelicaciesItems.FRIED_POTATOES.get());
        food.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(player.getFoodData().getFoodLevel() == 12, "Fried Potatoes restore 8, now " + player.getFoodData().getFoodLevel());
        helper.assertTrue(food.isEmpty(), "the food is used");
        player.getFoodData().setFoodLevel(20);
        ItemStack egg = new ItemStack(ServusDelicaciesItems.FRIED_EGG.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, egg);
        InteractionResult full = egg.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertFalse(full.consumesAction(), "a full player does not eat");
        ItemStack onion = new ItemStack(ServusDelicaciesItems.CHOPPED_ONION.get());
        player.getFoodData().setFoodLevel(4);
        player.setItemInHand(InteractionHand.MAIN_HAND, onion);
        helper.assertFalse(onion.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "Chopped Onion is no food");
        helper.succeed();
    }
}
