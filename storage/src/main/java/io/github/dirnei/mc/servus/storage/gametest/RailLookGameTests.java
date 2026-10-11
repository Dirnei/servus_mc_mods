package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The track rails as wood blocks: the wood from the recipe stays on the placed rail, its drops and sneak placed runs. */
public final class RailLookGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos AT = new BlockPos(5, 2, 5);
    private static final Identifier SPRUCE = BuiltInRegistries.BLOCK.getKey(Blocks.SPRUCE_PLANKS);
    private static final Identifier BIRCH = BuiltInRegistries.BLOCK.getKey(Blocks.BIRCH_PLANKS);

    private RailLookGameTests() {
    }

    private static Player playerHolding(GameTestHelper helper, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static ItemStack woodRail(Item item, Identifier wood) {
        ItemStack stack = new ItemStack(item, 8);
        stack.set(WoodContent.WOOD.get(), wood);
        return stack;
    }

    /** Places the stack on top of the stone under AT. */
    private static void placeOnFloor(GameTestHelper helper, Player player, ItemStack stack) {
        BlockPos floor = helper.absolutePos(AT.below());
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
    }

    private static Identifier woodAt(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof WoodHolder holder ? holder.woodSlot().raw() : null;
    }

    private static CraftingRecipe recipe(GameTestHelper helper, String id) {
        return (CraftingRecipe) helper.getLevel().recipeAccess()
                .byKey(ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, id)))
                .orElseThrow(() -> new AssertionError("no " + id + " recipe")).value();
    }

    private static CraftingInput grid(Item... items) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : items) {
            stacks.add(item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item));
        }
        return CraftingInput.of(3, 3, stacks);
    }

    /** A spruce rail places spruce sleepers and drops a spruce rail; so does a farm rail. */
    @ServusGameTest(timeoutTicks = 20)
    public static void woodSurvivesPlaceAndBreak(GameTestHelper helper) {
        for (Item item : new Item[] {ServusStorageItems.RAIL.get(), ServusStorageItems.FARM_RAIL.get()}) {
            helper.setBlock(AT.below(), Blocks.STONE.defaultBlockState());
            helper.setBlock(AT, Blocks.AIR.defaultBlockState());
            ItemStack stack = woodRail(item, SPRUCE);
            placeOnFloor(helper, playerHolding(helper, stack), stack);
            BlockPos pos = helper.absolutePos(AT);
            helper.assertTrue(helper.getBlockState(AT).getBlock().asItem() == item, "the rail should be placed, got " + helper.getBlockState(AT));
            helper.assertTrue(SPRUCE.equals(woodAt(helper, AT)), "the placed rail is spruce, got " + woodAt(helper, AT));
            List<ItemStack> drops = Block.getDrops(helper.getBlockState(AT), helper.getLevel(), pos, helper.getLevel().getBlockEntity(pos),
                    null, new ItemStack(Items.IRON_PICKAXE));
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(item) && SPRUCE.equals(drops.get(0).get(WoodContent.WOOD.get())),
                    "the drop keeps spruce, got " + drops);
        }
        helper.succeed();
    }

    /** Sneaking with the carrot on a birch rail drops a birch rail. */
    @ServusGameTest(timeoutTicks = 40)
    public static void dismantleKeepsWood(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.STONE.defaultBlockState());
        ItemStack stack = woodRail(ServusStorageItems.RAIL.get(), BIRCH);
        placeOnFloor(helper, playerHolding(helper, stack), stack);
        helper.assertBlockPresent(ServusStorageBlocks.RAIL.get(), AT);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack carrot = new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, carrot);
        player.setShiftKeyDown(true);
        BlockPos absolute = helper.absolutePos(AT);
        carrot.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
        helper.runAfterDelay(2, () -> {
            helper.assertBlockPresent(Blocks.AIR, AT);
            List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(2));
            helper.assertTrue(items.size() == 1 && items.get(0).getItem().is(ServusStorageItems.RAIL.get())
                    && BIRCH.equals(items.get(0).getItem().get(WoodContent.WOOD.get())), "a birch rail should drop, got "
                    + items.stream().map(item -> item.getItem() + " " + item.getItem().get(WoodContent.WOOD.get())).toList());
            helper.succeed();
        });
    }

    /** Iron ingots around birch planks make sixteen birch rails. */
    @ServusGameTest(timeoutTicks = 20)
    public static void railRecipeRecordsWood(GameTestHelper helper) {
        CraftingRecipe recipe = recipe(helper, "rail");
        CraftingInput input = grid(
                Items.IRON_INGOT, Items.AIR, Items.IRON_INGOT,
                Items.IRON_INGOT, Items.BIRCH_PLANKS, Items.IRON_INGOT,
                Items.IRON_INGOT, Items.AIR, Items.IRON_INGOT);
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "the rail recipe should take birch planks");
        ItemStack result = recipe.assemble(input);
        helper.assertTrue(result.is(ServusStorageItems.RAIL.get()) && result.getCount() == 16
                && BIRCH.equals(result.get(WoodContent.WOOD.get())), "sixteen birch rails, got " + result + " " + result.get(WoodContent.WOOD.get()));
        helper.succeed();
    }

    /** Copper ingots around spruce planks make sixteen spruce farm rails; iron does not. */
    @ServusGameTest(timeoutTicks = 20)
    public static void farmRailRecipeTakesCopper(GameTestHelper helper) {
        CraftingRecipe recipe = recipe(helper, "farm_rail");
        CraftingInput copper = grid(
                Items.COPPER_INGOT, Items.AIR, Items.COPPER_INGOT,
                Items.COPPER_INGOT, Items.SPRUCE_PLANKS, Items.COPPER_INGOT,
                Items.COPPER_INGOT, Items.AIR, Items.COPPER_INGOT);
        helper.assertTrue(recipe.matches(copper, helper.getLevel()), "the farm rail recipe should take copper and planks");
        ItemStack result = recipe.assemble(copper);
        helper.assertTrue(result.is(ServusStorageItems.FARM_RAIL.get()) && result.getCount() == 16
                && SPRUCE.equals(result.get(WoodContent.WOOD.get())), "sixteen spruce farm rails, got " + result);
        CraftingInput iron = grid(
                Items.IRON_INGOT, Items.AIR, Items.IRON_INGOT,
                Items.IRON_INGOT, Items.SPRUCE_PLANKS, Items.IRON_INGOT,
                Items.IRON_INGOT, Items.AIR, Items.IRON_INGOT);
        helper.assertFalse(recipe.matches(iron, helper.getLevel()), "iron makes a Warehouse Rail, not a farm rail");
        helper.succeed();
    }

    /** A sneak placed run of birch rails puts birch on every rail it places. */
    @ServusGameTest(timeoutTicks = 40)
    public static void runKeepsWood(GameTestHelper helper) {
        for (int x = 0; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 1, 3), Blocks.STONE.defaultBlockState());
        }
        for (int x = 1; x <= 2; x++) {
            BlockPos rail = new BlockPos(x, 2, 3);
            helper.setBlock(rail, Block.updateFromNeighbourShapes(ServusStorageBlocks.RAIL.get().defaultBlockState(),
                    helper.getLevel(), helper.absolutePos(rail)));
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos clicked = helper.absolutePos(new BlockPos(1, 2, 3));
        for (int i = 0; i < 2; i++) {
            ItemStack stack = woodRail(ServusStorageItems.RAIL.get(), BIRCH);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.setShiftKeyDown(true);
            Vec3 location = new Vec3(clicked.getX() + 0.9, clicked.getY() + 2.0 / 16.0, clicked.getZ() + 0.5);
            stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(location, Direction.UP, clicked, false)));
            player.setShiftKeyDown(false);
        }
        for (int x = 3; x <= 4; x++) {
            BlockPos rail = new BlockPos(x, 2, 3);
            helper.assertBlockPresent(ServusStorageBlocks.RAIL.get(), rail);
            helper.assertTrue(BIRCH.equals(woodAt(helper, rail)), "the run's rail at x " + x + " is birch, got " + woodAt(helper, rail));
        }
        helper.succeed();
    }
}
