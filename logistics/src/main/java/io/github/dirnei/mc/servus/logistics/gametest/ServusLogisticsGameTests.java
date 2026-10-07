package io.github.dirnei.mc.servus.logistics.gametest;

import io.github.dirnei.mc.servus.lib.block.ConnectableBlock;
import io.github.dirnei.mc.servus.lib.side.SideMode;
import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.duct.DuctModelData;
import io.github.dirnei.mc.servus.logistics.duct.Line;
import io.github.dirnei.mc.servus.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.logistics.facade.FacadeItem;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ServusLogisticsIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ServusLogisticsGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos DUCT = new BlockPos(3, 1, 3);

    /** Three ducts in a row along x with a chest north of each. */
    private static final BlockPos DUCT_A = new BlockPos(2, 1, 6);
    private static final BlockPos DUCT_B = new BlockPos(3, 1, 6);
    private static final BlockPos DUCT_C = new BlockPos(4, 1, 6);
    private static final BlockPos CHEST_A = DUCT_A.north();
    private static final BlockPos CHEST_B = DUCT_B.north();
    private static final BlockPos CHEST_C = DUCT_C.north();

    private ServusLogisticsGameTests() {
    }

    /**
     * Places a Utility Duct carrying the given lines with default modes and
     * channel 1. {@code setBlock} skips placement logic, so the connection
     * flags are refreshed from the neighbours afterwards.
     */
    private static UtilityDuctBlockEntity duct(GameTestHelper helper, BlockPos pos, Line... lines) {
        helper.setBlock(pos, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos);
        for (Line line : lines) {
            duct.addLine(line);
        }
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
        return duct;
    }

    private static ChestBlockEntity chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST.defaultBlockState());
        return helper.getBlockEntity(pos);
    }

    private static int count(GameTestHelper helper, BlockPos pos, Item item) {
        ChestBlockEntity chest = helper.getBlockEntity(pos);
        int total = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Chest A pulls into channel 1; chest B on channel 1 gets everything, chest C on channel 2 nothing. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void itemsStayOnTheirChannel(GameTestHelper helper) {
        chest(helper, CHEST_A).setItem(0, new ItemStack(Items.COBBLESTONE, 20));
        chest(helper, CHEST_B);
        chest(helper, CHEST_C);
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.ITEM);
        duct(helper, DUCT_B, Line.ITEM);
        UtilityDuctBlockEntity c = duct(helper, DUCT_C, Line.ITEM);
        a.setMode(Line.ITEM, Direction.NORTH, SideMode.INPUT);
        c.setChannel(Line.ITEM, Direction.NORTH, 1);

        helper.runAfterDelay(80, () -> {
            helper.assertTrue(count(helper, CHEST_B, Items.COBBLESTONE) == 20,
                    "chest B on channel 1 should hold all 20 cobblestone, got " + count(helper, CHEST_B, Items.COBBLESTONE));
            helper.assertTrue(count(helper, CHEST_C, Items.COBBLESTONE) == 0, "chest C on channel 2 must receive nothing");
            helper.assertTrue(count(helper, CHEST_A, Items.COBBLESTONE) == 0, "chest A should be emptied");
            helper.succeed();
        });
    }

    /** The pull in side's filter lists cobblestone only; dirt stays, and the other channel sees nothing. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void pullInHonoursFilterAndChannel(GameTestHelper helper) {
        ChestBlockEntity source = chest(helper, CHEST_A);
        source.setItem(0, new ItemStack(Items.COBBLESTONE, 10));
        source.setItem(1, new ItemStack(Items.DIRT, 10));
        chest(helper, CHEST_B);
        chest(helper, CHEST_C);
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.ITEM);
        duct(helper, DUCT_B, Line.ITEM);
        UtilityDuctBlockEntity c = duct(helper, DUCT_C, Line.ITEM);
        a.filter(Direction.NORTH).setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        a.setMode(Line.ITEM, Direction.NORTH, SideMode.INPUT);
        c.setChannel(Line.ITEM, Direction.NORTH, 1);

        helper.runAfterDelay(80, () -> {
            helper.assertTrue(count(helper, CHEST_B, Items.COBBLESTONE) == 10, "chest B should receive the 10 cobblestone");
            helper.assertTrue(count(helper, CHEST_B, Items.DIRT) == 0, "dirt must not pass the filter");
            helper.assertTrue(count(helper, CHEST_A, Items.DIRT) == 10, "dirt stays in chest A");
            helper.assertTrue(count(helper, CHEST_C, Items.COBBLESTONE) == 0 && count(helper, CHEST_C, Items.DIRT) == 0,
                    "chest C on channel 2 must receive nothing");
            helper.succeed();
        });
    }

    /**
     * A two line duct with a chest east, a one line duct west and stone north:
     * the item line reaches both neighbours, the energy line none; adding the
     * energy line to the west duct and removing the chest update the masks.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void armsFollowLinesAndNeighbours(GameTestHelper helper) {
        BlockPos centre = new BlockPos(6, 1, 6);
        BlockPos chestPos = centre.east();
        BlockPos westPos = centre.west();
        helper.setBlock(centre.north(), Blocks.STONE.defaultBlockState());
        chest(helper, chestPos);
        UtilityDuctBlockEntity west = duct(helper, westPos, Line.ITEM);
        UtilityDuctBlockEntity duct = duct(helper, centre, Line.ITEM, Line.ENERGY);
        int east = DuctModelData.bit(Direction.EAST);
        int westBit = DuctModelData.bit(Direction.WEST);

        helper.assertTrue(duct.arms(Line.ITEM) == (east | westBit), "item arms should reach the chest and the west duct, got " + duct.arms(Line.ITEM));
        helper.assertTrue(duct.arms(Line.ENERGY) == 0, "no energy arm: the chest has none and the west duct lacks the line, got " + duct.arms(Line.ENERGY));
        helper.assertTrue(duct.endpoints() == east, "only the chest side is an endpoint, got " + duct.endpoints());
        helper.assertTrue(west.arms(Line.ITEM) == east && west.endpoints() == 0, "the west duct has one item arm toward the centre and no endpoint");
        BlockState state = helper.getBlockState(centre);
        helper.assertTrue(state.getValue(ConnectableBlock.EAST) && state.getValue(ConnectableBlock.WEST) && !state.getValue(ConnectableBlock.NORTH),
                "block state flags follow the union of the arms");

        west.addLine(Line.ENERGY);
        helper.assertTrue(duct.arms(Line.ENERGY) == westBit, "the energy arm should appear toward the west duct, got " + duct.arms(Line.ENERGY));

        duct.setMode(Line.ITEM, Direction.EAST, SideMode.DISABLED);
        helper.assertTrue(duct.arms(Line.ITEM) == westBit && duct.endpoints() == 0, "a disabled side has no arm and is no endpoint");
        duct.setMode(Line.ITEM, Direction.EAST, SideMode.BOTH);

        helper.setBlock(chestPos, Blocks.AIR.defaultBlockState());
        helper.assertTrue(duct.arms(Line.ITEM) == westBit, "the item arm toward the removed chest should vanish, got " + duct.arms(Line.ITEM));
        helper.assertTrue(duct.endpoints() == 0, "no endpoint without the chest, got " + duct.endpoints());
        helper.assertTrue(!helper.getBlockState(centre).getValue(ConnectableBlock.EAST), "the east flag should clear");
        helper.succeed();
    }

    /** Stone plus iron gives six stone facades, a slab gives nothing, and a duct takes and returns a facade. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void facadeRecipeAndApplication(GameTestHelper helper) {
        Optional<RecipeHolder<CraftingRecipe>> stone = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                CraftingInput.of(2, 1, List.of(new ItemStack(Items.STONE), new ItemStack(Items.IRON_INGOT))), helper.getLevel());
        helper.assertTrue(stone.isPresent(), "stone plus iron should match the facade recipe");
        ItemStack result = stone.get().value().assemble(
                CraftingInput.of(2, 1, List.of(new ItemStack(Items.STONE), new ItemStack(Items.IRON_INGOT))),
                helper.getLevel().registryAccess());
        helper.assertTrue(result.is(ServusLogisticsItems.FACADE.get()) && result.getCount() == 6,
                "result should be six facades");
        helper.assertTrue(FacadeItem.blockOf(result) == Blocks.STONE, "facade should remember stone");

        Optional<RecipeHolder<CraftingRecipe>> slab = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                CraftingInput.of(2, 1, List.of(new ItemStack(Items.STONE_SLAB), new ItemStack(Items.IRON_INGOT))), helper.getLevel());
        helper.assertTrue(slab.isEmpty() || !(slab.get().value() instanceof io.github.dirnei.mc.servus.logistics.facade.FacadeRecipe),
                "a slab must not make a facade");

        helper.setBlock(DUCT, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(DUCT);
        duct.addLine(Line.ITEM);
        duct.setFacade(Blocks.STONE.defaultBlockState());
        helper.assertTrue(duct.facade() != null && duct.facade().is(Blocks.STONE), "duct should carry the stone facade");
        ItemStack removed = duct.removeFacade();
        helper.assertTrue(removed.is(ServusLogisticsItems.FACADE.get()) && FacadeItem.blockOf(removed) == Blocks.STONE,
                "removing should give the stone facade back");
        helper.assertTrue(duct.facade() == null, "duct should have no facade after removal");
        helper.succeed();
    }
}
