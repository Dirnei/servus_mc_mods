package io.github.dirnei.mc.servus.foundation.logistics.gametest;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlock;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankItem;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankRecipe;
import io.github.dirnei.mc.servus.foundation.logistics.tank.TankStatsManager;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GameTests of the Fluid Tank: forming, splitting, one fluid rule, buckets and drops. */
@GameTestHolder(ServusFoundationIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FluidTankGameTests {

    private static final String EMPTY = "empty_12x6x12";

    private FluidTankGameTests() {
    }

    private static int cap() {
        return TankStatsManager.INSTANCE.capacityPerBlock();
    }

    private static void tank(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusLogisticsBlocks.FLUID_TANK.get().defaultBlockState());
    }

    /** Places tank blocks filling the box from {@code min} to {@code max}, both inclusive. */
    private static void box(GameTestHelper helper, BlockPos min, BlockPos max) {
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            tank(helper, pos.immutable());
        }
    }

    private static IFluidHandler handler(GameTestHelper helper, BlockPos pos) {
        IFluidHandler handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(handler != null, "tank at " + pos.toShortString() + " should expose a fluid handler");
        return handler;
    }

    private static FluidTankBlockEntity entity(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos);
    }

    private static FluidStack water(int amount) {
        return new FluidStack(Fluids.WATER, amount);
    }

    private static void assertCapacity(GameTestHelper helper, BlockPos pos, int expected) {
        int capacity = handler(helper, pos).getTankCapacity(0);
        helper.assertTrue(capacity == expected, "tank at " + pos.toShortString() + " should report " + expected + " mB, got " + capacity);
    }

    private static void assertAmount(GameTestHelper helper, BlockPos pos, int expected) {
        int amount = handler(helper, pos).getFluidInTank(0).getAmount();
        helper.assertTrue(amount == expected, "tank at " + pos.toShortString() + " should hold " + expected + " mB, got " + amount);
    }

    private static void assertShare(GameTestHelper helper, BlockPos pos, int expected) {
        int amount = entity(helper, pos).share().getAmount();
        helper.assertTrue(amount == expected, "block at " + pos.toShortString() + " should hold a share of " + expected + " mB, got " + amount);
    }

    private static boolean joined(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getBlockState(pos).getValue(FluidTankBlock.property(side));
    }

    private static boolean anyJoined(GameTestHelper helper, BlockPos pos) {
        for (Direction side : Direction.values()) {
            if (joined(helper, pos, side)) {
                return true;
            }
        }
        return false;
    }

    private static InteractionResult placeItem(GameTestHelper helper, Player player, BlockPos pos, ItemStack stack) {
        BlockPos absolute = helper.absolutePos(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
        return ((FluidTankItem) stack.getItem()).place(context);
    }

    /** Two stacked blocks form one tank; water filled at the top drains from the bottom. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void twoBlocksShareOneTank(GameTestHelper helper) {
        BlockPos bottom = new BlockPos(2, 1, 2);
        BlockPos top = bottom.above();
        tank(helper, bottom);
        tank(helper, top);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    assertCapacity(helper, bottom, 2 * cap());
                    assertCapacity(helper, top, 2 * cap());
                    helper.assertTrue(joined(helper, bottom, Direction.UP) && joined(helper, top, Direction.DOWN), "the two blocks should be joined");
                    int filled = handler(helper, top).fill(water(cap() + 4000), FluidAction.EXECUTE);
                    helper.assertTrue(filled == cap() + 4000, "the top should take all the water, took " + filled);
                    assertAmount(helper, bottom, cap() + 4000);
                    assertShare(helper, bottom, cap());
                    assertShare(helper, top, 4000);
                    FluidStack drained = handler(helper, bottom).drain(3000, FluidAction.EXECUTE);
                    helper.assertTrue(drained.getAmount() == 3000 && drained.is(Fluids.WATER), "the bottom should give 3000 mB of water");
                    assertAmount(helper, top, cap() + 1000);
                })
                .thenSucceed();
    }

    /** Clicks the top face of the block at {@code pos} with a tank item, like a player's right click. */
    private static InteractionResult placeOnTop(GameTestHelper helper, Player player, BlockPos pos, ItemStack stack) {
        BlockPos absolute = helper.absolutePos(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
        BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
        return ((FluidTankItem) stack.getItem()).place(context);
    }

    /** An L of three blocks is a pair and a single; the fourth block forms one 2 by 2 tank holding all their fluid. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void floorFormsWithTheFourthBlock(GameTestHelper helper) {
        BlockPos a = new BlockPos(2, 1, 2);
        BlockPos b = a.east();
        BlockPos c = a.south();
        BlockPos d = a.east().south();
        tank(helper, a);
        tank(helper, b);
        tank(helper, c);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    assertCapacity(helper, a, 2 * cap());
                    assertCapacity(helper, c, cap());
                    handler(helper, a).fill(water(1000), FluidAction.EXECUTE);
                    handler(helper, c).fill(water(1000), FluidAction.EXECUTE);
                    assertAmount(helper, b, 1000);
                    assertAmount(helper, c, 1000);
                    helper.assertTrue(joined(helper, a, Direction.EAST) && !joined(helper, a, Direction.SOUTH), "the pair joins, the single does not");
                    helper.assertTrue(!anyJoined(helper, c), "the single shows its full frame");
                    tank(helper, d);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    for (BlockPos pos : List.of(a, b, c, d)) {
                        assertCapacity(helper, pos, 4 * cap());
                        assertAmount(helper, pos, 2000);
                        assertShare(helper, pos, 500);
                    }
                    helper.assertTrue(joined(helper, a, Direction.EAST) && joined(helper, a, Direction.SOUTH)
                            && !joined(helper, a, Direction.WEST) && !joined(helper, a, Direction.UP), "a corner joins toward the box only");
                })
                .thenSucceed();
    }

    /** A block on top of a 2 by 2 tank leaves it formed; completing the top layer grows it to 2 by 2 by 2. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void blockOnTopKeepsTheTankAndALayerGrowsIt(GameTestHelper helper) {
        BlockPos min = new BlockPos(2, 1, 2);
        box(helper, min, new BlockPos(3, 1, 3));
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    handler(helper, min).fill(water(2 * cap()), FluidAction.EXECUTE);
                    tank(helper, min.above());
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    assertCapacity(helper, min, 4 * cap());
                    assertAmount(helper, min, 2 * cap());
                    assertCapacity(helper, min.above(), cap());
                    helper.assertTrue(joined(helper, min, Direction.EAST) && !joined(helper, min, Direction.UP),
                            "the floor stays joined and shows a frame toward the block on top");
                    tank(helper, min.above().east());
                    tank(helper, min.above().south());
                    tank(helper, min.above().east().south());
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    assertCapacity(helper, min, 8 * cap());
                    assertAmount(helper, min.above(), 2 * cap());
                    helper.assertTrue(joined(helper, min, Direction.UP), "the two layers join");
                })
                .thenSucceed();
    }

    /** A 2 by 2 by 2 tank fills bottom up; breaking a corner regroups the other seven and keeps their fluid. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void brokenCornerRegroupsAndKeepsShares(GameTestHelper helper) {
        BlockPos min = new BlockPos(2, 1, 2);
        BlockPos max = new BlockPos(3, 2, 3);
        box(helper, min, max);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    assertCapacity(helper, min, 8 * cap());
                    handler(helper, max).fill(water(6 * cap()), FluidAction.EXECUTE);
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                        assertShare(helper, pos.immutable(), pos.getY() == min.getY() ? cap() : cap() / 2);
                    }
                    helper.setBlock(max, Blocks.AIR.defaultBlockState());
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    int total = 0;
                    for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                        BlockPos p = pos.immutable();
                        if (!p.equals(max)) {
                            total += entity(helper, p).share().getAmount();
                        }
                    }
                    helper.assertTrue(total == 6 * cap() - cap() / 2, "only the broken block's share may be gone, left " + total);
                    assertCapacity(helper, min, 4 * cap());
                    assertAmount(helper, min, 4 * cap());
                })
                .thenSucceed();
    }

    /** A single block touching a 2 by 2 by 2 tank leaves the tank formed and works on its own. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void neighbourBlockLeavesTheBoxFormed(GameTestHelper helper) {
        BlockPos min = new BlockPos(2, 1, 2);
        BlockPos max = new BlockPos(3, 2, 3);
        BlockPos stray = new BlockPos(4, 1, 2);
        box(helper, min, max);
        tank(helper, stray);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    assertCapacity(helper, min, 8 * cap());
                    assertCapacity(helper, stray, cap());
                    helper.assertTrue(joined(helper, min, Direction.EAST) && !joined(helper, min.east(), Direction.EAST)
                            && !anyJoined(helper, stray), "the box is joined and shows a frame toward the single block");
                })
                .thenSucceed();
    }

    /** A stack of tank items on top of a 2 by 2 tank places a whole layer and uses four items. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void stackOnTopPlacesAWholeLayer(GameTestHelper helper) {
        BlockPos min = new BlockPos(2, 1, 2);
        box(helper, min, new BlockPos(3, 1, 3));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    ItemStack stack = new ItemStack(ServusLogisticsItems.FLUID_TANK.get(), 16);
                    placeOnTop(helper, player, min.east(), stack);
                    for (BlockPos pos : BlockPos.betweenClosed(min.above(), new BlockPos(3, 2, 3))) {
                        helper.assertBlockPresent(ServusLogisticsBlocks.FLUID_TANK.get(), pos.immutable());
                    }
                    helper.assertTrue(stack.getCount() == 12, "four items should be used, left " + stack.getCount());
                })
                .thenIdle(2)
                .thenExecute(() -> assertCapacity(helper, min, 8 * cap()))
                .thenSucceed();
    }

    /** Too few items, sneaking or a blocked spot place one block instead of a layer. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void layerFallsBackToOneBlock(GameTestHelper helper) {
        BlockPos few = new BlockPos(1, 1, 1);
        BlockPos sneak = new BlockPos(5, 1, 1);
        BlockPos blocked = new BlockPos(1, 1, 5);
        for (BlockPos min : List.of(few, sneak, blocked)) {
            box(helper, min, min.east().south());
        }
        helper.setBlock(blocked.above().east(), Blocks.STONE.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    ItemStack three = new ItemStack(ServusLogisticsItems.FLUID_TANK.get(), 3);
                    placeOnTop(helper, player, few, three);
                    helper.assertTrue(three.getCount() == 2, "too few items place one block, left " + three.getCount());
                    helper.assertBlockNotPresent(ServusLogisticsBlocks.FLUID_TANK.get(), few.above().east());

                    player.setShiftKeyDown(true);
                    ItemStack sneaking = new ItemStack(ServusLogisticsItems.FLUID_TANK.get(), 16);
                    placeOnTop(helper, player, sneak, sneaking);
                    helper.assertTrue(sneaking.getCount() == 15, "sneaking places one block, left " + sneaking.getCount());
                    player.setShiftKeyDown(false);

                    ItemStack stack = new ItemStack(ServusLogisticsItems.FLUID_TANK.get(), 16);
                    placeOnTop(helper, player, blocked, stack);
                    helper.assertTrue(stack.getCount() == 15, "a blocked spot places one block, left " + stack.getCount());
                    helper.assertBlockPresent(ServusLogisticsBlocks.FLUID_TANK.get(), blocked.above());
                })
                .thenSucceed();
    }

    /** A water bucket empties into an empty tank, an empty bucket takes it back out. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void bucketsFillAndEmpty(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        tank(helper, pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    BlockPos absolute = helper.absolutePos(pos);
                    BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
                    BlockState state = helper.getLevel().getBlockState(absolute);
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
                    state.useItemOn(player.getItemInHand(InteractionHand.MAIN_HAND), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
                    assertAmount(helper, pos, 1000);
                    helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "the player should hold an empty bucket");

                    state.useItemOn(player.getItemInHand(InteractionHand.MAIN_HAND), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
                    assertAmount(helper, pos, 0);
                    helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.WATER_BUCKET), "the player should hold a water bucket again");
                })
                .thenSucceed();
    }

    /** A tank holding water accepts no lava, single or formed. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void waterTankRefusesLava(GameTestHelper helper) {
        BlockPos single = new BlockPos(2, 1, 2);
        BlockPos formed = new BlockPos(6, 1, 6);
        tank(helper, single);
        tank(helper, formed);
        tank(helper, formed.above());
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    for (BlockPos pos : List.of(single, formed)) {
                        handler(helper, pos).fill(water(1000), FluidAction.EXECUTE);
                        int lava = handler(helper, pos).fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE);
                        helper.assertTrue(lava == 0, "the water tank at " + pos.toShortString() + " took " + lava + " mB of lava");
                        assertAmount(helper, pos, 1000);
                    }
                })
                .thenSucceed();
    }

    /** A tank block cannot be placed between a water and a lava tank, but next to water alone it can. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void placementBetweenWaterAndLavaIsRefused(GameTestHelper helper) {
        BlockPos waterTank = new BlockPos(2, 1, 2);
        BlockPos lavaTank = new BlockPos(4, 1, 2);
        BlockPos between = new BlockPos(3, 1, 2);
        BlockPos nextToWater = new BlockPos(2, 1, 3);
        tank(helper, waterTank);
        tank(helper, lavaTank);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    handler(helper, waterTank).fill(water(1000), FluidAction.EXECUTE);
                    handler(helper, lavaTank).fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE);
                    InteractionResult refused = placeItem(helper, player, between, new ItemStack(ServusLogisticsItems.FLUID_TANK.get()));
                    helper.assertTrue(refused == InteractionResult.FAIL, "placing between water and lava should fail, got " + refused);
                    helper.assertBlockNotPresent(ServusLogisticsBlocks.FLUID_TANK.get(), between);
                    InteractionResult placed = placeItem(helper, player, nextToWater, new ItemStack(ServusLogisticsItems.FLUID_TANK.get()));
                    helper.assertTrue(placed.consumesAction(), "placing next to water alone should work, got " + placed);
                    helper.assertBlockPresent(ServusLogisticsBlocks.FLUID_TANK.get(), nextToWater);
                })
                .thenSucceed();
    }

    /** A full single block drops an item carrying its water, and placing it elsewhere restores the water. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void movedFullBlockKeepsItsWater(GameTestHelper helper) {
        BlockPos from = new BlockPos(2, 1, 2);
        BlockPos to = new BlockPos(7, 1, 7);
        tank(helper, from);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack[] dropped = new ItemStack[1];
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    handler(helper, from).fill(water(cap()), FluidAction.EXECUTE);
                    BlockPos absolute = helper.absolutePos(from);
                    List<ItemStack> drops = Block.getDrops(helper.getLevel().getBlockState(absolute), helper.getLevel(), absolute,
                            helper.getLevel().getBlockEntity(absolute));
                    helper.assertTrue(drops.size() == 1 && drops.get(0).is(ServusLogisticsItems.FLUID_TANK.get()), "the block should drop one tank item");
                    FluidStack carried = FluidTankItem.fluid(drops.get(0));
                    helper.assertTrue(carried.is(Fluids.WATER) && carried.getAmount() == cap(), "the item should carry the water, got " + carried);
                    dropped[0] = drops.get(0);
                    helper.setBlock(from, Blocks.AIR.defaultBlockState());
                    InteractionResult placed = placeItem(helper, player, to, dropped[0]);
                    helper.assertTrue(placed.consumesAction(), "the item should place, got " + placed);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    assertAmount(helper, to, cap());
                    helper.assertTrue(handler(helper, to).getFluidInTank(0).is(Fluids.WATER), "the placed tank should hold water");
                })
                .thenSucceed();
    }

    /** A tower of four half full renders the lower two full; its empty top drops empty and the rest keeps the fluid. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void towerFillsBottomUpAndKeepsFluidWhenTopBreaks(GameTestHelper helper) {
        BlockPos base = new BlockPos(2, 1, 2);
        for (int i = 0; i < 4; i++) {
            tank(helper, base.above(i));
        }
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    handler(helper, base.above(3)).fill(water(2 * cap()), FluidAction.EXECUTE);
                    assertShare(helper, base, cap());
                    assertShare(helper, base.above(1), cap());
                    assertShare(helper, base.above(2), 0);
                    assertShare(helper, base.above(3), 0);
                    BlockPos top = helper.absolutePos(base.above(3));
                    List<ItemStack> drops = Block.getDrops(helper.getLevel().getBlockState(top), helper.getLevel(), top,
                            helper.getLevel().getBlockEntity(top));
                    helper.assertTrue(drops.size() == 1 && FluidTankItem.fluid(drops.get(0)).isEmpty(), "the empty top should drop an empty tank");
                    helper.setBlock(base.above(3), Blocks.AIR.defaultBlockState());
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    assertCapacity(helper, base, 3 * cap());
                    assertAmount(helper, base, 2 * cap());
                })
                .thenSucceed();
    }

    private static CraftingInput tankGrid(net.minecraft.world.item.Item floor) {
        ItemStack c = new ItemStack(Items.COPPER_INGOT);
        ItemStack g = new ItemStack(Items.GLASS);
        return CraftingInput.of(3, 3, List.of(c.copy(), g.copy(), c.copy(), g.copy(), ItemStack.EMPTY, g.copy(),
                c.copy(), new ItemStack(floor), c.copy()));
    }

    /** Copper, glass and stone bricks craft a tank with a stone brick floor; glass in the floor slot crafts nothing. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void recipeTakesTheFloorBlock(GameTestHelper helper) {
        var level = helper.getLevel();
        CraftingInput bricks = tankGrid(Items.STONE_BRICKS);
        Optional<RecipeHolder<CraftingRecipe>> match = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, bricks, level);
        helper.assertTrue(match.isPresent(), "copper, glass and stone bricks should match the tank recipe");
        ItemStack result = match.get().value().assemble(bricks, level.registryAccess());
        helper.assertTrue(result.is(ServusLogisticsItems.FLUID_TANK.get()) && result.getCount() == 1, "result should be one tank");
        helper.assertTrue(FluidTankItem.floor(result) == Blocks.STONE_BRICKS, "the tank should remember stone bricks");
        Optional<RecipeHolder<CraftingRecipe>> glass = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                tankGrid(Items.GLASS), level);
        helper.assertTrue(glass.isEmpty() || !(glass.get().value() instanceof FluidTankRecipe), "glass must not be a floor");
        helper.succeed();
    }

    /** A placed tank takes its floor from the item and drops it again. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void floorTravelsWithTheItem(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        InteractionResult placed = placeItem(helper, player, pos, FluidTankItem.withFloor(Blocks.STONE_BRICKS));
        helper.assertTrue(placed.consumesAction(), "the tank should place, got " + placed);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    BlockPos absolute = helper.absolutePos(pos);
                    FluidTankBlockEntity tank = (FluidTankBlockEntity) helper.getLevel().getBlockEntity(absolute);
                    helper.assertTrue(tank != null && tank.floor() == Blocks.STONE_BRICKS, "the placed tank should have a stone brick floor");
                    List<ItemStack> drops = Block.getDrops(helper.getLevel().getBlockState(absolute), helper.getLevel(), absolute, tank);
                    helper.assertTrue(drops.size() == 1 && FluidTankItem.floor(drops.get(0)) == Blocks.STONE_BRICKS,
                            "the dropped tank should keep its floor");
                })
                .thenSucceed();
    }
}
