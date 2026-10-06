package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.rail.RailBlock;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import io.github.dirnei.mc.servus.storage.terminal.CounterTerminalBlock;
import io.github.dirnei.mc.servus.storage.terminal.CounterTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Items;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ServusStorageGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos CRATE = new BlockPos(3, 1, 3);
    private static final BlockPos CONTROLLER = new BlockPos(4, 1, 3);
    private static final BlockPos TERMINAL = new BlockPos(5, 1, 3);

    private static final BlockPos RAIL_CRATE = new BlockPos(2, 1, 3);
    private static final BlockPos RAIL_A = new BlockPos(3, 1, 3);
    private static final BlockPos RAIL_B = new BlockPos(4, 1, 3);
    private static final BlockPos RAIL_CONTROLLER = new BlockPos(5, 1, 3);
    private static final BlockPos RAIL_TERMINAL = new BlockPos(6, 1, 3);
    private static final BlockPos RAIL_CHEST = new BlockPos(3, 1, 4);

    private ServusStorageGameTests() {
    }

    /** Two rails link a crate to the controller; a chest beside a rail stays out of the index. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void railsLinkCrateAndController(GameTestHelper helper) {
        helper.setBlock(RAIL_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(RAIL_A.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(RAIL_B.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(RAIL_A, ServusStorageBlocks.RAIL.get().defaultBlockState());
        helper.setBlock(RAIL_B, ServusStorageBlocks.RAIL.get().defaultBlockState());
        helper.setBlock(RAIL_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(RAIL_TERMINAL, ServusStorageBlocks.COUNTER_TERMINAL.get().defaultBlockState()
                .setValue(CounterTerminalBlock.FACING, Direction.NORTH));
        helper.setBlock(RAIL_CHEST, Blocks.CHEST.defaultBlockState());
        CrateBlockEntity crate = helper.getBlockEntity(RAIL_CRATE);
        crate.insert(new ItemStack(Items.COBBLESTONE, 40), false);
        ChestBlockEntity chest = helper.getBlockEntity(RAIL_CHEST);
        chest.setItem(0, new ItemStack(Items.BREAD, 10));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = helper.getBlockEntity(RAIL_TERMINAL);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online through the rails");
            helper.assertTrue(stockOf(menu, Items.COBBLESTONE) == 40, "stock should list 40 cobblestone from the crate");
            helper.assertTrue(stockOf(menu, Items.BREAD) == 0, "bread beside a rail must not be indexed");
            helper.assertTrue(lit(helper, RAIL_CRATE), "crate lamp should be lit while online");

            helper.setBlock(RAIL_A, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(10, () -> {
                CounterTerminalMenu after = new CounterTerminalMenu(1, player.getInventory(), terminal);
                helper.assertTrue(after.stock().online(), "controller side stays online");
                helper.assertTrue(stockOf(after, Items.COBBLESTONE) == 0, "crate stock should leave the index after the rail is broken");
                helper.assertTrue(!lit(helper, RAIL_CRATE), "crate lamp should go dark once cut off");
                helper.assertTrue(lit(helper, RAIL_TERMINAL), "terminal lamp stays lit beside the controller");
                helper.succeed();
            });
        });
    }

    private static final BlockPos LIFT_CRATE = new BlockPos(2, 1, 3);
    private static final BlockPos LIFT_RAIL_LOW = new BlockPos(3, 1, 3);
    private static final BlockPos LIFT_BOTTOM = new BlockPos(4, 1, 3);
    private static final BlockPos LIFT_MIDDLE = new BlockPos(4, 2, 3);
    private static final BlockPos LIFT_TOP = new BlockPos(4, 3, 3);
    private static final BlockPos LIFT_RAIL_HIGH = new BlockPos(5, 3, 3);
    private static final BlockPos LIFT_CONTROLLER = new BlockPos(6, 3, 3);
    private static final BlockPos LIFT_TERMINAL = new BlockPos(7, 3, 3);
    private static final BlockPos LIFT_CHEST = new BlockPos(4, 2, 4);
    private static final BlockPos LIFT_SIDE_CRATE = new BlockPos(4, 1, 4);

    /** A three segment shaft links a ground floor crate to a controller two floors up; a chest on the shaft stays out. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void elevatorLinksFloors(GameTestHelper helper) {
        helper.setBlock(LIFT_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(LIFT_RAIL_LOW.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(LIFT_BOTTOM.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(LIFT_RAIL_LOW, ServusStorageBlocks.RAIL.get().defaultBlockState());
        helper.setBlock(LIFT_BOTTOM, ServusStorageBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(LIFT_MIDDLE, ServusStorageBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(LIFT_TOP, ServusStorageBlocks.ELEVATOR.get().defaultBlockState());
        helper.setBlock(LIFT_RAIL_HIGH.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(LIFT_CONTROLLER.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(LIFT_TERMINAL.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(LIFT_RAIL_HIGH, ServusStorageBlocks.RAIL.get().defaultBlockState());
        helper.setBlock(LIFT_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(LIFT_TERMINAL, ServusStorageBlocks.COUNTER_TERMINAL.get().defaultBlockState()
                .setValue(CounterTerminalBlock.FACING, Direction.NORTH));
        helper.setBlock(LIFT_CHEST, Blocks.CHEST.defaultBlockState());
        helper.setBlock(LIFT_SIDE_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity crate = helper.getBlockEntity(LIFT_CRATE);
        crate.insert(new ItemStack(Items.COBBLESTONE, 30), false);
        CrateBlockEntity sideCrate = helper.getBlockEntity(LIFT_SIDE_CRATE);
        sideCrate.insert(new ItemStack(Items.APPLE, 7), false);
        ChestBlockEntity chest = helper.getBlockEntity(LIFT_CHEST);
        chest.setItem(0, new ItemStack(Items.BREAD, 10));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = helper.getBlockEntity(LIFT_TERMINAL);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online through the shaft");
            helper.assertTrue(stockOf(menu, Items.COBBLESTONE) == 30, "stock should list 30 cobblestone from the ground floor crate");
            helper.assertTrue(stockOf(menu, Items.BREAD) == 0, "bread beside the shaft must not be indexed");
            helper.assertTrue(lit(helper, LIFT_CRATE), "crate lamp should be lit through the shaft");
            helper.assertTrue(stockOf(menu, Items.APPLE) == 0, "a crate touching only a segment must not join through it");
            helper.assertTrue(!lit(helper, LIFT_SIDE_CRATE), "that crate's lamp stays dark");

            helper.setBlock(LIFT_MIDDLE, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(10, () -> {
                CounterTerminalMenu after = new CounterTerminalMenu(1, player.getInventory(), terminal);
                helper.assertTrue(after.stock().online(), "controller side stays online");
                helper.assertTrue(stockOf(after, Items.COBBLESTONE) == 0, "crate stock should leave the index after the shaft is cut");
                helper.assertTrue(!lit(helper, LIFT_CRATE), "crate lamp should go dark once the shaft is cut");
                helper.assertTrue(lit(helper, LIFT_TERMINAL), "terminal lamp stays lit beside the controller");
                helper.succeed();
            });
        });
    }

    private static final BlockPos QUAD = new BlockPos(8, 1, 8);
    private static final BlockPos QUAD_CONTROLLER = new BlockPos(9, 1, 8);
    private static final BlockPos QUAD_TERMINAL = new BlockPos(10, 1, 8);

    /** A quad crate keeps two types in two compartments, refuses a mismatch, and the terminal lists both. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void quadCrateCompartments(GameTestHelper helper) {
        helper.setBlock(QUAD, ServusStorageBlocks.QUAD_CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(QUAD_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(QUAD_TERMINAL, ServusStorageBlocks.COUNTER_TERMINAL.get().defaultBlockState()
                .setValue(CounterTerminalBlock.FACING, Direction.NORTH));
        CrateBlockEntity crate = helper.getBlockEntity(QUAD);
        helper.assertTrue(crate.compartments() == 4, "quad crate should have four compartments");
        helper.assertTrue(crate.insert(0, new ItemStack(Items.COBBLESTONE, 40), false) == 40, "compartment 0 takes cobblestone");
        helper.assertTrue(crate.insert(3, new ItemStack(Items.DIRT, 12), false) == 12, "compartment 3 takes dirt");
        helper.assertTrue(crate.insert(0, new ItemStack(Items.DIRT, 1), false) == 0, "compartment 0 refuses dirt");
        helper.assertTrue(crate.handler().getSlots() == 4, "handler exposes four slots");
        helper.assertTrue(crate.handler().getStackInSlot(0).getCount() == 40, "slot 0 reports 40 cobblestone");
        helper.assertTrue(crate.handler().getStackInSlot(3).is(Items.DIRT), "slot 3 reports dirt");
        helper.assertTrue(crate.handler().getStackInSlot(1).isEmpty(), "slot 1 is empty");
        helper.assertTrue(crate.insert(new ItemStack(Items.COBBLESTONE, 5), false) == 5 && crate.count(0) == 45,
                "untargeted insert joins the cobblestone compartment");
        helper.assertTrue(crate.capacityPerCompartment() * 4 == crate.capacity(), "compartments split the capacity");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = helper.getBlockEntity(QUAD_TERMINAL);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online");
            helper.assertTrue(stockOf(menu, Items.COBBLESTONE) == 45, "terminal lists 45 cobblestone");
            helper.assertTrue(stockOf(menu, Items.DIRT) == 12, "terminal lists 12 dirt");
            helper.succeed();
        });
    }

    private static final BlockPos RACK = new BlockPos(8, 1, 2);
    private static final BlockPos RACK_CONTROLLER = new BlockPos(9, 1, 2);
    private static final BlockPos RACK_TERMINAL = new BlockPos(10, 1, 2);

    /** A rack is indexed, lit, and shows the item types it holds the most of. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void heavyRackIndexesAndDisplays(GameTestHelper helper) {
        helper.setBlock(RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        helper.setBlock(RACK_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(RACK_TERMINAL, ServusStorageBlocks.COUNTER_TERMINAL.get().defaultBlockState()
                .setValue(CounterTerminalBlock.FACING, Direction.NORTH));
        HeavyRackBlockEntity rack = helper.getBlockEntity(RACK);
        rack.items().insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        rack.items().insertItem(1, new ItemStack(Items.DIRT, 32), false);
        rack.items().insertItem(2, new ItemStack(Items.SAND, 16), false);
        rack.items().insertItem(3, new ItemStack(Items.GRAVEL, 8), false);
        rack.items().insertItem(4, new ItemStack(Items.APPLE, 1), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = helper.getBlockEntity(RACK_TERMINAL);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online");
            helper.assertTrue(stockOf(menu, Items.COBBLESTONE) == 64 && stockOf(menu, Items.APPLE) == 1, "terminal lists the rack contents");
            List<ItemStack> sample = rack.displayed();
            helper.assertTrue(sample.size() == 4, "sample shows four types, had " + sample.size());
            helper.assertTrue(sample.get(0).is(Items.COBBLESTONE) && sample.get(3).is(Items.GRAVEL), "sample is ordered by count");

            rack.items().insertItem(5, new ItemStack(Items.APPLE, 64), false);
            rack.items().insertItem(6, new ItemStack(Items.APPLE, 36), false);
            helper.runAfterDelay(10, () -> {
                List<ItemStack> after = rack.displayed();
                helper.assertTrue(after.get(0).is(Items.APPLE), "apples lead the sample after the refill");
                helper.assertTrue(after.stream().noneMatch(stack -> stack.is(Items.GRAVEL)), "gravel dropped out of the sample");
                helper.succeed();
            });
        });
    }

    private static final BlockPos SH_CONTROLLER = new BlockPos(2, 1, 6);
    private static final BlockPos SH_TERMINAL = new BlockPos(2, 1, 7);
    private static final BlockPos SH_DIRECT_CRATE = new BlockPos(1, 1, 6);
    private static final BlockPos SH_FAR_CRATE = new BlockPos(7, 1, 7);

    /** A shuttle fetches from a crate at the end of the rail; a crate on the controller is still instant. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void shuttlePicksFromRail(GameTestHelper helper) {
        helper.setBlock(SH_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(SH_TERMINAL, ServusStorageBlocks.COUNTER_TERMINAL.get().defaultBlockState()
                .setValue(CounterTerminalBlock.FACING, Direction.NORTH));
        helper.setBlock(SH_DIRECT_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        for (int x = 3; x <= 7; x++) {
            helper.setBlock(new BlockPos(x, 0, 6), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 6), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(SH_FAR_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity far = helper.getBlockEntity(SH_FAR_CRATE);
        far.insert(new ItemStack(Items.COBBLESTONE, 40), false);
        CrateBlockEntity direct = helper.getBlockEntity(SH_DIRECT_CRATE);
        direct.insert(new ItemStack(Items.DIRT, 9), false);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(SH_CONTROLLER);
        helper.assertTrue(controller.addShuttle(), "one shuttle fits");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = helper.getBlockEntity(SH_TERMINAL);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online");
            menu.pull(ItemKey.of(new ItemStack(Items.COBBLESTONE)), true);
            helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 0, "cobblestone must not arrive at once");
            menu.pull(ItemKey.of(new ItemStack(Items.DIRT)), true);
            helper.assertTrue(player.getInventory().countItem(Items.DIRT) == 9, "dirt from the crate on the controller is instant");

            helper.runAfterDelay(20, () -> helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 0,
                    "shuttle is still on its way after 20 ticks"));
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 40,
                        "shuttle should have delivered 40 cobblestone, had " + player.getInventory().countItem(Items.COBBLESTONE));
                helper.assertTrue(far.isEmpty(), "far crate should be empty");
                helper.succeed();
            });
        });
    }

    private static final BlockPos FR_RACK = new BlockPos(5, 1, 9);
    private static final BlockPos FR_SIDE_RAIL = new BlockPos(6, 1, 9);
    private static final BlockPos FR_SIDE_CONTROLLER = new BlockPos(7, 1, 9);
    private static final BlockPos FR_SIDE_TERMINAL = new BlockPos(8, 1, 9);
    private static final BlockPos FR_FRONT_RAIL = new BlockPos(5, 1, 8);
    private static final BlockPos FR_FRONT_CONTROLLER = new BlockPos(4, 1, 8);
    private static final BlockPos FR_FRONT_TERMINAL = new BlockPos(3, 1, 8);

    /** A rail at the side of a rack does not link it; a rail at its front does. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void rackLinksRailOnlyFromFront(GameTestHelper helper) {
        helper.setBlock(FR_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        helper.setBlock(FR_SIDE_RAIL.below(), Blocks.STONE.defaultBlockState());
        placeRail(helper, FR_SIDE_RAIL);
        helper.setBlock(FR_SIDE_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(FR_SIDE_TERMINAL, ServusStorageBlocks.COUNTER_TERMINAL.get().defaultBlockState()
                .setValue(CounterTerminalBlock.FACING, Direction.NORTH));
        HeavyRackBlockEntity rack = helper.getBlockEntity(FR_RACK);
        rack.items().insertItem(0, new ItemStack(Items.SAND, 20), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity sideTerminal = helper.getBlockEntity(FR_SIDE_TERMINAL);
            CounterTerminalMenu sideMenu = new CounterTerminalMenu(1, player.getInventory(), sideTerminal);
            helper.assertTrue(sideMenu.stock().online(), "side warehouse is online");
            helper.assertTrue(stockOf(sideMenu, Items.SAND) == 0, "a rail at the rack's side must not link it");
            helper.assertTrue(!helper.getBlockState(FR_SIDE_RAIL).getValue(RailBlock.DOCK_WEST), "no dock onto the rack's side");

            helper.setBlock(FR_FRONT_RAIL.below(), Blocks.STONE.defaultBlockState());
            placeRail(helper, FR_FRONT_RAIL);
            helper.setBlock(FR_FRONT_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                    .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
            helper.setBlock(FR_FRONT_TERMINAL, ServusStorageBlocks.COUNTER_TERMINAL.get().defaultBlockState()
                    .setValue(CounterTerminalBlock.FACING, Direction.NORTH));
            helper.runAfterDelay(10, () -> {
                CounterTerminalBlockEntity frontTerminal = helper.getBlockEntity(FR_FRONT_TERMINAL);
                CounterTerminalMenu frontMenu = new CounterTerminalMenu(1, player.getInventory(), frontTerminal);
                helper.assertTrue(frontMenu.stock().online(), "front warehouse is online");
                helper.assertTrue(stockOf(frontMenu, Items.SAND) == 20, "a rail at the rack's front links it");
                helper.assertTrue(helper.getBlockState(FR_FRONT_RAIL).getValue(RailBlock.DOCK_SOUTH), "dock onto the rack's front");
                helper.succeed();
            });
        });
    }

    /** Places a rail the way a player would: its connection and dock flags come from the neighbours. */
    private static void placeRail(GameTestHelper helper, BlockPos pos) {
        BlockState state = Block.updateFromNeighbourShapes(ServusStorageBlocks.RAIL.get().defaultBlockState(),
                helper.getLevel(), helper.absolutePos(pos));
        helper.setBlock(pos, state);
    }

    private static final BlockPos STACK_BASE = new BlockPos(10, 1, 10);

    /** A rack placed on top of a rack takes the lower rack's facing, whatever way the player looks. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rackOnRackCopiesFacing(GameTestHelper helper) {
        helper.setBlock(STACK_BASE, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.EAST));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        BlockPos above = helper.absolutePos(STACK_BASE.above());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(above), Direction.UP, above, false);
        ItemStack rackItem = new ItemStack(ServusStorageBlocks.HEAVY_RACK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rackItem);
        net.minecraft.world.InteractionResult placed = rackItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.runAfterDelay(2, () -> {
            BlockState top = helper.getBlockState(STACK_BASE.above());
            helper.assertTrue(top.getBlock() instanceof HeavyRackBlock, "a rack should have been placed on top, got " + top.getBlock() + " result " + placed);
            helper.assertTrue(top.getValue(HeavyRackBlock.FACING) == Direction.EAST, "the top rack should face east like the one below");
            helper.assertTrue(top.getValue(HeavyRackBlock.DOWN), "the top rack should join the rack below");
            helper.assertTrue(helper.getBlockState(STACK_BASE).getValue(HeavyRackBlock.UP), "the lower rack should join the rack above");
            helper.succeed();
        });
    }

    private static final int SEC_Z = 11;

    /** Places a rack through its item with the mock player looking south, so the rack faces north. */
    private static void placeRackByHand(GameTestHelper helper, Player player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        ItemStack rackItem = new ItemStack(ServusStorageBlocks.HEAVY_RACK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rackItem);
        rackItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static boolean joined(GameTestHelper helper, int x, BooleanProperty side) {
        return helper.getBlockState(new BlockPos(x, 1, SEC_Z)).getValue(side);
    }

    /** Sections form in placement order and never move: three join, a fourth at either end stands alone. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rackSectionsStayPut(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        for (int x = 4; x <= 6; x++) {
            placeRackByHand(helper, player, new BlockPos(x, 1, SEC_Z));
        }
        helper.assertTrue(joined(helper, 4, HeavyRackBlock.RIGHT) && joined(helper, 5, HeavyRackBlock.LEFT), "bays 4 and 5 join");
        helper.assertTrue(joined(helper, 5, HeavyRackBlock.RIGHT) && joined(helper, 6, HeavyRackBlock.LEFT), "bays 5 and 6 join");
        helper.assertTrue(!joined(helper, 4, HeavyRackBlock.LEFT) && !joined(helper, 6, HeavyRackBlock.RIGHT), "the section has posts at both ends");

        placeRackByHand(helper, player, new BlockPos(7, 1, SEC_Z));
        helper.assertTrue(!joined(helper, 6, HeavyRackBlock.RIGHT) && !joined(helper, 7, HeavyRackBlock.LEFT), "a fourth bay on the right stands alone");
        placeRackByHand(helper, player, new BlockPos(3, 1, SEC_Z));
        helper.assertTrue(!joined(helper, 4, HeavyRackBlock.LEFT) && !joined(helper, 3, HeavyRackBlock.RIGHT), "a fourth bay on the left stands alone");
        helper.assertTrue(joined(helper, 4, HeavyRackBlock.RIGHT) && joined(helper, 6, HeavyRackBlock.LEFT), "the first three are unchanged");

        placeRackByHand(helper, player, new BlockPos(8, 1, SEC_Z));
        helper.assertTrue(joined(helper, 7, HeavyRackBlock.RIGHT) && joined(helper, 8, HeavyRackBlock.LEFT), "a fifth bay joins the lone fourth");
        helper.assertTrue(!joined(helper, 7, HeavyRackBlock.LEFT), "the fourth still does not join the full section");
        helper.succeed();
    }

    private static final int UP_Z = 11;

    /** An upper row placed shifted by one splits above the same post as the row below. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void upperRowFollowsLowerSections(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        for (int x = 1; x <= 5; x++) {
            placeRackByHand(helper, player, new BlockPos(x, 1, UP_Z));
        }
        helper.assertTrue(!joined(helper, 3, HeavyRackBlock.RIGHT), "lower row splits after the third bay");
        for (int x = 2; x <= 6; x++) {
            placeRackByHand(helper, player, new BlockPos(x, 2, UP_Z));
        }
        BlockState up2 = helper.getBlockState(new BlockPos(2, 2, UP_Z));
        BlockState up3 = helper.getBlockState(new BlockPos(3, 2, UP_Z));
        BlockState up4 = helper.getBlockState(new BlockPos(4, 2, UP_Z));
        BlockState up5 = helper.getBlockState(new BlockPos(5, 2, UP_Z));
        BlockState up6 = helper.getBlockState(new BlockPos(6, 2, UP_Z));
        helper.assertTrue(up2.getValue(HeavyRackBlock.RIGHT) && up3.getValue(HeavyRackBlock.LEFT), "upper 2 and 3 join like below");
        helper.assertTrue(!up3.getValue(HeavyRackBlock.RIGHT) && !up4.getValue(HeavyRackBlock.LEFT), "upper row splits above the lower post");
        helper.assertTrue(up4.getValue(HeavyRackBlock.RIGHT) && up5.getValue(HeavyRackBlock.LEFT), "upper 4 and 5 join like below");
        helper.assertTrue(!up5.getValue(HeavyRackBlock.RIGHT) && !up6.getValue(HeavyRackBlock.LEFT), "upper 6 has no rack below and the rack below 5 ends its section");
        helper.succeed();
    }

    /** Four in a row, a gap, one more, then the gap filled: the filler joins the lone bays on both sides. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void gapFillerJoinsBothLoneBays(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        for (int x : new int[] {1, 2, 3, 4, 6, 5}) {
            placeRackByHand(helper, player, new BlockPos(x, 1, SEC_Z));
        }
        helper.assertTrue(!joined(helper, 3, HeavyRackBlock.RIGHT) && !joined(helper, 4, HeavyRackBlock.LEFT), "the fourth bay stands apart from the first three");
        helper.assertTrue(joined(helper, 4, HeavyRackBlock.RIGHT) && joined(helper, 5, HeavyRackBlock.LEFT), "filler joins the fourth");
        helper.assertTrue(joined(helper, 5, HeavyRackBlock.RIGHT) && joined(helper, 6, HeavyRackBlock.LEFT), "filler joins the sixth");
        helper.succeed();
    }

    private static final BlockPos FG_RACK = new BlockPos(2, 1, 11);

    /** With a rack in hand, the side of a rack takes a new rack; the front opens the GUI instead. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rackPlacesAgainstRackSide(GameTestHelper helper) {
        helper.setBlock(FG_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        useRackOn(helper, player, FG_RACK, Direction.EAST);
        helper.assertTrue(helper.getBlockState(FG_RACK.east()).getBlock() instanceof HeavyRackBlock, "clicking the side places a rack beside it");
        useRackOn(helper, player, FG_RACK, Direction.NORTH);
        helper.assertTrue(!(helper.getBlockState(FG_RACK.north()).getBlock() instanceof HeavyRackBlock), "clicking the front opens the rack instead of placing");
        helper.succeed();
    }

    /** Uses a rack item on the given face of the block at pos, through the block's own interaction. */
    private static void useRackOn(GameTestHelper helper, Player player, BlockPos pos, Direction face) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false);
        ItemStack rackItem = new ItemStack(ServusStorageBlocks.HEAVY_RACK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rackItem);
        BlockState state = helper.getBlockState(pos);
        ItemInteractionResult result = state.useItemOn(rackItem, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        if (result == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION) {
            rackItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        } else if (result == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            state.useWithoutItem(helper.getLevel(), player, hit);
        }
    }

    private static boolean lit(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(StorageNodeBlocks.ONLINE);
    }

    private static long stockOf(CounterTerminalMenu menu, Item item) {
        long total = 0;
        for (StockEntry entry : menu.stock().entries()) {
            if (entry.stack().is(item)) {
                total += entry.count();
            }
        }
        return total;
    }

    /** Terminal pulls a crate's stock into the player and stores a stack back. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void terminalPullsAndPushes(GameTestHelper helper) {
        helper.setBlock(CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(TERMINAL, ServusStorageBlocks.COUNTER_TERMINAL.get().defaultBlockState()
                .setValue(CounterTerminalBlock.FACING, Direction.NORTH));
        CrateBlockEntity crate = helper.getBlockEntity(CRATE);
        crate.insert(new ItemStack(Items.COBBLESTONE, 50), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = helper.getBlockEntity(TERMINAL);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online with one controller");
            helper.assertTrue(menu.stock().entries().size() == 1 && menu.stock().entries().get(0).count() == 50,
                    "stock should list 50 cobblestone");

            menu.pull(ItemKey.of(new ItemStack(Items.COBBLESTONE)), true);
            helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 50, "player should hold 50 cobblestone");
            helper.assertTrue(crate.isEmpty(), "crate should be empty after the pull");

            int slot = player.getInventory().findSlotMatchingItem(new ItemStack(Items.COBBLESTONE));
            helper.assertTrue(slot >= 0, "cobblestone should be in a slot");
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 20));
            int menuIndex = slot < 9 ? 27 + slot : slot - 9;
            menu.quickMoveStack(player, menuIndex);
            helper.assertTrue(crate.count() == 20, "crate should hold 20 cobblestone after the push, had " + crate.count());
            helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 0, "inventory should be empty of cobblestone");
            helper.succeed();
        });
    }
}
