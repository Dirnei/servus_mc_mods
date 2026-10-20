package io.github.dirnei.mc.servus.delicacies.gametest;

import static io.github.dirnei.mc.servus.delicacies.gametest.KitchenTestSupport.DOOR;

import io.github.dirnei.mc.servus.delicacies.board.CuttingBoardBlockEntity;
import io.github.dirnei.mc.servus.delicacies.board.CuttingInput;
import io.github.dirnei.mc.servus.delicacies.cooking.BakingOvenBlockEntity;
import io.github.dirnei.mc.servus.delicacies.cooking.PanBlockEntity;
import io.github.dirnei.mc.servus.delicacies.cooking.PotBlockEntity;
import io.github.dirnei.mc.servus.delicacies.cooking.VesselBlock;
import io.github.dirnei.mc.servus.delicacies.crop.CropKind;
import io.github.dirnei.mc.servus.delicacies.meat.LeberkaeseBlock;
import io.github.dirnei.mc.servus.delicacies.meat.LoafSlices;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesRecipes;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The meat dishes: the Schnitzel chain from board to pan, Weisswurst in the Pot, the placed loaf sliced with the knife
 * and its drops, Roast Pork and Leberkaese from the oven, Gravy eaten from data, and the shipped recipes.
 */
public final class MeatGameTests {

    private static final BlockPos ON_DOOR = DOOR.above();
    private static final BlockPos BOARD = new BlockPos(5, 1, 5);
    private static final BlockPos LOAF = new BlockPos(5, 1, 2);

    private MeatGameTests() {
    }

    private static boolean holds(ItemStack stack, Item item, int count) {
        return stack.is(item) && stack.getCount() == count;
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    private static PotBlockEntity potWithWater(GameTestHelper helper, int water) {
        helper.setBlock(ON_DOOR, ServusDelicaciesBlocks.POT.get());
        PotBlockEntity pot = helper.getBlockEntity(ON_DOOR, PotBlockEntity.class);
        if (water > 0) {
            FluidView.of(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(ON_DOOR), Direction.UP))
                    .fill(new FluidStack(Fluids.WATER, water), false);
        }
        return pot;
    }

    private static BakingOvenBlockEntity oven(GameTestHelper helper, ItemStack... inputs) {
        helper.setBlock(ON_DOOR, ServusDelicaciesBlocks.BAKING_OVEN.get());
        BakingOvenBlockEntity oven = helper.getBlockEntity(ON_DOOR, BakingOvenBlockEntity.class);
        for (ItemStack input : inputs) {
            helper.assertTrue(oven.insertInputs(input).isEmpty(), "the oven takes " + input);
        }
        return oven;
    }

    private static ItemStack crafted(GameTestHelper helper, int width, int height, List<ItemStack> grid) {
        CraftingInput input = CraftingInput.of(width, height, grid);
        return ServusRecipes.craftingFor(helper.getLevel(), input).map(recipe -> recipe.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    private static ItemStack cut(GameTestHelper helper, ItemStack item) {
        return ServusRecipes.first(helper.getLevel(), ServusDelicaciesRecipes.CUTTING.get(),
                new CuttingInput(item, new ItemStack(ServusDelicaciesItems.KITCHEN_KNIFE.get())))
                .map(recipe -> recipe.value().result()).orElse(ItemStack.EMPTY);
    }

    @ServusGameTest(timeoutTicks = 300)
    public static void schnitzelChain(GameTestHelper helper) {
        helper.setBlock(BOARD, ServusDelicaciesBlocks.CUTTING_BOARD.get());
        CuttingBoardBlockEntity board = helper.getBlockEntity(BOARD, CuttingBoardBlockEntity.class);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        KitchenTestSupport.click(helper, player, BOARD, new ItemStack(Items.PORKCHOP));
        KitchenTestSupport.click(helper, player, BOARD, new ItemStack(ServusDelicaciesItems.KITCHEN_KNIFE.get()));
        ItemStack cutlet = board.stack(CuttingBoardBlockEntity.SLOT_PIECES);
        helper.assertTrue(holds(cutlet, ServusDelicaciesItems.PORK_CUTLET.get(), 1), "the knife cuts a Pork Cutlet, got " + cutlet);

        ItemStack breaded = crafted(helper, 2, 2, List.of(cutlet.copy(), new ItemStack(ServusDelicaciesItems.FLOUR.get()),
                new ItemStack(Items.EGG), new ItemStack(ServusDelicaciesItems.BREAD_CRUMBS.get())));
        helper.assertTrue(holds(breaded, ServusDelicaciesItems.BREADED_CUTLET.get(), 1), "the cutlet is breaded, got " + breaded);

        KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 2));
        helper.setBlock(ON_DOOR, ServusDelicaciesBlocks.PAN.get());
        PanBlockEntity pan = helper.getBlockEntity(ON_DOOR, PanBlockEntity.class);
        helper.assertTrue(pan.insertInputs(breaded).isEmpty(), "the Pan takes the Breaded Cutlet");
        helper.runAfterDelay(20, () -> helper.assertTrue(helper.getBlockState(ON_DOOR).getValue(VesselBlock.COOKING), "the Pan fries"));
        helper.succeedWhen(() -> {
            helper.assertTrue(holds(pan.output(), ServusDelicaciesItems.SCHNITZEL.get(), 1), "one Schnitzel, got " + pan.output());
            helper.assertTrue(pan.inputs().stream().allMatch(ItemStack::isEmpty), "the Breaded Cutlet is used");
        });
    }

