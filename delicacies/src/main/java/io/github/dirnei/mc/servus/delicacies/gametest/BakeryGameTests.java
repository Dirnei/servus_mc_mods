package io.github.dirnei.mc.servus.delicacies.gametest;

import static io.github.dirnei.mc.servus.delicacies.gametest.KitchenTestSupport.DOOR;

import io.github.dirnei.mc.servus.delicacies.board.CuttingInput;
import io.github.dirnei.mc.servus.delicacies.cooking.BakingOvenBlockEntity;
import io.github.dirnei.mc.servus.delicacies.cooking.PotBlockEntity;
import io.github.dirnei.mc.servus.delicacies.cooking.VesselBlock;
import io.github.dirnei.mc.servus.delicacies.cooking.VesselBlockEntity;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesRecipes;
import io.github.dirnei.mc.servus.foundation.drive.ElectricMotorBlockEntity;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.mill.MillBlockEntity;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The bakery: the Mill grinds flour, the Baking Oven bakes rolls and pretzels only on a hot stove and works by hand and
 * hopper, the Pot boils salt and lye, the Pretzel is eaten from data, and the shipped vanilla and cutting recipes.
 */
public final class BakeryGameTests {

    private static final BlockPos ON_DOOR = DOOR.above();
    private static final BlockPos MILL = new BlockPos(5, 2, 5);

    private BakeryGameTests() {
    }

    private static BakingOvenBlockEntity oven(GameTestHelper helper, ItemStack... inputs) {
        helper.setBlock(ON_DOOR, ServusDelicaciesBlocks.BAKING_OVEN.get());
        BakingOvenBlockEntity oven = helper.getBlockEntity(ON_DOOR, BakingOvenBlockEntity.class);
        for (ItemStack input : inputs) {
            helper.assertTrue(oven.insertInputs(input).isEmpty(), "the oven takes " + input);
        }
        return oven;
    }

    private static ItemStack[] pretzelInputs() {
        return new ItemStack[] {new ItemStack(ServusDelicaciesItems.DOUGH.get(), 2), new ItemStack(ServusDelicaciesItems.LYE.get()),
                new ItemStack(ServusDelicaciesItems.SALT.get())};
    }

