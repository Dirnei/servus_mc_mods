package io.github.dirnei.mc.servus.foundation.logistics.gametest;

import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.Component;
import io.github.dirnei.mc.servus.foundation.data.DataSummary;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctModelData;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.facade.FacadeItem;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ServusFoundationIds.MOD_ID)
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
        helper.assertTrue(slab.isEmpty() || !(slab.get().value() instanceof io.github.dirnei.mc.servus.foundation.logistics.facade.FacadeRecipe),
                "a slab must not make a facade");

        helper.setBlock(DUCT, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(DUCT);
        duct.addLine(Line.ITEM);
        duct.setPanel(Direction.NORTH, Blocks.STONE.defaultBlockState(), false);
        helper.assertTrue(duct.panel(Direction.NORTH) != null && duct.panel(Direction.NORTH).is(Blocks.STONE), "duct should carry the stone panel");
        ItemStack removed = duct.removePanel(Direction.NORTH);
        helper.assertTrue(removed.is(ServusLogisticsItems.FACADE.get()) && FacadeItem.blockOf(removed) == Blocks.STONE,
                "removing should give the stone facade back");
        helper.assertTrue(!duct.hasPanels(), "duct should have no panel after removal");
        helper.succeed();
    }

    /** Every example facade shown in the creative tab and in JEI comes from the facade recipe. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void facadeExamplesAreCraftable(GameTestHelper helper) {
        for (Block block : FacadeItem.EXAMPLES) {
            CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(block), new ItemStack(Items.IRON_INGOT)));
            Optional<RecipeHolder<CraftingRecipe>> recipe =
                    helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
            helper.assertTrue(recipe.isPresent(), "no facade recipe for " + block);
            ItemStack result = recipe.get().value().assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(result.is(ServusLogisticsItems.FACADE.get()) && result.getCount() == 6 && FacadeItem.blockOf(result) == block,
                    "six facades of " + block + " expected, got " + result);
        }
        helper.succeed();
    }

    // Facade panels

    /** Uses a facade stack on a face of the duct at {@code pos} as a survival player; returns what is left. */
    private static ItemStack usePanel(GameTestHelper helper, BlockPos pos, Direction face, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = helper.absolutePos(pos);
        Vec3 point = Vec3.atCenterOf(absolute).relative(face, 0.5);
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(point, face, absolute, false)));
        return stack;
    }

    private static int itemNetworkId(UtilityDuctBlockEntity duct) {
        return duct.network(Line.ITEM).map(network -> network.id()).orElse(-1);
    }

    /** A facade covers the clicked face only, and a covered face takes no second panel. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void panelOnClickedFace(GameTestHelper helper) {
        UtilityDuctBlockEntity duct = duct(helper, DUCT, Line.ITEM);
        ItemStack left = usePanel(helper, DUCT, Direction.NORTH, FacadeItem.of(Blocks.STONE, 2));
        helper.assertTrue(left.getCount() == 1, "one facade is used");
        helper.assertTrue(duct.panel(Direction.NORTH) != null && duct.panel(Direction.NORTH).is(Blocks.STONE), "north carries stone");
        for (Direction side : Direction.values()) {
            helper.assertTrue(side == Direction.NORTH || duct.panel(side) == null, "only the clicked face is covered, not " + side);
        }
        usePanel(helper, DUCT, Direction.NORTH, left);
        helper.assertTrue(left.getCount() == 1, "a covered face takes no second panel");
        helper.assertTrue(duct.panel(Direction.NORTH).is(Blocks.STONE), "the first panel stays");
        usePanel(helper, DUCT, Direction.UP, FacadeItem.of(Blocks.OAK_PLANKS, 1, true));
        helper.assertTrue(duct.panel(Direction.UP) != null && duct.isHollow(Direction.UP), "a hollow panel goes on top");
        helper.succeed();
    }

    /** A solid panel between two item ducts removes both arms and splits the network; taking it off joins them. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void solidPanelSplitsDucts(GameTestHelper helper) {
        UtilityDuctBlockEntity west = duct(helper, DUCT, Line.ITEM);
        UtilityDuctBlockEntity east = duct(helper, DUCT.east(), Line.ITEM);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(itemNetworkId(west) >= 0 && itemNetworkId(west) == itemNetworkId(east), "one network before the panel");
            west.setPanel(Direction.EAST, Blocks.STONE.defaultBlockState(), false);
        });
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(!west.hasArm(Line.ITEM, Direction.EAST) && !east.hasArm(Line.ITEM, Direction.WEST), "no arms through the panel");
            helper.assertTrue(itemNetworkId(west) != itemNetworkId(east), "two networks behind a solid panel");
            west.removePanel(Direction.EAST);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(west.hasArm(Line.ITEM, Direction.EAST) && east.hasArm(Line.ITEM, Direction.WEST), "the arms come back");
            helper.assertTrue(itemNetworkId(west) == itemNetworkId(east), "one network again");
            helper.succeed();
        });
    }

    /** A solid panel toward a chest removes the endpoint and the item handler on that side. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void solidPanelClosesEndpoint(GameTestHelper helper) {
        chest(helper, DUCT.north());
        UtilityDuctBlockEntity duct = duct(helper, DUCT, Line.ITEM);
        helper.assertTrue(duct.isEndpoint(Direction.NORTH), "the chest is an endpoint");
        duct.setPanel(Direction.NORTH, Blocks.STONE.defaultBlockState(), false);
        helper.assertTrue(!duct.isEndpoint(Direction.NORTH), "no endpoint behind the panel");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(DUCT), Direction.NORTH) == null,
                "no item handler on the covered side");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(DUCT), Direction.SOUTH) != null,
                "the other sides keep theirs");
        helper.succeed();
    }

    /** A hollow panel between two ducts keeps the arms and the network. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void hollowPanelKeepsNetwork(GameTestHelper helper) {
        UtilityDuctBlockEntity west = duct(helper, DUCT, Line.ITEM);
        UtilityDuctBlockEntity east = duct(helper, DUCT.east(), Line.ITEM);
        helper.runAfterDelay(2, () -> west.setPanel(Direction.EAST, Blocks.STONE.defaultBlockState(), true));
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(west.hasArm(Line.ITEM, Direction.EAST) && east.hasArm(Line.ITEM, Direction.WEST), "the arms pass the hole");
            helper.assertTrue(itemNetworkId(west) >= 0 && itemNetworkId(west) == itemNetworkId(east), "still one network");
            helper.succeed();
        });
    }

    /** Sneaking with the carrot on a panel takes off that panel only and keeps the duct. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void carrotRemovesOnePanel(GameTestHelper helper) {
        UtilityDuctBlockEntity duct = duct(helper, DUCT, Line.ITEM);
        duct.setPanel(Direction.NORTH, Blocks.STONE.defaultBlockState(), false);
        duct.setPanel(Direction.UP, Blocks.OAK_PLANKS.defaultBlockState(), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        ItemStack carrot = new ItemStack(ServusFoundationItems.CONFIGURATION_CARROT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, carrot);
        BlockPos absolute = helper.absolutePos(DUCT);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.NORTH, 0.5), Direction.NORTH, absolute, false);
        ServusLogisticsBlocks.UTILITY_DUCT.get().onWrench(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, carrot, hit),
                helper.getBlockState(DUCT));
        helper.assertTrue(helper.getBlockState(DUCT).is(ServusLogisticsBlocks.UTILITY_DUCT.get()), "the duct stays");
        helper.assertTrue(duct.panel(Direction.NORTH) == null, "the clicked panel is gone");
        helper.assertTrue(duct.panel(Direction.UP) != null, "the other panel stays");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(2));
        helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(ServusLogisticsItems.FACADE.get())
                && FacadeItem.blockOf(drops.get(0).getItem()) == Blocks.STONE, "a stone facade drops, got " + drops);
        helper.succeed();
    }

    /** Breaking a duct drops its line and both kinds of panel. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void breakingDropsPanels(GameTestHelper helper) {
        UtilityDuctBlockEntity duct = duct(helper, DUCT, Line.ITEM);
        duct.setPanel(Direction.NORTH, Blocks.STONE.defaultBlockState(), false);
        duct.setPanel(Direction.SOUTH, Blocks.BRICKS.defaultBlockState(), true);
        helper.destroyBlock(DUCT);
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(DUCT)).inflate(2));
        helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(ServusLogisticsItems.ITEM_PIPE.get())), "the item pipe drops");
        helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(ServusLogisticsItems.FACADE.get())
                && FacadeItem.blockOf(e.getItem()) == Blocks.STONE), "the stone facade drops");
        helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(ServusLogisticsItems.HOLLOW_FACADE.get())
                && FacadeItem.blockOf(e.getItem()) == Blocks.BRICKS), "the hollow brick facade drops");
        helper.succeed();
    }

    /** Eight stone facades around an empty centre give eight hollow stone facades; a mixed ring gives nothing. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void hollowRingRecipe(GameTestHelper helper) {
        List<ItemStack> ring = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ring.add(i == 4 ? ItemStack.EMPTY : FacadeItem.of(Blocks.STONE, 1));
        }
        CraftingInput input = CraftingInput.of(3, 3, ring);
        Optional<RecipeHolder<CraftingRecipe>> recipe =
                helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "the ring matches");
        ItemStack result = recipe.get().value().assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(ServusLogisticsItems.HOLLOW_FACADE.get()) && result.getCount() == 8 && FacadeItem.blockOf(result) == Blocks.STONE,
                "eight hollow stone facades expected, got " + result);
        ring.set(0, FacadeItem.of(Blocks.OAK_PLANKS, 1));
        Optional<RecipeHolder<CraftingRecipe>> mixed =
                helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, ring), helper.getLevel());
        helper.assertTrue(mixed.isEmpty(), "a mixed ring makes nothing");
        helper.succeed();
    }

    /** A Data Cable adds the data line to an item duct; two data ducts form one network with no members. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void dataLineOnAnItemDuct(GameTestHelper helper) {
        UtilityDuctBlockEntity first = duct(helper, DUCT, Line.ITEM);
        UtilityDuctBlockEntity second = duct(helper, DUCT.east(), Line.DATA);
        helper.assertTrue(first.addLine(Line.DATA), "the data line should be added to the item duct");
        helper.assertTrue(first.has(Line.ITEM) && first.has(Line.DATA), "the duct carries both lines");
        helper.runAfterDelay(2, () -> {
            DataNetwork network = DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(DUCT)).orElse(null);
            helper.assertTrue(network != null && network == second.dataNetwork().orElse(null), "both ducts share one data network");
            helper.assertTrue(first.hasArm(Line.DATA, Direction.EAST), "a data arm links the two ducts");
            Component summary = DataSummary.of(network);
            helper.assertTrue(summary.getContents() instanceof TranslatableContents contents
                            && java.util.Arrays.equals(contents.getArgs(), new Object[] {2, 0, 0}),
                    "summary should say 2 cables, 0 producers, 0 stock, got " + summary.getString());
            helper.succeed();
        });
    }

    // Fluid Pipe: vanilla cauldrons are the fixtures, 1,000 mB each through NeoForge's cauldron wrapper.

    private static void waterCauldron(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
    }

    private static void emptyCauldron(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CAULDRON.defaultBlockState());
    }

    private static FluidStack fluid(GameTestHelper helper, BlockPos pos) {
        IFluidHandler handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), null);
        return handler == null ? FluidStack.EMPTY : handler.getFluidInTank(0);
    }

    private static boolean holds(GameTestHelper helper, BlockPos pos, Fluid fluid, int amount) {
        FluidStack stack = fluid(helper, pos);
        return amount == 0 ? stack.isEmpty() : stack.is(fluid) && stack.getAmount() == amount;
    }

    private static String describe(GameTestHelper helper, BlockPos pos) {
        FluidStack stack = fluid(helper, pos);
        return stack.isEmpty() ? "nothing" : stack.getAmount() + " mB of " + stack.getFluidHolder().getRegisteredName();
    }

    /** A water cauldron on a pull in side empties into an empty cauldron on a default side, the whole 1,000 mB at once. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void fluidFlowsFromPullInToDefaultSide(GameTestHelper helper) {
        waterCauldron(helper, CHEST_A);
        emptyCauldron(helper, CHEST_C);
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.FLUID);
        duct(helper, DUCT_B, Line.FLUID);
        duct(helper, DUCT_C, Line.FLUID);
        a.setMode(Line.FLUID, Direction.NORTH, SideMode.INPUT);
        helper.assertTrue(a.hasArm(Line.FLUID, Direction.NORTH), "the fluid line should reach the cauldron");

        helper.runAfterDelay(3, () -> {
            helper.assertTrue(holds(helper, CHEST_C, Fluids.WATER, 1000), "the sink cauldron should hold 1000 mB of water, got " + describe(helper, CHEST_C));
            helper.assertTrue(holds(helper, CHEST_A, Fluids.WATER, 0), "the source cauldron should be empty, got " + describe(helper, CHEST_A));
            helper.succeed();
        });
    }

    /** A full and an empty cauldron on default sides: the water moves once and never back. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void fluidDoesNotRunBackOnDefaultSides(GameTestHelper helper) {
        waterCauldron(helper, CHEST_A);
        emptyCauldron(helper, CHEST_C);
        duct(helper, DUCT_A, Line.FLUID);
        duct(helper, DUCT_B, Line.FLUID);
        duct(helper, DUCT_C, Line.FLUID);

        Runnable settled = () -> {
            helper.assertTrue(holds(helper, CHEST_C, Fluids.WATER, 1000), "the empty cauldron should have filled, got " + describe(helper, CHEST_C));
            helper.assertTrue(holds(helper, CHEST_A, Fluids.WATER, 0), "the full cauldron should have emptied, got " + describe(helper, CHEST_A));
        };
        helper.runAfterDelay(10, settled);
        helper.runAfterDelay(11, settled);
        helper.runAfterDelay(20, () -> {
            settled.run();
            helper.succeed();
        });
    }

    /** Only the cauldron on the pull in side's channel receives the water. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void fluidStaysOnItsChannel(GameTestHelper helper) {
        waterCauldron(helper, CHEST_A);
        emptyCauldron(helper, CHEST_B);
        emptyCauldron(helper, CHEST_C);
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.FLUID);
        UtilityDuctBlockEntity b = duct(helper, DUCT_B, Line.FLUID);
        duct(helper, DUCT_C, Line.FLUID);
        a.setMode(Line.FLUID, Direction.NORTH, SideMode.INPUT);
        a.setChannel(Line.FLUID, Direction.NORTH, 1);
        b.setChannel(Line.FLUID, Direction.NORTH, 1);

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(holds(helper, CHEST_B, Fluids.WATER, 1000), "the cauldron on channel 2 should hold the water, got " + describe(helper, CHEST_B));
            helper.assertTrue(holds(helper, CHEST_C, Fluids.WATER, 0), "the cauldron on channel 1 must stay empty, got " + describe(helper, CHEST_C));
            helper.succeed();
        });
    }

    /** Water and lava on one channel: each empty cauldron ends up with one whole fluid, never a mix, and both sources empty. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void mixedFluidsReachMatchingSinks(GameTestHelper helper) {
        BlockPos ductD = DUCT_C.east();
        waterCauldron(helper, CHEST_A);
        helper.setBlock(CHEST_B, Blocks.LAVA_CAULDRON.defaultBlockState());
        emptyCauldron(helper, CHEST_C);
        emptyCauldron(helper, ductD.north());
        UtilityDuctBlockEntity a = duct(helper, DUCT_A, Line.FLUID);
        UtilityDuctBlockEntity b = duct(helper, DUCT_B, Line.FLUID);
        duct(helper, DUCT_C, Line.FLUID);
        duct(helper, ductD, Line.FLUID);
        a.setMode(Line.FLUID, Direction.NORTH, SideMode.INPUT);
        b.setMode(Line.FLUID, Direction.NORTH, SideMode.INPUT);

        helper.runAfterDelay(10, () -> {
            boolean waterThenLava = holds(helper, CHEST_C, Fluids.WATER, 1000) && holds(helper, ductD.north(), Fluids.LAVA, 1000);
            boolean lavaThenWater = holds(helper, CHEST_C, Fluids.LAVA, 1000) && holds(helper, ductD.north(), Fluids.WATER, 1000);
            helper.assertTrue(waterThenLava || lavaThenWater, "each sink should hold one whole fluid, got "
                    + describe(helper, CHEST_C) + " and " + describe(helper, ductD.north()));
            helper.assertTrue(fluid(helper, CHEST_A).isEmpty() && fluid(helper, CHEST_B).isEmpty(), "both sources should be empty");
            helper.succeed();
        });
    }

    /** Fluid filled into a duct side goes straight to the cauldron on its channel; a full cauldron refuses more. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void fluidPushedIntoADuctPassesOn(GameTestHelper helper) {
        emptyCauldron(helper, CHEST_C);
        duct(helper, DUCT_A, Line.FLUID);
        duct(helper, DUCT_B, Line.FLUID);
        duct(helper, DUCT_C, Line.FLUID);

        helper.runAfterDelay(2, () -> {
            IFluidHandler duct = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(DUCT_A), Direction.NORTH);
            helper.assertTrue(duct != null, "the duct should expose a fluid handler on an in and out side");
            FluidStack water = new FluidStack(Fluids.WATER, 1000);
            helper.assertTrue(duct.fill(water, IFluidHandler.FluidAction.SIMULATE) == 1000, "a simulated fill should report room for 1000 mB");
            helper.assertTrue(holds(helper, CHEST_C, Fluids.WATER, 0), "a simulated fill must not move fluid");
            int filled = duct.fill(water, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 1000, "the duct should accept 1000 mB, got " + filled);
            helper.assertTrue(holds(helper, CHEST_C, Fluids.WATER, 1000), "the cauldron should hold the water, got " + describe(helper, CHEST_C));
            helper.assertTrue(duct.fill(water, IFluidHandler.FluidAction.EXECUTE) == 0, "with every sink full the duct accepts nothing");
            helper.assertTrue(duct.getTankCapacity(0) == 0 && duct.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                    "the duct keeps and gives nothing");

            UtilityDuctBlockEntity a = helper.getBlockEntity(DUCT_A);
            a.setMode(Line.FLUID, Direction.NORTH, SideMode.OUTPUT);
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(DUCT_A), Direction.NORTH) == null,
                    "a push out side exposes no fluid handler");
            helper.succeed();
        });
    }

    /** A duct with all three lines is the thickest size and drops one item per line. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void threeLineDuctDropsThreeItems(GameTestHelper helper) {
        UtilityDuctBlockEntity duct = duct(helper, DUCT, Line.ITEM, Line.ENERGY);
        AABB two = helper.getBlockState(DUCT).getShape(helper.getLevel(), helper.absolutePos(DUCT)).bounds();
        helper.assertTrue(duct.addLine(Line.FLUID), "the fluid line should be added to the two line duct");
        helper.assertTrue(duct.lines().size() == 3, "the duct should carry three lines");
        AABB three = helper.getBlockState(DUCT).getShape(helper.getLevel(), helper.absolutePos(DUCT)).bounds();
        helper.assertTrue(Math.abs(two.minX - 5 / 16.0) < 1e-6 && Math.abs(three.minX - 4 / 16.0) < 1e-6,
                "the centre should grow from 6 to 8 pixels, got " + two + " and " + three);

        helper.destroyBlock(DUCT);
        helper.assertItemEntityPresent(ServusLogisticsItems.ITEM_PIPE.get(), DUCT, 2);
        helper.assertItemEntityPresent(ServusLogisticsItems.POWER_CABLE.get(), DUCT, 2);
        helper.assertItemEntityPresent(ServusLogisticsItems.FLUID_PIPE.get(), DUCT, 2);
        helper.succeed();
    }

    /** Uses a line item on a face of the block at {@code pos}, like a player's right click. */
    private static void useLine(GameTestHelper helper, Player player, Item item, BlockPos pos, Direction face) {
        BlockPos absolute = helper.absolutePos(pos);
        Vec3 point = Vec3.atCenterOf(absolute).relative(face, 0.5);
        ItemStack stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(point, face, absolute, false)));
    }

    /**
     * A line item on a duct that already carries the line places a new duct against the face;
     * a different line is added; sneaking always places.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void lineItemPlacesBesideAFullDuct(GameTestHelper helper) {
        duct(helper, DUCT, Line.ITEM);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        useLine(helper, player, ServusLogisticsItems.ITEM_PIPE.get(), DUCT, Direction.EAST);
        UtilityDuctBlockEntity east = helper.getBlockEntity(DUCT.east());
        helper.assertTrue(east.has(Line.ITEM), "an Item Pipe on an item duct places a new duct to the east");

        useLine(helper, player, ServusLogisticsItems.POWER_CABLE.get(), DUCT, Direction.EAST);
        UtilityDuctBlockEntity duct = helper.getBlockEntity(DUCT);
        helper.assertTrue(duct.has(Line.ENERGY), "a Power Cable is added to a duct that lacks it");
        helper.assertBlockNotPresent(ServusLogisticsBlocks.UTILITY_DUCT.get(), DUCT.east().east());

        player.setShiftKeyDown(true);
        useLine(helper, player, ServusLogisticsItems.FLUID_PIPE.get(), DUCT, Direction.UP);
        UtilityDuctBlockEntity above = helper.getBlockEntity(DUCT.above());
        helper.assertTrue(above.has(Line.FLUID), "a sneak click places a new Fluid Pipe duct above");
        helper.assertFalse(duct.has(Line.FLUID), "a sneak click does not add the line to the clicked duct");
        helper.succeed();
    }

    /** Sneaking walks the run of ducts carrying the held line: east along a row, along it from its top face, up a column, and not into another line. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void sneakLineExtendsRun(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int x = 1; x <= 6; x++) {
            duct(helper, new BlockPos(x, 1, 9), Line.ITEM);
        }
        for (int y = 1; y <= 4; y++) {
            duct(helper, new BlockPos(9, y, 2), Line.ENERGY);
        }
        duct(helper, new BlockPos(1, 1, 11), Line.FLUID);
        duct(helper, new BlockPos(2, 1, 11), Line.ITEM);
        player.setShiftKeyDown(true);

        useLine(helper, player, ServusLogisticsItems.ITEM_PIPE.get(), new BlockPos(2, 1, 9), Direction.EAST);
        UtilityDuctBlockEntity end = helper.getBlockEntity(new BlockPos(7, 1, 9));
        helper.assertTrue(end.has(Line.ITEM), "the new Item Pipe duct should land past the sixth duct");
        helper.assertTrue(helper.<UtilityDuctBlockEntity>getBlockEntity(new BlockPos(2, 1, 9)).lines().size() == 1,
                "the clicked duct should stay unchanged");

        BlockPos fourth = helper.absolutePos(new BlockPos(4, 1, 9));
        ItemStack pipe = new ItemStack(ServusLogisticsItems.ITEM_PIPE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, pipe);
        pipe.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, pipe,
                new BlockHitResult(new Vec3(fourth.getX() + 0.7, fourth.getY() + 0.75, fourth.getZ() + 0.5), Direction.UP, fourth, false)));
        helper.assertTrue(helper.<UtilityDuctBlockEntity>getBlockEntity(new BlockPos(8, 1, 9)).has(Line.ITEM),
                "a hit on the top of a straight pipe east of centre should land past the east end");
        helper.assertBlockNotPresent(ServusLogisticsBlocks.UTILITY_DUCT.get(), new BlockPos(4, 2, 9));

        useLine(helper, player, ServusLogisticsItems.POWER_CABLE.get(), new BlockPos(9, 1, 2), Direction.UP);
        UtilityDuctBlockEntity top = helper.getBlockEntity(new BlockPos(9, 5, 2));
        helper.assertTrue(top.has(Line.ENERGY), "the new Power Cable duct should land on top of the column");

        BlockPos fluid = helper.absolutePos(new BlockPos(1, 1, 11));
        ItemStack held = new ItemStack(ServusLogisticsItems.FLUID_PIPE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        held.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, held,
                new BlockHitResult(Vec3.atCenterOf(fluid).relative(Direction.EAST, 0.5), Direction.EAST, fluid, false)));
        helper.assertTrue(held.getCount() == 1, "a run blocked by another line keeps the stack");
        helper.assertFalse(helper.<UtilityDuctBlockEntity>getBlockEntity(new BlockPos(2, 1, 11)).has(Line.FLUID),
                "the Item Pipe duct in the way gets no Fluid Pipe");
        helper.assertBlockNotPresent(ServusLogisticsBlocks.UTILITY_DUCT.get(), new BlockPos(3, 1, 11));
        helper.succeed();
    }
}