    @ServusGameTest(timeoutTicks = 300)
    public static void weisswurstInThePot(GameTestHelper helper) {
        KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 2));
        PotBlockEntity pot = potWithWater(helper, 1000);
        pot.insertInputs(stack(ServusDelicaciesItems.SAUSAGE_MEAT.get(), 2));
        helper.succeedWhen(() -> {
            helper.assertTrue(holds(pot.output(), ServusDelicaciesItems.WEISSWURST.get(), 2), "two Weisswurst, got " + pot.output());
            helper.assertTrue(pot.inputs().stream().allMatch(ItemStack::isEmpty), "the Sausage Meat is used");
            helper.assertTrue(pot.fluid().getAmount() == 750, "250 mB water used, left " + pot.fluid().getAmount());
        });
    }

    @ServusGameTest(timeoutTicks = 300)
    public static void noWeisswurstWithoutWater(GameTestHelper helper) {
        KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 2));
        PotBlockEntity pot = potWithWater(helper, 0);
        pot.insertInputs(stack(ServusDelicaciesItems.SAUSAGE_MEAT.get(), 2));
        helper.runAfterDelay(260, () -> {
            helper.assertTrue(pot.output().isEmpty() && pot.progress() == 0, "nothing cooks without water");
            helper.assertTrue(holds(pot.inputs().getFirst(), ServusDelicaciesItems.SAUSAGE_MEAT.get(), 2), "the Sausage Meat stays raw");
            helper.succeed();
        });
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void leberkaeseSlicedWithKnife(GameTestHelper helper) {
        helper.setBlock(LOAF, ServusDelicaciesBlocks.LEBERKAESE.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        KitchenTestSupport.click(helper, player, LOAF, ItemStack.EMPTY);
        KitchenTestSupport.click(helper, player, LOAF, new ItemStack(Items.STICK));
        helper.assertTrue(helper.getBlockState(LOAF).getValue(LeberkaeseBlock.CUT) == 0, "a hand or a stick cuts nothing");
        helper.assertTrue(player.getInventory().countItem(ServusDelicaciesItems.LEBERKAESE_SLICE.get()) == 0, "and gives nothing");

        ItemStack knife = new ItemStack(ServusDelicaciesItems.KITCHEN_KNIFE.get());
        for (int cut = 1; cut < LoafSlices.SLICES; cut++) {
            KitchenTestSupport.click(helper, player, LOAF, knife);
            helper.assertTrue(helper.getBlockState(LOAF).getValue(LeberkaeseBlock.CUT) == cut, "the loaf is one slice shorter");
            helper.assertTrue(player.getInventory().countItem(ServusDelicaciesItems.LEBERKAESE_SLICE.get()) == cut,
                    cut + " slices in the inventory");
        }
        KitchenTestSupport.click(helper, player, LOAF, knife);
        helper.assertBlockNotPresent(ServusDelicaciesBlocks.LEBERKAESE.get(), LOAF);
        helper.assertTrue(player.getInventory().countItem(ServusDelicaciesItems.LEBERKAESE_SLICE.get()) == LoafSlices.SLICES,
                "the last cut gives the last slice");
        helper.assertTrue(knife.getDamageValue() == LoafSlices.SLICES, "the knife wore once per slice, " + knife.getDamageValue());
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void cutLoafDropsItsSlices(GameTestHelper helper) {
        BlockState uncut = ServusDelicaciesBlocks.LEBERKAESE.get().defaultBlockState();
        List<ItemStack> whole = Block.getDrops(uncut, helper.getLevel(), helper.absolutePos(LOAF), null);
        helper.assertTrue(whole.size() == 1 && holds(whole.getFirst(), ServusDelicaciesItems.LEBERKAESE.get(), 1),
                "an uncut loaf drops itself, got " + whole);
        List<ItemStack> cut = Block.getDrops(uncut.setValue(LeberkaeseBlock.CUT, 1), helper.getLevel(), helper.absolutePos(LOAF), null);
        helper.assertTrue(cut.size() == 1 && holds(cut.getFirst(), ServusDelicaciesItems.LEBERKAESE_SLICE.get(), 3),
                "a loaf with one slice off drops three slices, got " + cut);
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 500)
    public static void roastPorkInTheOven(GameTestHelper helper) {
        KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 4));
        BakingOvenBlockEntity oven = oven(helper, stack(Items.PORKCHOP, 2), new ItemStack(ServusDelicaciesItems.harvest(CropKind.ONION)),
                new ItemStack(ServusDelicaciesItems.SALT.get()));
        helper.succeedWhen(() -> {
            helper.assertTrue(holds(oven.output(), ServusDelicaciesItems.ROAST_PORK.get(), 2), "two Roast Pork, got " + oven.output());
            helper.assertTrue(oven.inputs().stream().allMatch(ItemStack::isEmpty), "Porkchops, Onion and Salt are used");
        });
    }

    @ServusGameTest(timeoutTicks = 500)
    public static void ovenBakesLeberkaese(GameTestHelper helper) {
        KitchenTestSupport.fuelledStove(helper, 2, new ItemStack(Items.COAL, 4));
        BakingOvenBlockEntity oven = oven(helper, stack(ServusDelicaciesItems.MINCED_PORK.get(), 2),
                stack(ServusDelicaciesItems.MINCED_BEEF.get(), 2), new ItemStack(ServusDelicaciesItems.SALT.get()));
        helper.succeedWhen(() -> helper.assertTrue(holds(oven.output(), ServusDelicaciesItems.LEBERKAESE.get(), 1),
                "one Leberkaese loaf, got " + oven.output()));
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void gravyGivesTheBowlBack(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(4);
        ItemStack left = new ItemStack(ServusDelicaciesItems.GRAVY.get()).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(player.getFoodData().getFoodLevel() == 7, "Gravy restores 3, now " + player.getFoodData().getFoodLevel());
        helper.assertTrue(left.is(Items.BOWL), "the Bowl comes back, got " + left);
        ItemStack schnitzel = new ItemStack(ServusDelicaciesItems.SCHNITZEL.get());
        schnitzel.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(player.getFoodData().getFoodLevel() == 17, "a Schnitzel restores 10, now " + player.getFoodData().getFoodLevel());
        ItemStack cutlet = new ItemStack(ServusDelicaciesItems.PORK_CUTLET.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, cutlet);
        helper.assertFalse(cutlet.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "a Pork Cutlet is no food");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void meatRecipesShipped(GameTestHelper helper) {
        helper.assertTrue(holds(cut(helper, new ItemStack(Items.BEEF)), ServusDelicaciesItems.MINCED_BEEF.get(), 2), "Beef cuts into two");
        helper.assertTrue(holds(cut(helper, new ItemStack(ServusDelicaciesItems.PORK_CUTLET.get())), ServusDelicaciesItems.MINCED_PORK.get(), 2),
                "a Pork Cutlet cuts into two Minced Pork");
        helper.assertTrue(holds(cut(helper, new ItemStack(ServusDelicaciesItems.LEBERKAESE.get())), ServusDelicaciesItems.LEBERKAESE_SLICE.get(),
                LoafSlices.SLICES), "a loaf on the board gives every slice");

        ItemStack sausage = crafted(helper, 2, 2, List.of(new ItemStack(ServusDelicaciesItems.MINCED_PORK.get()),
                new ItemStack(ServusDelicaciesItems.MINCED_BEEF.get()), new ItemStack(ServusDelicaciesItems.harvest(CropKind.PARSLEY)),
                new ItemStack(ServusDelicaciesItems.SALT.get())));
        helper.assertTrue(holds(sausage, ServusDelicaciesItems.SAUSAGE_MEAT.get(), 2), "two Sausage Meat, got " + sausage);
        ItemStack roll = crafted(helper, 2, 1, List.of(new ItemStack(ServusDelicaciesItems.LEBERKAESE_SLICE.get()),
                new ItemStack(ServusDelicaciesItems.BREAD_ROLL.get())));
        helper.assertTrue(holds(roll, ServusDelicaciesItems.LEBERKAESE_ROLL.get(), 1), "a Leberkaese Roll, got " + roll);

        helper.assertTrue(ServusRecipes.byId(helper.getLevel(), Identifier.fromNamespaceAndPath("servus_delicacies", "raw_cordon_bleu"))
                .isPresent(), "the Cordon Bleu recipe loads with the cheese tag");
        ItemStack seeds = new ItemStack(ServusDelicaciesItems.harvest(CropKind.MUSTARD));
        var mustard = ServusRecipes.all(helper.getLevel(), MachineContent.MILLING.get()).stream()
                .map(holder -> holder.value())
                .filter(milling -> milling.input().test(seeds))
                .findFirst();
        helper.assertTrue(mustard.isPresent() && mustard.get().result().is(ServusDelicaciesItems.MUSTARD_FLOUR.get()),
                "Mustard Seeds mill into Mustard Flour");
        helper.succeed();
    }
}