    private static PotBlockEntity potWithWater(GameTestHelper helper, int water) {
        helper.setBlock(ON_DOOR, ServusDelicaciesBlocks.POT.get());
        PotBlockEntity pot = helper.getBlockEntity(ON_DOOR, PotBlockEntity.class);
        FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(ON_DOOR), Direction.UP))
                .fill(new FluidStack(Fluids.WATER, water), false);
        return pot;
    }

    private static boolean holds(ItemStack stack, Item item, int count) {
        return stack.is(item) && stack.getCount() == count;
    }

    @ServusGameTest(timeoutTicks = 200)
    public static void millGrindsWheatToFlour(GameTestHelper helper) {
        helper.setBlock(MILL, MachineContent.MILL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        MillBlockEntity mill = helper.getBlockEntity(MILL, MillBlockEntity.class);
        mill.inventory().setStackInSlot(MillBlockEntity.SLOT_INPUT, new ItemStack(Items.WHEAT));
        BlockPos motor = MILL.east();
        helper.setBlock(motor, MachineContent.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.WEST));
        helper.getBlockEntity(motor, ElectricMotorBlockEntity.class).energyStorage().setEnergy(8000);
        helper.succeedWhen(() -> {
            helper.assertTrue(mill.inventory().getStackInSlot(MillBlockEntity.SLOT_INPUT).isEmpty(), "the Wheat is ground");
            helper.assertTrue(holds(mill.inventory().getStackInSlot(MillBlockEntity.SLOT_OUTPUT), ServusDelicaciesItems.FLOUR.get(), 1),
                    "one Flour is in the output");
        });
    }

    @ServusGameTest(timeoutTicks = 300)
    public static void ovenBakesBreadRollOnHotStove(GameTestHelper helper) {
        KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 2));
        BakingOvenBlockEntity oven = oven(helper, new ItemStack(ServusDelicaciesItems.DOUGH.get()));
        helper.runAfterDelay(20, () -> helper.assertTrue(helper.getBlockState(ON_DOOR).getValue(VesselBlock.COOKING), "the oven bakes"));
        helper.succeedWhen(() -> {
            helper.assertTrue(holds(oven.output(), ServusDelicaciesItems.BREAD_ROLL.get(), 1), "one Bread Roll, got " + oven.output());
            helper.assertTrue(oven.inputs().stream().allMatch(ItemStack::isEmpty), "the Dough is used");
        });
    }

    @ServusGameTest(timeoutTicks = 300)
    public static void ovenBakesPretzelsOnHotStove(GameTestHelper helper) {
        KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 2));
        BakingOvenBlockEntity oven = oven(helper, pretzelInputs());
        helper.succeedWhen(() -> {
            helper.assertTrue(holds(oven.output(), ServusDelicaciesItems.PRETZEL.get(), 2), "two Pretzels, got " + oven.output());
            helper.assertTrue(oven.inputs().stream().allMatch(ItemStack::isEmpty), "Dough, Lye and Salt are used");
        });
    }

    @ServusGameTest(timeoutTicks = 300)
    public static void noBakingWithoutFire(GameTestHelper helper) {
        KitchenTestSupport.stove(helper, 2);
        BakingOvenBlockEntity oven = oven(helper, pretzelInputs());
        helper.runAfterDelay(280, () -> {
            helper.assertTrue(oven.output().isEmpty() && oven.progress() == 0, "nothing bakes on a cold stove");
            helper.assertFalse(helper.getBlockState(ON_DOOR).getValue(VesselBlock.COOKING), "the window stays dark");
            helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(ON_DOOR)).getLightEmission() == 0, "no light");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 600)
    public static void ovenByHandAndHopper(GameTestHelper helper) {
        KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 2));
        BakingOvenBlockEntity oven = oven(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack dirt = new ItemStack(Items.DIRT);
        InteractionResult refused = KitchenTestSupport.click(helper, player, ON_DOOR, dirt);
        helper.assertTrue(dirt.getCount() == 1 && !refused.consumesAction(), "Dirt stays in the hand");
        KitchenTestSupport.click(helper, player, ON_DOOR, new ItemStack(ServusDelicaciesItems.DOUGH.get()));
        helper.assertTrue(holds(oven.inputs().getFirst(), ServusDelicaciesItems.DOUGH.get(), 1), "the Dough goes in by hand");
        BlockPos hopper = ON_DOOR.above();
        helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        helper.getBlockEntity(hopper, HopperBlockEntity.class).setItem(0, new ItemStack(ServusDelicaciesItems.DOUGH.get()));
        helper.succeedWhen(() -> {
            helper.assertTrue(holds(oven.output(), ServusDelicaciesItems.BREAD_ROLL.get(), 2), "both Dough are baked, got " + oven.output());
            ItemView view = ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(ON_DOOR), Direction.DOWN));
            for (int slot = 0; slot < VesselBlockEntity.INPUTS; slot++) {
                helper.assertTrue(view.extractItem(slot, 64, false).isEmpty(), "inputs stay in");
            }
            helper.assertTrue(holds(view.extractItem(VesselBlockEntity.OUTPUT, 64, false), ServusDelicaciesItems.BREAD_ROLL.get(), 2),
                    "automation takes the Bread Rolls");
        });
    }

    @ServusGameTest(timeoutTicks = 500)
    public static void potBoilsSalt(GameTestHelper helper) {
        KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 4));
        PotBlockEntity pot = potWithWater(helper, 1000);
        helper.succeedWhen(() -> {
            helper.assertTrue(holds(pot.output(), ServusDelicaciesItems.SALT.get(), 1), "one Salt, got " + pot.output());
            helper.assertTrue(pot.fluid().isEmpty(), "the water is boiled away");
        });
    }

    @ServusGameTest(timeoutTicks = 300)
    public static void potMakesLye(GameTestHelper helper) {
        KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 2));
        PotBlockEntity pot = potWithWater(helper, 1000);
        pot.insertInputs(new ItemStack(ServusDelicaciesItems.WOOD_ASH.get()));
        helper.succeedWhen(() -> {
            helper.assertTrue(holds(pot.output(), ServusDelicaciesItems.LYE.get(), 1), "one Lye, got " + pot.output());
            helper.assertTrue(pot.fluid().getAmount() == 750, "250 mB water used, left " + pot.fluid().getAmount());
        });
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void pretzelIsEdibleWithDataValues(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(4);
        ItemStack pretzel = new ItemStack(ServusDelicaciesItems.PRETZEL.get());
        pretzel.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(player.getFoodData().getFoodLevel() == 10, "a Pretzel restores 6, now " + player.getFoodData().getFoodLevel());
        helper.assertTrue(pretzel.isEmpty(), "the Pretzel is used");
        ItemStack dough = new ItemStack(ServusDelicaciesItems.DOUGH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, dough);
        helper.assertFalse(dough.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(),
                "Dough is no food");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void bakeryRecipesShipped(GameTestHelper helper) {
        ItemStack flour = new ItemStack(ServusDelicaciesItems.FLOUR.get());
        CraftingInput grid = CraftingInput.of(2, 2, List.of(flour.copy(), flour.copy(), flour.copy(), new ItemStack(Items.WATER_BUCKET)));
        var dough = ServusRecipes.craftingFor(helper.getLevel(), grid).orElseThrow();
        helper.assertTrue(holds(dough.value().assemble(grid), ServusDelicaciesItems.DOUGH.get(), 3), "three Dough");
        helper.assertTrue(dough.value().getRemainingItems(grid).stream().anyMatch(stack -> stack.is(Items.BUCKET)), "the Bucket stays");

        var campfire = ServusRecipes.first(helper.getLevel(), RecipeType.CAMPFIRE_COOKING, new SingleRecipeInput(new ItemStack(Items.OAK_LOG)));
        helper.assertTrue(campfire.isPresent() && campfire.get().value().assemble(new SingleRecipeInput(new ItemStack(Items.OAK_LOG)))
                .is(ServusDelicaciesItems.WOOD_ASH.get()), "an Oak Log burns to Wood Ash on a Campfire");
        var furnace = ServusRecipes.first(helper.getLevel(), RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(Items.CHARCOAL)));
        helper.assertTrue(furnace.isPresent() && furnace.get().value().assemble(new SingleRecipeInput(new ItemStack(Items.CHARCOAL)))
                .is(ServusDelicaciesItems.WOOD_ASH.get()), "Charcoal smelts to Wood Ash");

        var crumbs = ServusRecipes.first(helper.getLevel(), ServusDelicaciesRecipes.CUTTING.get(),
                new CuttingInput(new ItemStack(ServusDelicaciesItems.BREAD_ROLL.get()), new ItemStack(ServusDelicaciesItems.KITCHEN_KNIFE.get())));
        helper.assertTrue(crumbs.isPresent() && holds(crumbs.get().value().result(), ServusDelicaciesItems.BREAD_CRUMBS.get(), 2),
                "a Bread Roll cuts into two Bread Crumbs");
        helper.succeed();
    }
}
