package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.wood.Wood;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlock;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The wood from the recipe and the Pattern Storage lights. */
public final class BlockStyleGameTests {

    private static final BlockPos AT = new BlockPos(5, 1, 5);
    private static final Identifier BIRCH = BuiltInRegistries.BLOCK.getKey(Blocks.BIRCH_PLANKS);
    private static final Identifier SPRUCE = BuiltInRegistries.BLOCK.getKey(Blocks.SPRUCE_PLANKS);
    private static final Identifier ACACIA = BuiltInRegistries.BLOCK.getKey(Blocks.ACACIA_PLANKS);

    private BlockStyleGameTests() {
    }

    private static ItemStack craft(GameTestHelper helper, String recipe, int width, int height, List<ItemStack> grid) {
        RecipeHolder<?> holder = helper.getLevel().recipeAccess()
                .byKey(ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, recipe)))
                .orElseThrow(() -> new AssertionError("no recipe " + recipe));
        CraftingRecipe crafting = (CraftingRecipe) holder.value();
        CraftingInput input = CraftingInput.of(width, height, grid);
        helper.assertTrue(crafting.matches(input, helper.getLevel()), recipe + " should match its grid");
        return crafting.assemble(input);
    }

    private static List<ItemStack> grid(Item... items) {
        List<ItemStack> list = new ArrayList<>();
        for (Item item : items) {
            list.add(item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item));
        }
        return list;
    }

    /** The Storage Terminal crafted with birch planks carries birch. */
    @ServusGameTest(timeoutTicks = 20)
    public static void woodRecipeRecordsBirch(GameTestHelper helper) {
        ItemStack result = craft(helper, "storage_terminal", 3, 3, grid(
                Items.GLASS, Items.GLASS, Items.GLASS,
                Items.IRON_INGOT, Items.CHEST, Items.IRON_INGOT,
                Items.BIRCH_PLANKS, Items.BIRCH_PLANKS, Items.BIRCH_PLANKS));
        helper.assertTrue(result.is(ServusStorageItems.STORAGE_TERMINAL.get()), "a Storage Terminal");
        helper.assertTrue(BIRCH.equals(result.get(WoodContent.WOOD.get())), "birch planks give birch, got " + result.get(WoodContent.WOOD.get()));
        helper.succeed();
    }

    /** Two spruce and one oak plank give spruce. */
    @ServusGameTest(timeoutTicks = 20)
    public static void woodRecipeTakesTheMajority(GameTestHelper helper) {
        ItemStack result = craft(helper, "shuttle_garage", 3, 2, grid(
                Items.IRON_INGOT, ServusStorageItems.RAIL.get(), Items.IRON_INGOT,
                Items.SPRUCE_PLANKS, Items.OAK_PLANKS, Items.SPRUCE_PLANKS));
        helper.assertTrue(SPRUCE.equals(result.get(WoodContent.WOOD.get())), "the majority wins, got " + result.get(WoodContent.WOOD.get()));
        helper.succeed();
    }

    /** A birch item places a birch block, and its drop keeps birch. */
    @ServusGameTest(timeoutTicks = 20)
    public static void woodSurvivesPlaceAndBreak(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.STONE.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(ServusStorageItems.PRODUCTION_TERMINAL.get());
        stack.set(WoodContent.WOOD.get(), BIRCH);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos floor = helper.absolutePos(AT.below());
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
        helper.assertBlockPresent(ServusStorageBlocks.PRODUCTION_TERMINAL.get(), AT);
        WoodHolder placed = (WoodHolder) helper.getLevel().getBlockEntity(helper.absolutePos(AT));
        helper.assertTrue(BIRCH.equals(placed.woodSlot().wood()), "the placed block is birch, got " + placed.woodSlot().wood());
        BlockPos pos = helper.absolutePos(AT);
        List<ItemStack> drops = Block.getDrops(helper.getBlockState(AT), helper.getLevel(), pos, helper.getLevel().getBlockEntity(pos),
                null, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(drops.size() == 1 && BIRCH.equals(drops.get(0).get(WoodContent.WOOD.get())),
                "the drop keeps birch, got " + drops);
        helper.succeed();
    }

    /** Five acacia planks give an acacia Shuttle Workshop. */
    @ServusGameTest(timeoutTicks = 20)
    public static void workshopRecipeRecordsAcacia(GameTestHelper helper) {
        ItemStack result = craft(helper, "shuttle_workshop", 3, 3, grid(
                Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT,
                Items.ACACIA_PLANKS, Items.CRAFTING_TABLE, Items.ACACIA_PLANKS,
                Items.ACACIA_PLANKS, Items.ACACIA_PLANKS, Items.ACACIA_PLANKS));
        helper.assertTrue(result.is(ServusStorageItems.SHUTTLE_WORKSHOP.get()), "a Shuttle Workshop");
        helper.assertTrue(ACACIA.equals(result.get(WoodContent.WOOD.get())), "acacia planks give acacia, got " + result.get(WoodContent.WOOD.get()));
        helper.succeed();
    }

    /** A birch workshop item places a birch workshop, and breaking it drops a birch workshop. */
    @ServusGameTest(timeoutTicks = 20)
    public static void workshopWoodSurvivesBreak(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.STONE.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(ServusStorageItems.SHUTTLE_WORKSHOP.get());
        stack.set(WoodContent.WOOD.get(), BIRCH);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos floor = helper.absolutePos(AT.below());
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
        helper.assertBlockPresent(ServusStorageBlocks.SHUTTLE_WORKSHOP.get(), AT);
        WoodHolder placed = (WoodHolder) helper.getLevel().getBlockEntity(helper.absolutePos(AT));
        helper.assertTrue(BIRCH.equals(placed.woodSlot().wood()), "the placed workshop is birch, got " + placed.woodSlot().wood());
        BlockPos pos = helper.absolutePos(AT);
        List<ItemStack> drops = Block.getDrops(helper.getBlockState(AT), helper.getLevel(), pos, helper.getLevel().getBlockEntity(pos),
                null, new ItemStack(Items.IRON_AXE));
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(ServusStorageItems.SHUTTLE_WORKSHOP.get())
                        && BIRCH.equals(drops.get(0).get(WoodContent.WOOD.get())),
                "the drop is a birch workshop, got " + drops);
        helper.succeed();
    }

    /** Dismantling a Packing Station with the carrot drops it with its wood. */
    @ServusGameTest(timeoutTicks = 20)
    public static void woodSurvivesDismantle(GameTestHelper helper) {
        helper.setBlock(AT, ServusStorageBlocks.PACKING_STATION.get().defaultBlockState());
        ((WoodHolder) helper.getLevel().getBlockEntity(helper.absolutePos(AT))).woodSlot().set(SPRUCE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        GameTestSteps.useCarrot(helper, player, AT, Direction.NORTH);
        BlockPos pos = helper.absolutePos(AT);
        helper.assertBlockNotPresent(ServusStorageBlocks.PACKING_STATION.get(), AT);
        List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1),
                entity -> entity.getItem().is(ServusStorageItems.PACKING_STATION.get()));
        helper.assertTrue(items.size() == 1 && SPRUCE.equals(Wood.of(items.get(0).getItem())), "the dismantled item keeps spruce, got " + items);
        helper.succeed();
    }

    /** The Pattern Storage lights follow saved and deleted patterns. */
    @ServusGameTest(timeoutTicks = 20)
    public static void patternLightsFollowPatterns(GameTestHelper helper) {
        PatternStorageBlockEntity storage = StationJobs.patternStorage(helper, AT);
        helper.assertTrue(lights(helper) == 0, "an empty storage is dark");
        storage.set(0, StationJobs.sticks());
        storage.set(1, StationJobs.sticks());
        helper.assertTrue(lights(helper) == 2, "two patterns light two lights, got " + lights(helper));
        storage.clear(0);
        helper.assertTrue(lights(helper) == 1, "deleting one leaves one lit, got " + lights(helper));
        helper.succeed();
    }

    private static int lights(GameTestHelper helper) {
        BlockState state = helper.getBlockState(AT);
        return state.getValue(PatternStorageBlock.LIGHTS);
    }
}
