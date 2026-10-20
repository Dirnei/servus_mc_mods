package io.github.dirnei.mc.servus.delicacies.gametest;

import static io.github.dirnei.mc.servus.delicacies.gametest.KitchenTestSupport.DOOR;

import io.github.dirnei.mc.servus.delicacies.board.CuttingInput;
import io.github.dirnei.mc.servus.delicacies.cooking.PotBlockEntity;
import io.github.dirnei.mc.servus.delicacies.cooking.VesselBlockEntity;
import io.github.dirnei.mc.servus.delicacies.crock.FermentingCrockBlock;
import io.github.dirnei.mc.servus.delicacies.crock.FermentingCrockBlockEntity;
import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenTags;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesRecipes;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The sides: the Fermenting Crock seals, keeps game time and ferments whole batches into Sauerkraut and Cheese, stays
 * shut while sealed, and the Pot cooks the dumplings and the red cabbage. GameTests let the crock's time pass with
 * {@link FermentingCrockBlockEntity#backdate} instead of waiting days.
 */
public final class SidesGameTests {

    private static final BlockPos CROCK = new BlockPos(2, 1, 2);
    private static final BlockPos ON_DOOR = DOOR.above();
    private static final int SAUERKRAUT_TIME = 72000;
    private static final int CHEESE_TIME = 48000;

    private SidesGameTests() {
    }

    private static FermentingCrockBlockEntity crock(GameTestHelper helper, BlockPos pos, ItemStack... inputs) {
        helper.setBlock(pos, ServusDelicaciesBlocks.FERMENTING_CROCK.get());
        FermentingCrockBlockEntity crock = helper.getBlockEntity(pos, FermentingCrockBlockEntity.class);
        for (ItemStack input : inputs) {
            helper.assertTrue(crock.insertInputs(input).isEmpty(), "the crock takes " + input);
        }
        return crock;
    }

    private static ItemStack cabbage(int count) {
        return new ItemStack(ServusDelicaciesItems.SHREDDED_CABBAGE.get(), count);
    }

    private static ItemStack salt(int count) {
        return new ItemStack(ServusDelicaciesItems.SALT.get(), count);
    }

    private static boolean holds(ItemStack stack, Item item, int count) {
        return stack.is(item) && stack.getCount() == count;
    }

    private static boolean sealedState(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(FermentingCrockBlock.SEALED);
    }

    private static boolean inputsHold(FermentingCrockBlockEntity crock, Item item, int count) {
        return crock.inputs().stream().filter(stack -> !stack.isEmpty())
                .allMatch(stack -> holds(stack, item, count)) && crock.inputs().stream().anyMatch(stack -> !stack.isEmpty());
    }

    @ServusGameTest(timeoutTicks = 60)
    public static void sauerkrautAfterTime(GameTestHelper helper) {
        FermentingCrockBlockEntity crock = crock(helper, CROCK, cabbage(4), salt(1));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(crock.sealed() && sealedState(helper, CROCK), "Shredded Cabbage and Salt seal the crock");
            helper.assertTrue(crock.remaining() > SAUERKRAUT_TIME - 100, "about three days to go, got " + crock.remaining());
            crock.backdate(SAUERKRAUT_TIME / 2);
            helper.assertTrue(crock.sealed() && crock.output().isEmpty(), "half the time: still sealed and nothing made");
            crock.backdate(SAUERKRAUT_TIME / 2);
            helper.assertFalse(crock.sealed() || sealedState(helper, CROCK), "the lid is off once the time is up");
            helper.assertTrue(holds(crock.output(), ServusDelicaciesItems.SAUERKRAUT.get(), 4), "four Sauerkraut, got " + crock.output());
            helper.assertTrue(crock.inputs().stream().allMatch(ItemStack::isEmpty), "cabbage and salt are used");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 60)
    public static void cheeseAfterTime(GameTestHelper helper) {
        FermentingCrockBlockEntity crock = crock(helper, CROCK);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        KitchenTestSupport.click(helper, player, CROCK, new ItemStack(Items.MILK_BUCKET));
        helper.assertTrue(crock.fluid().getAmount() == 1000, "the Milk Bucket empties into the crock, got " + crock.fluid().getAmount());
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "the player keeps an empty Bucket");
        KitchenTestSupport.click(helper, player, CROCK, salt(1));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(crock.sealed(), "milk and Salt seal the crock");
            crock.backdate(CHEESE_TIME);
            helper.assertTrue(holds(crock.output(), ServusDelicaciesItems.CHEESE.get(), 4), "four Cheese, got " + crock.output());
            helper.assertTrue(crock.fluid().isEmpty(), "the milk is used");
            helper.assertTrue(crock.output().is(KitchenTags.CHEESE), "Cheese is in c:foods/cheese");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 60)
    public static void crockBatchesAndLeftovers(GameTestHelper helper) {
        FermentingCrockBlockEntity whole = crock(helper, CROCK, cabbage(8), salt(2));
        BlockPos other = CROCK.east(2);
        FermentingCrockBlockEntity partial = crock(helper, other, cabbage(6), salt(1));
        helper.runAfterDelay(5, () -> {
            whole.backdate(SAUERKRAUT_TIME);
            partial.backdate(SAUERKRAUT_TIME);
            helper.assertTrue(holds(whole.output(), ServusDelicaciesItems.SAUERKRAUT.get(), 8), "two batches, got " + whole.output());
            helper.assertTrue(whole.inputs().stream().allMatch(ItemStack::isEmpty), "everything fermented");
            helper.assertTrue(holds(partial.output(), ServusDelicaciesItems.SAUERKRAUT.get(), 4), "one batch, got " + partial.output());
            helper.assertTrue(inputsHold(partial, ServusDelicaciesItems.SHREDDED_CABBAGE.get(), 2), "two Shredded Cabbage stay");
            helper.assertFalse(partial.sealed(), "the leftovers alone do not seal it again");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 100)
    public static void sealedCrockRefusesInput(GameTestHelper helper) {
        FermentingCrockBlockEntity crock = crock(helper, CROCK, cabbage(4), salt(1));
        BlockPos hopper = CROCK.above();
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(crock.sealed(), "the crock is sealed");
            helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
            helper.getBlockEntity(hopper, HopperBlockEntity.class).setItem(0, salt(1));
        });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(helper.getBlockEntity(hopper, HopperBlockEntity.class).getItem(0).is(ServusDelicaciesItems.SALT.get()),
                    "the Salt stays in the hopper while the crock is sealed");
            int salt = crock.inputs().stream().filter(stack -> stack.is(ServusDelicaciesItems.SALT.get())).mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(crock.sealed() && salt == 1, "the crock still holds one Salt and stays sealed, Salt " + salt);
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 100)
    public static void pipesTakeOnlyResults(GameTestHelper helper) {
        BlockPos above = CROCK.above();
        FermentingCrockBlockEntity crock = crock(helper, above, cabbage(6), salt(1));
        helper.runAfterDelay(5, () -> {
            crock.backdate(SAUERKRAUT_TIME);
            helper.setBlock(CROCK, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        });
        helper.succeedWhen(() -> {
            HopperBlockEntity hopper = helper.getBlockEntity(CROCK, HopperBlockEntity.class);
            int sauerkraut = 0;
            for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
                ItemStack stack = hopper.getItem(slot);
                helper.assertFalse(stack.is(ServusDelicaciesItems.SHREDDED_CABBAGE.get()), "the leftover cabbage stays in the crock");
                if (stack.is(ServusDelicaciesItems.SAUERKRAUT.get())) {
                    sauerkraut += stack.getCount();
                }
            }
            helper.assertTrue(sauerkraut == 4, "the hopper pulls the four Sauerkraut, has " + sauerkraut);
            ItemView view = ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(above), Direction.DOWN));
            for (int slot = 0; slot < VesselBlockEntity.INPUTS; slot++) {
                helper.assertTrue(view.extractItem(slot, 64, true).isEmpty(), "inputs stay in");
            }
        });
    }

    private static PotBlockEntity potWith(GameTestHelper helper, FluidStack fluid, ItemStack... inputs) {
        KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 4));
        helper.setBlock(ON_DOOR, ServusDelicaciesBlocks.POT.get());
        PotBlockEntity pot = helper.getBlockEntity(ON_DOOR, PotBlockEntity.class);
        int filled = FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(ON_DOOR), Direction.UP))
                .fill(fluid, false);
        helper.assertTrue(filled == fluid.getAmount(), "the Pot takes " + fluid.getAmount() + " mB, took " + filled);
        for (ItemStack input : inputs) {
            helper.assertTrue(pot.insertInputs(input).isEmpty(), "the Pot takes " + input);
        }
        return pot;
    }

    private static void potMakes(GameTestHelper helper, PotBlockEntity pot, Item result, int count) {
        helper.succeedWhen(() -> {
            helper.assertTrue(holds(pot.output(), result, count), "the Pot makes " + count + " " + result + ", got " + pot.output());
            helper.assertTrue(pot.inputs().stream().allMatch(ItemStack::isEmpty), "the ingredients are used");
        });
    }

    @ServusGameTest(timeoutTicks = 500)
    public static void breadDumplingsInThePot(GameTestHelper helper) {
        PotBlockEntity pot = potWith(helper, new FluidStack(NeoForgeMod.MILK.get(), 250),
                new ItemStack(ServusDelicaciesItems.BREAD_CRUMBS.get(), 2), new ItemStack(Items.EGG),
                new ItemStack(ServusDelicaciesItems.CHOPPED_ONION.get()), new ItemStack(ServusDelicaciesItems.harvest(
                        CropKind.PARSLEY)));
        potMakes(helper, pot, ServusDelicaciesItems.BREAD_DUMPLING.get(), 2);
    }

    @ServusGameTest(timeoutTicks = 500)
    public static void potatoDumplingsInThePot(GameTestHelper helper) {
        PotBlockEntity pot = potWith(helper, new FluidStack(Fluids.WATER, 250),
                new ItemStack(ServusDelicaciesItems.GRATED_POTATO.get(), 2), new ItemStack(ServusDelicaciesItems.BOILED_POTATOES.get()),
                salt(1));
        potMakes(helper, pot, ServusDelicaciesItems.POTATO_DUMPLING.get(), 2);
    }

    @ServusGameTest(timeoutTicks = 500)
    public static void redCabbageInThePot(GameTestHelper helper) {
        PotBlockEntity pot = potWith(helper, new FluidStack(Fluids.WATER, 250),
                new ItemStack(ServusDelicaciesItems.SHREDDED_RED_CABBAGE.get(), 2), new ItemStack(Items.APPLE), new ItemStack(Items.SUGAR));
        potMakes(helper, pot, ServusDelicaciesItems.BRAISED_RED_CABBAGE.get(), 2);
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void sidesRecipesShipped(GameTestHelper helper) {
        ItemStack knife = new ItemStack(ServusDelicaciesItems.KITCHEN_KNIFE.get());
        var red = ServusRecipes.first(helper.getLevel(), ServusDelicaciesRecipes.CUTTING.get(),
                new CuttingInput(new ItemStack(ServusDelicaciesItems.harvest(CropKind.RED_CABBAGE)), knife));
        helper.assertTrue(red.isPresent() && holds(red.get().value().result(), ServusDelicaciesItems.SHREDDED_RED_CABBAGE.get(), 2),
                "a Red Cabbage cuts into two Shredded Red Cabbage");
        var grated = ServusRecipes.first(helper.getLevel(), ServusDelicaciesRecipes.CUTTING.get(),
                new CuttingInput(new ItemStack(Items.POTATO), knife));
        helper.assertTrue(grated.isPresent() && holds(grated.get().value().result(), ServusDelicaciesItems.GRATED_POTATO.get(), 1),
                "a Potato grates into Grated Potato");

        assertCrafts(helper, List.of(new ItemStack(ServusDelicaciesItems.SAUERKRAUT.get()), new ItemStack(Items.BOWL)),
                ServusDelicaciesItems.BOWL_OF_SAUERKRAUT.get());
        assertCrafts(helper, List.of(new ItemStack(ServusDelicaciesItems.BRAISED_RED_CABBAGE.get()), new ItemStack(Items.BOWL)),
                ServusDelicaciesItems.BOWL_OF_RED_CABBAGE.get());
        assertCrafts(helper, List.of(new ItemStack(ServusDelicaciesItems.BOILED_POTATOES.get()), new ItemStack(ServusDelicaciesItems.BOILED_POTATOES.get()),
                new ItemStack(ServusDelicaciesItems.CHOPPED_ONION.get()), new ItemStack(Items.BOWL)), ServusDelicaciesItems.POTATO_SALAD.get());

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(4);
        ItemStack left = new ItemStack(ServusDelicaciesItems.BOWL_OF_SAUERKRAUT.get()).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(player.getFoodData().getFoodLevel() == 8, "a Bowl of Sauerkraut restores 4, now " + player.getFoodData().getFoodLevel());
        helper.assertTrue(left.is(Items.BOWL), "the Bowl comes back, got " + left);
        ItemStack dumpling = new ItemStack(ServusDelicaciesItems.BREAD_DUMPLING.get());
        dumpling.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(player.getFoodData().getFoodLevel() == 14, "a Bread Dumpling restores 6, now " + player.getFoodData().getFoodLevel());
        helper.succeed();
    }

    private static void assertCrafts(GameTestHelper helper, List<ItemStack> items, Item result) {
        CraftingInput grid = CraftingInput.of(2, 2, padded(items));
        var recipe = ServusRecipes.craftingFor(helper.getLevel(), grid);
        helper.assertTrue(recipe.isPresent() && recipe.get().value().assemble(grid).is(result), "the grid crafts " + result);
    }

    private static List<ItemStack> padded(List<ItemStack> items) {
        List<ItemStack> grid = new ArrayList<>(items);
        while (grid.size() < 4) {
            grid.add(ItemStack.EMPTY);
        }
        return grid;
    }
}
