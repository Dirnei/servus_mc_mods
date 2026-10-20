package io.github.dirnei.mc.servus.brewery.gametest;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.malting.FloorContent;
import io.github.dirnei.mc.servus.brewery.malting.GerminatingRecipe;
import io.github.dirnei.mc.servus.brewery.malting.Germination;
import io.github.dirnei.mc.servus.brewery.malting.MaltingFloorBlock;
import io.github.dirnei.mc.servus.brewery.malting.MaltingFloorBlockEntity;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlocks;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import io.github.dirnei.mc.servus.foundation.drive.ElectricMotorBlockEntity;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.mill.MillBlockEntity;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Soaking grain, spreading it on the Malting Floor, germination, turning, pipes and breaking, and the Mill grinding
 * malt to grist.
 */
public final class MaltingGameTests {

    private static final BlockPos FLOOR = new BlockPos(2, 1, 2);
    /** The Mill faces north; the motor sits east of it with its shaft pointing west into the Mill's left side. */
    private static final BlockPos MILL = new BlockPos(5, 2, 5);
    private static final BlockPos MOTOR = MILL.east();

    private MaltingGameTests() {
    }

    // ---------- soaking ----------

    @ServusGameTest
    public static void soakingRecipeKeepsTheBucket(GameTestHelper helper) {
        Item barley = ServusBreweryItems.BARLEY.get();
        List<ItemStack> grid = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            grid.add(new ItemStack(slot == 4 ? Items.WATER_BUCKET : barley));
        }
        CraftingInput input = CraftingInput.of(3, 3, grid);
        CraftingRecipe recipe = ServusRecipes.craftingFor(helper.getLevel(), input).orElseThrow().value();
        ItemStack result = recipe.assemble(input);
        helper.assertTrue(result.is(ServusBreweryItems.SOAKED_BARLEY.get()) && result.getCount() == 8,
                "eight Barley around a Water Bucket should make eight Soaked Barley, got " + result);
        NonNullList<ItemStack> remaining = recipe.getRemainingItems(input);
        helper.assertTrue(remaining.get(4).is(Items.BUCKET), "the empty Bucket should stay in the grid, got " + remaining);
        helper.succeed();
    }

    // ---------- the floor by hand ----------

    @ServusGameTest
    public static void spreadingSoakedBarley(GameTestHelper helper) {
        MaltingFloorBlockEntity floor = floor(helper);
        Player player = holding(helper, new ItemStack(ServusBreweryItems.SOAKED_BARLEY.get(), 16));
        helper.useBlock(FLOOR, player);
        helper.assertTrue(floor.grain().is(ServusBreweryItems.SOAKED_BARLEY.get()) && floor.grain().getCount() == 16,
                "the sixteen Soaked Barley should lie on the floor, got " + floor.grain());
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the player's stack should be used up");
        helper.assertTrue(content(helper) == FloorContent.SOAKED, "the floor should show soaked grain");
        helper.assertTrue(floor.germination().isPresent(), "the batch should be germinating");
        helper.succeed();
    }

    @ServusGameTest
    public static void otherGrainStaysOff(GameTestHelper helper) {
        MaltingFloorBlockEntity floor = floor(helper);
        Player plain = holding(helper, new ItemStack(ServusBreweryItems.BARLEY.get(), 4));
        helper.useBlock(FLOOR, plain);
        helper.assertTrue(floor.grain().isEmpty(), "plain Barley should not go onto the floor");
        helper.useBlock(FLOOR, holding(helper, new ItemStack(ServusBreweryItems.SOAKED_BARLEY.get(), 4)));
        Player wheat = holding(helper, new ItemStack(ServusBreweryItems.SOAKED_WHEAT.get(), 4));
        helper.useBlock(FLOOR, wheat);
        helper.assertTrue(floor.grain().is(ServusBreweryItems.SOAKED_BARLEY.get()) && floor.grain().getCount() == 4,
                "Soaked Wheat should not go onto a floor with Soaked Barley, got " + floor.grain());
        helper.assertTrue(wheat.getMainHandItem().getCount() == 4, "the wheat should stay in the hand");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 300)
    public static void soakedBarleyBecomesGreenMalt(GameTestHelper helper) {
        MaltingFloorBlockEntity floor = floor(helper);
        ItemView pipe = pipe(helper);
        pipe.insertItem(new ItemStack(ServusBreweryItems.SOAKED_BARLEY.get(), 16), false);
        int time = recipe(helper, ServusBreweryItems.SOAKED_BARLEY.get()).time();
        CompoundTag saved = floor.saveWithoutMetadata(helper.getLevel().registryAccess());
        saved.putLong("Started", helper.getLevel().getGameTime() - time);
        floor.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.succeedWhen(() -> {
            helper.assertTrue(floor.grain().is(ServusBreweryItems.GREEN_MALT.get()) && floor.grain().getCount() == 16,
                    "the sixteen Soaked Barley should have become Green Malt, got " + floor.grain());
            helper.assertTrue(content(helper) == FloorContent.GREEN, "the floor should show green malt");
        });
    }

    @ServusGameTest(timeoutTicks = 300)
    public static void notDoneBeforeItsTime(GameTestHelper helper) {
        MaltingFloorBlockEntity floor = floor(helper);
        helper.useBlock(FLOOR, holding(helper, new ItemStack(ServusBreweryItems.SOAKED_BARLEY.get(), 8)));
        helper.runAfterDelay(250, () -> {
            floor.check();
            helper.assertTrue(floor.grain().is(ServusBreweryItems.SOAKED_BARLEY.get()), "the barley should still be soaking");
            helper.assertTrue(content(helper) == FloorContent.SOAKED, "the floor should still show soaked grain");
            helper.succeed();
        });
    }

    @ServusGameTest
    public static void turningGivesABonusOncePerCooldown(GameTestHelper helper) {
        MaltingFloorBlockEntity floor = floor(helper);
        helper.useBlock(FLOOR, holding(helper, new ItemStack(ServusBreweryItems.SOAKED_BARLEY.get(), 8)));
        GerminatingRecipe recipe = recipe(helper, ServusBreweryItems.SOAKED_BARLEY.get());
        Player hand = holding(helper, ItemStack.EMPTY);
        helper.useBlock(FLOOR, hand);
        helper.assertTrue(floor.germination().map(Germination::bonus).orElse(-1L) == recipe.turnBonus(),
                "the first turn should give the bonus");
        helper.useBlock(FLOOR, hand);
        helper.assertTrue(floor.germination().map(Germination::bonus).orElse(-1L) == recipe.turnBonus(),
                "a second turn within the cooldown should give nothing");
        helper.assertTrue(floor.grain().is(ServusBreweryItems.SOAKED_BARLEY.get()), "turning should leave the grain on the floor");
        helper.succeed();
    }

    @ServusGameTest
    public static void collectingGreenMaltAndTakingBackSoakedGrain(GameTestHelper helper) {
        MaltingFloorBlockEntity floor = floor(helper);
        floor.slot().setStackInSlot(0, new ItemStack(ServusBreweryItems.GREEN_MALT.get(), 5));
        Player player = holding(helper, ItemStack.EMPTY);
        helper.useBlock(FLOOR, player);
        helper.assertTrue(floor.grain().isEmpty(), "the Green Malt should be collected");
        helper.assertTrue(player.getInventory().countItem(ServusBreweryItems.GREEN_MALT.get()) == 5, "the player should hold the Green Malt");
        helper.assertTrue(content(helper) == FloorContent.EMPTY, "the floor should be empty");

        floor.slot().setStackInSlot(0, new ItemStack(ServusBreweryItems.SOAKED_BARLEY.get(), 3));
        Player sneaking = holding(helper, ItemStack.EMPTY);
        sneaking.setShiftKeyDown(true);
        helper.useBlock(FLOOR, sneaking);
        helper.assertTrue(floor.grain().isEmpty(), "sneaking should take the soaked grain back");
        helper.assertTrue(sneaking.getInventory().countItem(ServusBreweryItems.SOAKED_BARLEY.get()) == 3, "the player should hold the Soaked Barley");
        helper.succeed();
    }

    @ServusGameTest
    public static void pipesTakeOnlyGreenMalt(GameTestHelper helper) {
        MaltingFloorBlockEntity floor = floor(helper);
        ItemView pipe = pipe(helper);
        pipe.insertItem(new ItemStack(ServusBreweryItems.SOAKED_BARLEY.get(), 8), false);
        helper.assertTrue(pipe.extractItem(0, 64, false).isEmpty(), "a pipe should not take soaked grain");
        helper.assertTrue(floor.grain().getCount() == 8, "the soaked grain should stay");
        floor.slot().setStackInSlot(0, new ItemStack(ServusBreweryItems.GREEN_MALT.get(), 8));
        ItemStack taken = pipe.extractItem(0, 64, false);
        helper.assertTrue(taken.is(ServusBreweryItems.GREEN_MALT.get()) && taken.getCount() == 8, "a pipe should take the Green Malt, got " + taken);
        helper.assertTrue(content(helper) == FloorContent.EMPTY, "the floor should be empty");
        helper.succeed();
    }

    @ServusGameTest
    public static void breakingDropsTheGrain(GameTestHelper helper) {
        floor(helper);
        helper.useBlock(FLOOR, holding(helper, new ItemStack(ServusBreweryItems.SOAKED_BARLEY.get(), 6)));
        helper.getLevel().destroyBlock(helper.absolutePos(FLOOR), true);
        helper.assertItemEntityCountIs(ServusBreweryItems.SOAKED_BARLEY.get(), FLOOR, 2, 6);
        helper.assertItemEntityCountIs(ServusBreweryItems.MALTING_FLOOR.get(), FLOOR, 2, 1);
        helper.succeed();
    }

    // ---------- milling ----------

    @ServusGameTest(timeoutTicks = 200)
    public static void millGrindsPaleMaltToGrist(GameTestHelper helper) {
        grinds(helper, ServusBreweryItems.PALE_MALT.get(), ServusBreweryItems.PALE_MALT_GRIST.get());
    }

    @ServusGameTest(timeoutTicks = 200)
    public static void millGrindsDarkMaltToGrist(GameTestHelper helper) {
        grinds(helper, ServusBreweryItems.DARK_MALT.get(), ServusBreweryItems.DARK_MALT_GRIST.get());
    }

    @ServusGameTest(timeoutTicks = 200)
    public static void millGrindsWheatMaltToGrist(GameTestHelper helper) {
        grinds(helper, ServusBreweryItems.WHEAT_MALT.get(), ServusBreweryItems.WHEAT_MALT_GRIST.get());
    }

    private static void grinds(GameTestHelper helper, Item malt, Item grist) {
        helper.assertTrue(ServusRecipes.byId(helper.getLevel(),
                Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "milling/" + itemPath(grist))).isPresent(),
                "Brewery should ship the milling recipe for " + itemPath(grist));
        helper.setBlock(MILL, MachineContent.MILL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        MillBlockEntity mill = helper.getBlockEntity(MILL, MillBlockEntity.class);
        mill.inventory().setStackInSlot(MillBlockEntity.SLOT_INPUT, new ItemStack(malt));
        helper.setBlock(MOTOR, MachineContent.ELECTRIC_MOTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.WEST));
        helper.getBlockEntity(MOTOR, ElectricMotorBlockEntity.class).energyStorage().setEnergy(8000);
        helper.succeedWhen(() -> {
            ItemStack output = mill.inventory().getStackInSlot(MillBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(mill.inventory().getStackInSlot(MillBlockEntity.SLOT_INPUT).isEmpty(), "the malt should be used up");
            helper.assertTrue(output.is(grist) && output.getCount() == 1, "one grist should be in the output, got " + output);
        });
    }

    // ---------- fixtures ----------

    private static MaltingFloorBlockEntity floor(GameTestHelper helper) {
        helper.setBlock(FLOOR, ServusBreweryBlocks.MALTING_FLOOR.get().defaultBlockState());
        return helper.getBlockEntity(FLOOR, MaltingFloorBlockEntity.class);
    }

    private static FloorContent content(GameTestHelper helper) {
        return helper.getBlockState(FLOOR).getValue(MaltingFloorBlock.CONTENT);
    }

    private static ItemView pipe(GameTestHelper helper) {
        return ItemView.of(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(FLOOR), Direction.UP));
    }

    private static GerminatingRecipe recipe(GameTestHelper helper, Item soaked) {
        return ServusBreweryRecipes.germinating(helper.getLevel(), new ItemStack(soaked)).orElseThrow();
    }

    private static Player holding(GameTestHelper helper, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static String itemPath(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
