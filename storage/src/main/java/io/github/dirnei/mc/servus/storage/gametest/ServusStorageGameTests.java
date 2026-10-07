package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.CounterTerminalMenu;
import io.github.dirnei.mc.servus.storage.menu.InboundStationMenu;
import net.minecraft.world.inventory.ClickType;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.zone.ZoneDefinition;
import io.github.dirnei.mc.servus.storage.zone.ZoneFilter;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageComponents;
import io.github.dirnei.mc.servus.storage.menu.ZoneLabelMenu;
import io.github.dirnei.mc.servus.storage.zone.PutAwayPlan;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlock;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import io.github.dirnei.mc.servus.lib.registry.ServusLibItems;
import io.github.dirnei.mc.servus.lib.production.ServusLibCapabilities;
import io.github.dirnei.mc.servus.lib.production.StockProvider;
import io.github.dirnei.mc.servus.lib.production.StockSink;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlock;
import io.github.dirnei.mc.servus.lib.side.RelativeSide;
import io.github.dirnei.mc.servus.lib.side.SideMode;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.order.OrderBook;
import io.github.dirnei.mc.servus.storage.order.OrderStep;
import io.github.dirnei.mc.servus.storage.order.WorkOrder;
import io.github.dirnei.mc.servus.storage.order.StockRule;
import io.github.dirnei.mc.servus.storage.board.OrderBoardBlock;
import io.github.dirnei.mc.servus.storage.board.OrderBoardBlockEntity;
import io.github.dirnei.mc.servus.storage.board.OrderBoardPayload;
import io.github.dirnei.mc.servus.storage.board.OrderRow;
import io.github.dirnei.mc.servus.storage.menu.OrderBoardMenu;
import io.github.dirnei.mc.servus.storage.terminal.RuleRow;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.lib.production.ProductionProvider;
import io.github.dirnei.mc.servus.lib.production.ProductionRecipe;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.rail.RailBlock;
import io.github.dirnei.mc.servus.lib.block.ConnectableBlock;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import io.github.dirnei.mc.servus.storage.terminal.CounterTerminalBlock;
import io.github.dirnei.mc.servus.storage.terminal.CounterTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.shuttle.ShuttleView;
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
    private static final BlockPos SH_TERMINAL = new BlockPos(3, 1, 7);
    private static final BlockPos SH_DIRECT_CRATE = new BlockPos(1, 1, 6);
    private static final BlockPos SH_FAR_CRATE = new BlockPos(7, 1, 7);
    private static final BlockPos SH_GARAGE = new BlockPos(3, 1, 5);

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
        helper.setBlock(SH_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(SH_GARAGE);
        helper.assertTrue(garage.addShuttle(), "one shuttle fits");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = helper.getBlockEntity(SH_TERMINAL);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online");
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64);
            helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0, "cobblestone must not arrive at once");
            menu.request(ItemKey.of(new ItemStack(Items.DIRT)), 9);
            helper.assertTrue(terminal.outputCount(Items.DIRT) == 9, "dirt from the crate on the controller is instant");

            helper.runAfterDelay(20, () -> helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0,
                    "shuttle is still on its way after 20 ticks"));
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 40,
                        "shuttle should have delivered 40 cobblestone into the output slots, had " + terminal.outputCount(Items.COBBLESTONE));
                helper.assertTrue(far.isEmpty(), "far crate should be empty");
                List<RequestRow> rows = menu.stock().requests();
                helper.assertTrue(rows.size() == 1 && rows.get(0).missing() == 24 && rows.get(0).state().equals("MISSING"),
                        "the dirt request is gone and the cobblestone request shows 24 missing, got " + rows);
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

    private static final BlockPos NR_RACK = new BlockPos(1, 1, 1);

    /** The carrot leaves a rack's facing and joins alone. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void carrotDoesNotTurnRack(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0.0F);
        placeRackByHand(helper, player, NR_RACK);
        placeRackByHand(helper, player, NR_RACK.east());
        BlockPos absolute = helper.absolutePos(NR_RACK);
        ItemStack carrot = new ItemStack(io.github.dirnei.mc.servus.lib.registry.ServusLibItems.CONFIGURATION_CARROT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, carrot);
        carrot.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
        BlockState after = helper.getBlockState(NR_RACK);
        helper.assertTrue(after.getBlock() instanceof HeavyRackBlock, "the rack is still there");
        helper.assertTrue(after.getValue(HeavyRackBlock.FACING) == Direction.NORTH, "the carrot must not turn the rack");
        helper.assertTrue(after.getValue(HeavyRackBlock.RIGHT), "the join to the neighbour survives the click");
        helper.succeed();
    }

    private static final BlockPos CLIMB_RACK = new BlockPos(6, 2, 6);

    /** A player pressed against a rack side climbs or slides without fall damage; the front stays a wall. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rackSideClimbsAndFrontDoesNot(GameTestHelper helper) {
        helper.setBlock(CLIMB_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos rack = helper.absolutePos(CLIMB_RACK);
        double gravity = player.getGravity();

        // West side: the player's east face rests on the rack's west face, feet a little above the rack bottom.
        player.setPos(rack.getX() - 0.3, rack.getY() + 0.3, rack.getZ() + 0.5);
        player.fallDistance = 4.0F;
        player.setDeltaMovement(0.0, -0.5, 0.0);
        player.horizontalCollision = false;
        io.github.dirnei.mc.servus.storage.rack.RackClimbing.tick(player);
        helper.assertTrue(player.fallDistance == 0.0F, "hanging on the side clears the fall distance");
        helper.assertTrue(Math.abs(player.getDeltaMovement().y + 0.15) < 1.0E-9, "hanging slides at the ladder speed, got " + player.getDeltaMovement().y);

        player.horizontalCollision = true;
        io.github.dirnei.mc.servus.storage.rack.RackClimbing.tick(player);
        helper.assertTrue(Math.abs(player.getDeltaMovement().y - (0.2 - gravity) * 0.98) < 1.0E-9, "pushing climbs at the ladder speed, got " + player.getDeltaMovement().y);

        // Front: the player's south face rests on the rack's north face.
        player.setPos(rack.getX() + 0.5, rack.getY() + 0.3, rack.getZ() - 0.3);
        player.fallDistance = 4.0F;
        player.setDeltaMovement(0.0, -0.5, 0.0);
        player.horizontalCollision = true;
        io.github.dirnei.mc.servus.storage.rack.RackClimbing.tick(player);
        helper.assertTrue(player.fallDistance == 4.0F, "the front is not climbable");
        helper.assertTrue(player.getDeltaMovement().y == -0.5, "the front leaves the motion alone");

        // Back: the player's north face rests on the rack's south face.
        player.setPos(rack.getX() + 0.5, rack.getY() + 0.3, rack.getZ() + 1.3);
        player.horizontalCollision = true;
        io.github.dirnei.mc.servus.storage.rack.RackClimbing.tick(player);
        helper.assertTrue(player.fallDistance == 4.0F, "the back is not climbable");
        helper.succeed();
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

    /** A request without shuttles lands in the output slots at once, the player collects it, and a push stores a stack back. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void terminalRequestsAndPushes(GameTestHelper helper) {
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

            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64);
            helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 50, "50 cobblestone should sit in the output slots, had " + terminal.outputCount(Items.COBBLESTONE));
            helper.assertTrue(crate.isEmpty(), "crate should be empty after the request");
            helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 0, "nothing goes into the inventory by itself");
            List<RequestRow> rows = menu.stock().requests();
            helper.assertTrue(rows.size() == 1 && rows.get(0).delivered() == 50 && rows.get(0).missing() == 14 && rows.get(0).state().equals("MISSING"),
                    "the request should show 50 delivered and 14 missing, got " + rows);

            menu.quickMoveStack(player, 0);
            helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 50, "shift click should collect the delivery");
            helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0, "the output slot should be empty");

            menu.cancel(rows.get(0).id());
            helper.assertTrue(menu.stock().requests().isEmpty(), "the cancelled request leaves the list");

            int slot = player.getInventory().findSlotMatchingItem(new ItemStack(Items.COBBLESTONE));
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 20));
            int menuIndex = menu.inventoryStart() + (slot < 9 ? 27 + slot : slot - 9);
            menu.quickMoveStack(player, menuIndex);
            helper.assertTrue(crate.count() == 20, "crate should hold 20 cobblestone after the push, had " + crate.count());
            helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 0, "inventory should be empty of cobblestone");

            menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 5);
            helper.assertTrue(menu.stock().message().equals("out_of_stock"), "a request for nothing in stock is refused, got " + menu.stock().message());
            helper.succeed();
        });
    }

    private static final BlockPos ZL_CRATE = new BlockPos(2, 1, 9);
    private static final BlockPos ZL_RACK = new BlockPos(2, 1, 7);

    /** Uses the held label on the block's north face. */
    private static void useLabel(GameTestHelper helper, Player player, ItemStack label, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0, -0.5), Direction.NORTH, absolute, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, label);
        label.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static ItemStack label(String name, ZoneFilter filter) {
        ItemStack label = new ItemStack(ServusStorageItems.ZONE_LABEL.get());
        if (name != null) {
            label.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }
        if (filter != null) {
            label.set(ServusStorageComponents.ZONE_FILTER.get(), filter);
        }
        return label;
    }

    /** A named label copies name and filter onto crates and racks, a restamp replaces, sneaking clears, an unnamed label does nothing, and the label stays. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void zoneLabelStampsAndClears(GameTestHelper helper) {
        helper.setBlock(ZL_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(ZL_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        CrateBlockEntity crate = helper.getBlockEntity(ZL_CRATE);
        HeavyRackBlockEntity rack = helper.getBlockEntity(ZL_RACK);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        ItemStack unnamed = label(null, new ZoneFilter(List.of(), List.of("c:ores")));
        useLabel(helper, player, unnamed, ZL_CRATE);
        helper.assertTrue(crate.zone().isEmpty(), "an unnamed label must not zone the crate");
        helper.assertTrue(crate.isEmpty(), "the label must not be inserted into the crate");

        ItemStack aisle = label(" Aisle 3 ", new ZoneFilter(List.of(), List.of("c:ores")));
        useLabel(helper, player, aisle, ZL_CRATE);
        useLabel(helper, player, aisle, ZL_RACK);
        helper.assertTrue(crate.zone().equals("Aisle 3"), "crate zone should be trimmed to Aisle 3, was '" + crate.zone() + "'");
        helper.assertTrue(rack.zone().equals("Aisle 3"), "rack zone should be Aisle 3, was '" + rack.zone() + "'");
        helper.assertTrue(crate.zoneKey().equals("aisle 3"), "zone key should be lower case");
        helper.assertTrue(crate.zoneFilter().tags().equals(List.of("c:ores")), "the crate should carry the label's tags, had " + crate.zoneFilter().tags());
        helper.assertTrue(rack.getDisplayName().getString().endsWith("(Aisle 3)"), "rack title should carry the zone, was " + rack.getDisplayName().getString());
        helper.assertTrue(!aisle.isEmpty() && aisle.getCount() == 1, "the label must not be consumed");

        ItemStack food = label("Food", new ZoneFilter(List.of(new ItemStack(Items.BREAD)), List.of()));
        useLabel(helper, player, food, ZL_RACK);
        helper.assertTrue(rack.zone().equals("Food"), "restamp should replace the zone");
        helper.assertTrue(rack.zoneFilter().items().size() == 1 && rack.zoneFilter().items().get(0).is(Items.BREAD), "restamp should replace the filter");

        player.setShiftKeyDown(true);
        useLabel(helper, player, food, ZL_CRATE);
        player.setShiftKeyDown(false);
        helper.assertTrue(crate.zone().isEmpty() && crate.zoneFilter().isEmpty(), "sneak click should clear the zone");

        ItemStack drop = new ItemStack(ServusStorageBlocks.HEAVY_RACK.get());
        rack.saveToItem(drop, helper.getLevel().registryAccess());
        helper.setBlock(ZL_RACK, Blocks.AIR.defaultBlockState());
        helper.setBlock(ZL_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        net.minecraft.world.item.BlockItem.updateCustomBlockEntityTag(helper.getLevel(), player, helper.absolutePos(ZL_RACK), drop);
        HeavyRackBlockEntity placed = helper.getBlockEntity(ZL_RACK);
        helper.assertTrue(placed.zone().equals("Food"), "a dismantled and placed rack keeps its zone, was '" + placed.zone() + "'");
        helper.assertTrue(placed.zoneFilter().items().size() == 1, "a dismantled and placed rack keeps its filter");
        helper.succeed();
    }

    /** The label menu names the label, parses tags and takes ghost items; the result stamps. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void zoneLabelMenuEditsDefinition(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ServusStorageItems.ZONE_LABEL.get()));
        ZoneLabelMenu menu = new ZoneLabelMenu(1, player.getInventory(), InteractionHand.MAIN_HAND);
        helper.assertTrue(ZoneDefinition.of(player.getItemInHand(InteractionHand.MAIN_HAND)) == null, "a fresh label defines no zone");

        menu.setName("  Ores ");
        menu.setTags("c:ores, #minecraft:logs, not a tag!!");
        menu.setCarried(new ItemStack(Items.BREAD, 5));
        menu.clicked(0, 0, ClickType.PICKUP, player);
        ItemStack labelStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        ZoneDefinition definition = ZoneDefinition.of(labelStack);
        helper.assertTrue(definition != null && definition.name().equals("Ores"), "the label should be named Ores");
        helper.assertTrue(definition.filter().tags().equals(List.of("c:ores", "minecraft:logs")), "tags should be parsed and cleaned, were " + definition.filter().tags());
        helper.assertTrue(definition.filter().items().size() == 1 && definition.filter().items().get(0).is(Items.BREAD)
                && definition.filter().items().get(0).getCount() == 1, "the ghost slot should put one bread on the label");
        helper.assertTrue(menu.getCarried().getCount() == 5, "the cursor keeps its bread");
        helper.assertTrue(labelStack.getHoverName().getString().equals("Ores"), "the item shows the zone name");

        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(ZoneDefinition.of(player.getItemInHand(InteractionHand.MAIN_HAND)).filter().items().isEmpty(), "an empty cursor clears the ghost slot");

        menu.setName("");
        helper.assertTrue(ZoneDefinition.of(player.getItemInHand(InteractionHand.MAIN_HAND)) == null, "a blank name unnames the label");
        menu.setName("Ores");

        helper.setBlock(ZL_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity crate = helper.getBlockEntity(ZL_CRATE);
        useLabel(helper, player, player.getItemInHand(InteractionHand.MAIN_HAND), ZL_CRATE);
        helper.assertTrue(crate.zone().equals("Ores") && crate.zoneFilter().tags().equals(List.of("c:ores", "minecraft:logs")),
                "the edited label should stamp name and tags, got " + crate.zone() + " " + crate.zoneFilter().tags());
        helper.succeed();
    }

    private static final BlockPos IZ_CONTROLLER = new BlockPos(6, 1, 9);
    private static final BlockPos IZ_ORES = new BlockPos(5, 1, 9);
    private static final BlockPos IZ_PLAIN = new BlockPos(7, 1, 9);
    private static final BlockPos IZ_SPARE = new BlockPos(6, 1, 10);

    private static final BlockPos IZ_IRON = new BlockPos(6, 1, 8);

    private static ZoneDefinition zone(String name, List<ItemStack> items, List<String> tags) {
        return new ZoneDefinition(name, new ZoneFilter(items, tags));
    }

    /** Inserts follow the zones: exact items beat tags, filtered blocks refuse other items, unzoned storage catches full zones. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void insertRespectsZones(GameTestHelper helper) {
        helper.setBlock(IZ_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (BlockPos pos : List.of(IZ_ORES, IZ_PLAIN, IZ_SPARE, IZ_IRON)) {
            helper.setBlock(pos, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        }
        CrateBlockEntity ores = helper.getBlockEntity(IZ_ORES);
        CrateBlockEntity iron = helper.getBlockEntity(IZ_IRON);
        CrateBlockEntity plain = helper.getBlockEntity(IZ_PLAIN);
        CrateBlockEntity spare = helper.getBlockEntity(IZ_SPARE);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(IZ_CONTROLLER);
        ores.setZone(zone("Ores", List.of(), List.of("c:ores")));
        iron.setZone(zone("Iron", List.of(new ItemStack(Items.IRON_ORE)), List.of()));
        plain.insert(new ItemStack(Items.BREAD, 1), false);

        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.status() == WarehouseNetwork.Status.ONLINE, "warehouse should be online");
            helper.assertTrue(network.zoneBlockCounts().equals(java.util.Map.of("ores", 1, "iron", 1)), "two zones with one block each, got " + network.zoneBlockCounts());

            ItemStack left = network.insert(new ItemStack(Items.IRON_ORE, 10));
            helper.assertTrue(left.isEmpty(), "iron ore should fit");
            helper.assertTrue(iron.count() == 10, "iron ore should land in the Iron crate (exact beats tag), had " + iron.count());
            helper.assertTrue(ores.isEmpty(), "the Ores crate should not take it while the exact match has room");

            left = network.insert(new ItemStack(Items.COPPER_ORE, 10));
            helper.assertTrue(left.isEmpty(), "copper ore should fit");
            helper.assertTrue(ores.count() == 10, "copper ore should land in the Ores crate by tag, had " + ores.count());
            helper.assertTrue(plain.count() == 1 && spare.isEmpty(), "unzoned crates must not take ore while a zone has room");

            left = network.insert(new ItemStack(Items.BREAD, 5));
            helper.assertTrue(left.isEmpty(), "bread should fit");
            helper.assertTrue(plain.count() == 6, "bread should join the unzoned crate already holding bread, had " + plain.count());

            iron.insert(new ItemStack(Items.IRON_ORE, 5000), false);
            ores.insert(new ItemStack(Items.COPPER_ORE, 5000), false);
            helper.assertTrue(iron.count() == iron.capacity() && ores.count() == ores.capacity(), "both zoned crates should be full");
            left = network.insert(new ItemStack(Items.IRON_ORE, 7));
            helper.assertTrue(left.isEmpty(), "with the zones full the unzoned crate takes the ore");
            helper.assertTrue(spare.count() == 7, "the ore should be in the unzoned spare crate, had " + spare.count());

            spare.insert(new ItemStack(Items.IRON_ORE, 5000), false);
            left = network.insert(new ItemStack(Items.IRON_ORE, 7));
            helper.assertTrue(left.getCount() == 7, "with everything full nothing is stored, left " + left.getCount());
            helper.succeed();
        });
    }

    private static final BlockPos ST_CONTROLLER = new BlockPos(9, 1, 3);
    private static final BlockPos ST_STATION = new BlockPos(10, 1, 3);
    private static final BlockPos ST_CRATE = new BlockPos(9, 1, 4);
    private static final BlockPos ST_CRATE_2 = new BlockPos(8, 1, 3);
    private static final BlockPos ST_CHEST = new BlockPos(11, 1, 3);

    private static boolean blocked(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(StorageNodeBlocks.ACTIVITY) == StorageNodeBlocks.Activity.BLOCKED;
    }

    private static InboundStationBlockEntity stationSetup(GameTestHelper helper) {
        helper.setBlock(ST_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(ST_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState()
                .setValue(InboundStationBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(ST_STATION);
    }

    /** A stack fed into the station lands in a crate within a few ticks when there are no shuttles. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void stationPutsAwayInstantly(GameTestHelper helper) {
        InboundStationBlockEntity station = stationSetup(helper);
        helper.setBlock(ST_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity crate = helper.getBlockEntity(ST_CRATE);
        helper.runAfterDelay(5, () -> {
            ItemStack left = ItemHandlerHelper.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 64), false);
            helper.assertTrue(left.isEmpty(), "the intake should take a stack");
            helper.assertTrue(station.intake().extractItem(0, 64, false).isEmpty(), "the intake must refuse extraction");
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(crate.count() == 64, "cobblestone should be in the crate, had " + crate.count());
                helper.assertTrue(station.isEmpty(), "the buffer should be empty");
                helper.assertTrue(!blocked(helper, ST_STATION), "lamp must not be red");
                helper.assertTrue(helper.getBlockState(ST_STATION).getValue(StorageNodeBlocks.ONLINE), "station shows the online lamp");
                helper.succeed();
            });
        });
    }

    /** With its zone full and no unzoned storage the stack waits with a red lamp, and leaves once an unzoned crate appears. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void stationBacksUpWhenZoneFull(GameTestHelper helper) {
        InboundStationBlockEntity station = stationSetup(helper);
        helper.setBlock(ST_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity full = helper.getBlockEntity(ST_CRATE);
        full.setZone(zone("Ores", List.of(), List.of("c:ores")));
        full.insert(new ItemStack(Items.IRON_ORE, 5000), false);
        helper.runAfterDelay(5, () -> {
            ItemHandlerHelper.insertItem(station.intake(), new ItemStack(Items.IRON_ORE, 7), false);
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(!station.isEmpty(), "the ore should wait in the buffer");
                helper.assertTrue(blocked(helper, ST_STATION), "the lamp should be red");
                helper.assertTrue(station.blockedReason() == PutAwayPlan.Reason.NO_ROOM,
                        "reason should be no room, was " + station.blockedReason());
                helper.assertTrue("Ores".equals(station.blockedZone()), "blocked zone should read Ores, was " + station.blockedZone());
                helper.setBlock(ST_CRATE_2, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
                CrateBlockEntity fresh = helper.getBlockEntity(ST_CRATE_2);
                helper.runAfterDelay(10, () -> {
                    helper.assertTrue(fresh.count() == 7, "the waiting ore should spill into the new unzoned crate, had " + fresh.count());
                    helper.assertTrue(station.isEmpty(), "the buffer should be empty again");
                    helper.assertTrue(!blocked(helper, ST_STATION), "the lamp should be back to normal");
                    helper.succeed();
                });
            });
        });
    }

    /** Neither a chest feeding the station nor the buffer itself counts as warehouse stock. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void chestOnStationIsNotStock(GameTestHelper helper) {
        InboundStationBlockEntity station = stationSetup(helper);
        helper.setBlock(ST_CHEST, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(ST_CHEST);
        chest.setItem(0, new ItemStack(Items.BREAD, 30));
        helper.runAfterDelay(5, () -> {
            ItemHandlerHelper.insertItem(station.intake(), new ItemStack(Items.BREAD, 10), false);
            helper.runAfterDelay(15, () -> {
                WarehouseControllerBlockEntity controller = helper.getBlockEntity(ST_CONTROLLER);
                WarehouseNetwork network = controller.network().orElseThrow();
                helper.assertTrue(network.index().count(ItemKey.of(new ItemStack(Items.BREAD))) == 0,
                        "bread beside or inside the station must not be indexed");
                helper.assertTrue(!station.isEmpty(), "with no storage the bread waits in the buffer");
                helper.assertTrue(station.blockedReason() == PutAwayPlan.Reason.NO_STORAGE, "reason should be no storage");
                helper.succeed();
            });
        });
    }

    private static final BlockPos DK_STATION = new BlockPos(5, 1, 5);

    /** Rails dock onto a station on every side, and the carrot dismantles it with its buffer. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void stationDocksAndDismantles(GameTestHelper helper) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            helper.setBlock(DK_STATION.relative(direction).below(), Blocks.STONE.defaultBlockState());
        }
        helper.setBlock(DK_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState()
                .setValue(InboundStationBlock.FACING, Direction.NORTH));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            placeRail(helper, DK_STATION.relative(direction));
        }
        InboundStationBlockEntity station = helper.getBlockEntity(DK_STATION);
        ItemHandlerHelper.insertItem(station.intake(), new ItemStack(Items.BREAD, 10), false);
        helper.runAfterDelay(2, () -> {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockState rail = helper.getBlockState(DK_STATION.relative(direction));
                helper.assertTrue(rail.getValue(RailBlock.dock(direction.getOpposite())),
                        "rail " + direction + " of the station should dock towards it");
            }
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack carrot = new ItemStack(ServusLibItems.CONFIGURATION_CARROT.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, carrot);
            player.setShiftKeyDown(true);
            BlockPos absolute = helper.absolutePos(DK_STATION);
            carrot.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
            helper.runAfterDelay(2, () -> {
                helper.assertBlockPresent(Blocks.AIR, DK_STATION);
                helper.assertItemEntityPresent(ServusStorageItems.INBOUND_STATION.get(), DK_STATION, 2.0);
                helper.assertItemEntityPresent(Items.BREAD, DK_STATION, 2.0);
                helper.succeed();
            });
        });
    }

    private static final BlockPos PA_CONTROLLER = new BlockPos(2, 1, 1);
    private static final BlockPos PA_STATION = new BlockPos(4, 1, 2);
    private static final BlockPos PA_CRATE = new BlockPos(8, 1, 2);
    private static final BlockPos PA_GARAGE = new BlockPos(3, 1, 2);

    /** A shuttle fetches a stack from the station and carries it along the rail to the crate. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void shuttleCarriesPutAway(GameTestHelper helper) {
        helper.setBlock(PA_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (int x = 3; x <= 8; x++) {
            helper.setBlock(new BlockPos(x, 0, 1), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 1), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(PA_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState()
                .setValue(InboundStationBlock.FACING, Direction.NORTH));
        helper.setBlock(PA_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        InboundStationBlockEntity station = helper.getBlockEntity(PA_STATION);
        CrateBlockEntity crate = helper.getBlockEntity(PA_CRATE);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(PA_CONTROLLER);
        helper.setBlock(PA_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(PA_GARAGE);
        helper.assertTrue(garage.addShuttle(), "one shuttle fits");

        helper.runAfterDelay(10, () -> {
            ItemHandlerHelper.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 40), false);
            helper.runAfterDelay(2, () -> helper.assertTrue(!station.isEmpty(), "the stack waits in the buffer until the shuttle arrives"));
            helper.runAfterDelay(12, () -> {
                helper.assertTrue(station.isEmpty(), "the shuttle should have loaded the stack by now");
                helper.assertTrue(crate.isEmpty(), "the crate must still be empty while the shuttle travels");
            });
            helper.runAfterDelay(70, () -> {
                helper.assertTrue(crate.count() == 40, "the shuttle should have delivered 40 cobblestone, crate has " + crate.count());
                helper.assertTrue(station.isEmpty(), "nothing should ride back");
                helper.assertTrue(controller.network().orElseThrow().summary().getString().contains("0 busy"),
                        "the shuttle should be home and idle: " + controller.network().orElseThrow().summary().getString());
                helper.succeed();
            });
        });
    }

    /** The station menu reports the warehouse state and shift click feeds the buffer. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void stationMenuFeedsBuffer(GameTestHelper helper) {
        InboundStationBlockEntity station = stationSetup(helper);
        helper.setBlock(ST_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity ores = helper.getBlockEntity(ST_CRATE);
        ores.setZone(zone("Ores", List.of(), List.of("c:ores")));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(5, () -> {
            InboundStationMenu menu = new InboundStationMenu(1, player.getInventory(), station);
            helper.assertTrue(menu.state().online(), "the station should see an online warehouse");
            helper.assertTrue(menu.state().idle(), "an empty buffer reads idle");

            player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 20));
            menu.quickMoveStack(player, menu.inventoryStart() + 27);
            helper.assertTrue(station.buffer().getStackInSlot(0).getCount() == 20 && station.buffer().getStackInSlot(0).is(Items.COBBLESTONE),
                    "shift click should move the cobblestone into the buffer");
            helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "the inventory slot should be empty");
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(!menu.state().blockedReason().isEmpty() && menu.state().blockedStack().is(Items.COBBLESTONE),
                        "cobblestone has no unzoned storage and the state should say so: " + menu.state());
                helper.succeed();
            });
        });
    }

    private static final BlockPos GA_GARAGE = new BlockPos(5, 1, 11);

    /** A garage takes shuttle items up to its capacity, refuses to be dismantled while it holds them, and breaks when empty. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void garageHoldsShuttles(GameTestHelper helper) {
        helper.setBlock(GA_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(GA_GARAGE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int capacity = garage.capacity();
        io.github.dirnei.mc.servus.storage.menu.ShuttleGarageMenu menu = new io.github.dirnei.mc.servus.storage.menu.ShuttleGarageMenu(1, player.getInventory(), garage);
        player.getInventory().setItem(0, new ItemStack(ServusStorageItems.SHUTTLE.get(), capacity + 1));
        for (int i = 0; i <= capacity; i++) {
            menu.quickMoveStack(player, menu.inventoryStart() + 27);
        }
        helper.assertTrue(garage.shuttleCount() == capacity, "one shift click per shuttle should fill every slot, filled " + garage.shuttleCount());
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 1, "one shuttle item should not fit");
        helper.assertTrue(!garage.shuttles().isItemValid(0, new ItemStack(Items.BREAD)), "only shuttles fit the slots");
        helper.assertTrue(garage.takeShuttle() && garage.takeShuttle(), "two shuttles can be taken back");
        helper.assertTrue(garage.shuttleCount() == capacity - 2, "count should drop by two");

        ItemStack carrot = new ItemStack(ServusLibItems.CONFIGURATION_CARROT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, carrot);
        player.setShiftKeyDown(true);
        BlockPos absolute = helper.absolutePos(GA_GARAGE);
        carrot.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
        helper.assertTrue(helper.getBlockState(GA_GARAGE).getBlock() instanceof ShuttleGarageBlock, "an occupied garage must not be dismantled");
        while (garage.takeShuttle()) {
            // empty it
        }
        carrot.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
        helper.runAfterDelay(2, () -> {
            helper.assertBlockPresent(Blocks.AIR, GA_GARAGE);
            helper.assertItemEntityPresent(ServusStorageItems.SHUTTLE_GARAGE.get(), GA_GARAGE, 2.0);
            helper.succeed();
        });
    }

    private static final BlockPos AR_GARAGE = new BlockPos(5, 1, 6);

    /** The rail behind a garage links to it as a plain arm; a rail in front neither links nor docks. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void garageRailIsAnArm(GameTestHelper helper) {
        helper.setBlock(AR_GARAGE.north().below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(AR_GARAGE.south().below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(AR_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
        placeRail(helper, AR_GARAGE.north());
        placeRail(helper, AR_GARAGE.south());
        helper.runAfterDelay(2, () -> {
            BlockState behind = helper.getBlockState(AR_GARAGE.south());
            helper.assertTrue(behind.getValue(ConnectableBlock.NORTH), "the rail behind should connect to the garage");
            helper.assertTrue(!behind.getValue(RailBlock.DOCK_NORTH), "the rail behind should show an arm, not a dock");
            BlockState front = helper.getBlockState(AR_GARAGE.north());
            helper.assertTrue(!front.getValue(ConnectableBlock.SOUTH), "the rail in front must not connect to the garage");
            helper.assertTrue(!front.getValue(RailBlock.DOCK_SOUTH), "the rail in front must not dock onto the garage");
            helper.succeed();
        });
    }

    private static final BlockPos TG_CONTROLLER = new BlockPos(1, 1, 3);
    private static final BlockPos TG_TERMINAL = new BlockPos(1, 1, 4);
    private static final BlockPos TG_GARAGE_A = new BlockPos(2, 1, 5);
    private static final BlockPos TG_GARAGE_B = new BlockPos(10, 1, 5);
    private static final BlockPos TG_CRATE = new BlockPos(9, 1, 5);

    /** With two garages the nearer shuttle serves and each shuttle returns to its own garage. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void shuttlesParkAtTheirGarage(GameTestHelper helper) {
        helper.setBlock(TG_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(TG_TERMINAL, ServusStorageBlocks.COUNTER_TERMINAL.get().defaultBlockState()
                .setValue(CounterTerminalBlock.FACING, Direction.NORTH));
        for (int x = 2; x <= 10; x++) {
            helper.setBlock(new BlockPos(x, 0, 4), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 4), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(TG_GARAGE_A, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        helper.setBlock(TG_GARAGE_B, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        helper.setBlock(TG_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        ShuttleGarageBlockEntity garageA = helper.getBlockEntity(TG_GARAGE_A);
        ShuttleGarageBlockEntity garageB = helper.getBlockEntity(TG_GARAGE_B);
        helper.assertTrue(garageA.addShuttle() && garageB.addShuttle(), "each garage takes a shuttle");
        CrateBlockEntity crate = helper.getBlockEntity(TG_CRATE);
        crate.insert(new ItemStack(Items.COBBLESTONE, 30), false);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(TG_CONTROLLER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.summary().getString().contains("2 garages, 2 shuttles"), "summary should count both garages: " + network.summary().getString());
            CounterTerminalBlockEntity terminal = helper.getBlockEntity(TG_TERMINAL);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64);
            helper.runAfterDelay(6, () -> {
                List<ShuttleView> a = network.shuttleViews(helper.absolutePos(TG_GARAGE_A));
                List<ShuttleView> b = network.shuttleViews(helper.absolutePos(TG_GARAGE_B));
                helper.assertTrue(a.isEmpty(), "garage A's shuttle should stay inside its garage and not be drawn");
                helper.assertTrue(b.size() == 1 && b.get(0).moving(), "garage B's shuttle, the nearer one, should be on its way");
            });
            helper.runAfterDelay(120, () -> {
                helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 30, "the cobblestone should have been delivered into the output slots");
                helper.assertTrue(network.shuttleViews(helper.absolutePos(TG_GARAGE_B)).isEmpty(), "garage B's shuttle should be back inside its garage");
                helper.assertTrue(network.summary().getString().contains("2 shuttles, 0 busy"), "both shuttles idle: " + network.summary().getString());
                helper.succeed();
            });
        });
    }

    private static final BlockPos RS_CONTROLLER = new BlockPos(1, 1, 8);
    private static final BlockPos RS_TERMINAL = new BlockPos(1, 1, 9);
    private static final BlockPos RS_RACK = new BlockPos(1, 1, 7);

    /** A rack is never served instantly: extract skips it, insert and terminal pushes leave it alone. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void rackNeedsShuttle(GameTestHelper helper) {
        helper.setBlock(RS_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(RS_TERMINAL, ServusStorageBlocks.COUNTER_TERMINAL.get().defaultBlockState()
                .setValue(CounterTerminalBlock.FACING, Direction.NORTH));
        helper.setBlock(RS_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        HeavyRackBlockEntity rack = helper.getBlockEntity(RS_RACK);
        rack.items().insertItem(0, new ItemStack(Items.BREAD, 40), false);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(RS_CONTROLLER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.index().count(ItemKey.of(new ItemStack(Items.BREAD))) == 40, "the rack's bread is indexed");
            helper.assertTrue(network.extract(ItemKey.of(new ItemStack(Items.BREAD)), 64).isEmpty(), "instant extract must skip the rack");
            helper.assertTrue(network.insert(new ItemStack(Items.COBBLESTONE, 20)).getCount() == 20, "instant insert must skip the rack");

            CounterTerminalBlockEntity terminal = helper.getBlockEntity(RS_TERMINAL);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 20));
            menu.quickMoveStack(player, 27);
            helper.assertTrue(player.getInventory().getItem(0).getCount() == 20, "a terminal push must not fill the rack");
            helper.assertTrue(rack.items().getStackInSlot(0).getCount() == 40 && rack.items().getStackInSlot(1).isEmpty(), "the rack is untouched");
            helper.succeed();
        });
    }

    private static final BlockPos SW_CONTROLLER = new BlockPos(1, 1, 11);
    private static final BlockPos SW_STATION = new BlockPos(3, 1, 10);
    private static final BlockPos SW_RACK = new BlockPos(5, 1, 10);
    private static final BlockPos SW_GARAGE = new BlockPos(6, 1, 10);

    /** A station stack whose only room is a rack waits with the needs a shuttle reason until a garage with a shuttle can reach the rack. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void stationWaitsForShuttleOnRacks(GameTestHelper helper) {
        helper.setBlock(SW_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (int x = 1; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 0, 11), Blocks.STONE.defaultBlockState());
        }
        for (int x = 2; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 1, 11), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(SW_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState().setValue(InboundStationBlock.FACING, Direction.NORTH));
        // The rack faces south, towards the rail behind it at z 11.
        helper.setBlock(SW_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.SOUTH));
        InboundStationBlockEntity station = helper.getBlockEntity(SW_STATION);
        HeavyRackBlockEntity rack = helper.getBlockEntity(SW_RACK);
        helper.runAfterDelay(10, () -> {
            ItemHandlerHelper.insertItem(station.intake(), new ItemStack(Items.BREAD, 10), false);
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(!station.isEmpty(), "the bread should wait: only a rack has room");
                helper.assertTrue(station.blockedReason() == PutAwayPlan.Reason.NO_SHUTTLE, "reason should be no shuttle, was " + station.blockedReason());
                helper.assertTrue(blocked(helper, SW_STATION), "the lamp should be red");
                helper.setBlock(SW_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
                ShuttleGarageBlockEntity garage = helper.getBlockEntity(SW_GARAGE);
                helper.assertTrue(garage.addShuttle(), "the garage takes a shuttle");
                helper.runAfterDelay(120, () -> {
                    int inRack = 0;
                    for (int i = 0; i < rack.items().getSlots(); i++) {
                        inRack += rack.items().getStackInSlot(i).is(Items.BREAD) ? rack.items().getStackInSlot(i).getCount() : 0;
                    }
                    helper.assertTrue(inRack == 10, "the shuttle should have carried the bread into the rack, rack has " + inRack);
                    helper.assertTrue(station.isEmpty(), "the buffer should be empty");
                    helper.assertTrue(!blocked(helper, SW_STATION), "the lamp should be normal again");
                    helper.succeed();
                });
            });
        });
    }

    private static final BlockPos RP_CONTROLLER = new BlockPos(8, 1, 10);
    private static final BlockPos RP_TERMINAL = new BlockPos(9, 1, 11);
    private static final BlockPos RP_RACK = new BlockPos(5, 1, 10);
    private static final BlockPos RP_GARAGE = new BlockPos(3, 1, 10);

    /** A rack request is refused without a garage and delivered into the output slots once a shuttle can reach the rack. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void rackPickByShuttle(GameTestHelper helper) {
        helper.setBlock(RP_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (int x = 3; x <= 8; x++) {
            helper.setBlock(new BlockPos(x, 0, 11), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 11), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(RP_TERMINAL, ServusStorageBlocks.COUNTER_TERMINAL.get().defaultBlockState().setValue(CounterTerminalBlock.FACING, Direction.NORTH));
        helper.setBlock(RP_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.SOUTH));
        HeavyRackBlockEntity rack = helper.getBlockEntity(RP_RACK);
        rack.items().insertItem(0, new ItemStack(Items.BREAD, 30), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = helper.getBlockEntity(RP_TERMINAL);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "online");
            menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 30);
            helper.assertTrue(menu.stock().message().equals("needs_shuttle"), "a rack request without a garage is refused, got " + menu.stock().message());
            helper.assertTrue(menu.stock().requests().isEmpty(), "no request is created");

            helper.setBlock(RP_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
            ShuttleGarageBlockEntity garage = helper.getBlockEntity(RP_GARAGE);
            helper.assertTrue(garage.addShuttle(), "the garage takes a shuttle");
            helper.runAfterDelay(5, () -> {
                menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 30);
                helper.assertTrue(menu.stock().message().isEmpty(), "the request should go through now, got " + menu.stock().message());
                helper.assertTrue(terminal.outputCount(Items.BREAD) == 0, "nothing arrives at once from a rack");
                helper.runAfterDelay(120, () -> {
                    helper.assertTrue(terminal.outputCount(Items.BREAD) == 30, "the shuttle should have brought 30 bread, output has " + terminal.outputCount(Items.BREAD));
                    helper.assertTrue(rack.isEmpty(), "the rack should be empty");
                    helper.assertTrue(menu.stock().requests().isEmpty(), "the request should be settled");
                    helper.succeed();
                });
            });
        });
    }

    private static final BlockPos RQ_CONTROLLER = new BlockPos(1, 1, 1);
    private static final BlockPos RQ_TERMINAL = new BlockPos(2, 1, 2);
    private static final BlockPos RQ_GARAGE = new BlockPos(3, 1, 2);
    private static final BlockPos RQ_CRATE = new BlockPos(6, 1, 2);

    private static CounterTerminalMenu requestSetup(GameTestHelper helper, Player player, int cobblestone) {
        helper.setBlock(RQ_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (int x = 2; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 0, 1), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 1), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(RQ_TERMINAL, ServusStorageBlocks.COUNTER_TERMINAL.get().defaultBlockState().setValue(CounterTerminalBlock.FACING, Direction.NORTH));
        helper.setBlock(RQ_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        helper.setBlock(RQ_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(RQ_GARAGE);
        garage.addShuttle();
        CrateBlockEntity crate = helper.getBlockEntity(RQ_CRATE);
        crate.insert(new ItemStack(Items.COBBLESTONE, cobblestone), false);
        CounterTerminalBlockEntity terminal = helper.getBlockEntity(RQ_TERMINAL);
        return new CounterTerminalMenu(1, player.getInventory(), terminal);
    }

    /** A request over a stack splits into two loads and shows its progress until both arrived. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void requestSplitsAndDelivers(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        CounterTerminalMenu menu = requestSetup(helper, player, 200);
        CounterTerminalBlockEntity terminal = menu.terminal();
        helper.runAfterDelay(10, () -> {
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 100);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request should go through");
            helper.runAfterDelay(6, () -> {
                List<RequestRow> rows = menu.stock().requests();
                helper.assertTrue(rows.size() == 1 && rows.get(0).requested() == 100 && rows.get(0).delivered() == 0, "one open request for 100, got " + rows);
                helper.assertTrue(rows.get(0).state().equals("PICKING") || rows.get(0).state().equals("WAITING_SHUTTLE"), "state should be picking or waiting, was " + rows.get(0).state());
            });
            helper.runAfterDelay(40, () -> {
                List<RequestRow> rows = menu.stock().requests();
                helper.assertTrue(rows.size() == 1 && rows.get(0).delivered() == 64 && rows.get(0).state().equals("PICKING"),
                        "the first load of 64 should have arrived while the second is picked, got " + rows);
            });
            helper.runAfterDelay(140, () -> {
                helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 100, "100 cobblestone should sit in the output slots, had " + terminal.outputCount(Items.COBBLESTONE));
                helper.assertTrue(menu.stock().requests().isEmpty(), "the request should be settled and gone");
                helper.succeed();
            });
        });
    }

    /** When the output slots are full the shuttle waits beside the terminal and unloads once a slot frees up. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void outputFullShuttleWaits(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        CounterTerminalMenu menu = requestSetup(helper, player, 64);
        CounterTerminalBlockEntity terminal = menu.terminal();
        for (int i = 0; i < terminal.output().getSlots(); i++) {
            terminal.output().setStackInSlot(i, new ItemStack(Items.DIRT, 64));
        }
        helper.runAfterDelay(10, () -> {
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64);
            helper.runAfterDelay(60, () -> {
                List<RequestRow> rows = menu.stock().requests();
                helper.assertTrue(rows.size() == 1 && rows.get(0).state().equals("WAITING_ROOM") && rows.get(0).delivered() == 0,
                        "the shuttle should be waiting for room, got " + rows);
                helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0, "nothing can land yet");
                terminal.output().setStackInSlot(0, ItemStack.EMPTY);
                helper.runAfterDelay(10, () -> {
                    helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 64, "the waiting shuttle should unload once a slot is free, had " + terminal.outputCount(Items.COBBLESTONE));
                    helper.assertTrue(menu.stock().requests().isEmpty(), "the request should be settled");
                    helper.succeed();
                });
            });
        });
    }

    private static final BlockPos CS_CONTROLLER = new BlockPos(6, 1, 6);
    private static final BlockPos CS_FULL = new BlockPos(5, 1, 6);
    private static final BlockPos CS_EMPTY = new BlockPos(7, 1, 6);

    /** The controller exposes the warehouse through the Lib stock capabilities. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void controllerExposesStock(GameTestHelper helper) {
        helper.setBlock(CS_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(CS_FULL, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(CS_EMPTY, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity full = helper.getBlockEntity(CS_FULL);
        CrateBlockEntity empty = helper.getBlockEntity(CS_EMPTY);
        full.insert(new ItemStack(Items.COBBLESTONE, 80), false);
        helper.runAfterDelay(10, () -> {
            BlockPos absolute = helper.absolutePos(CS_CONTROLLER);
            StockProvider provider = helper.getLevel().getCapability(ServusLibCapabilities.STOCK_PROVIDER, absolute, Direction.UP);
            StockSink sink = helper.getLevel().getCapability(ServusLibCapabilities.STOCK_SINK, absolute, null);
            helper.assertTrue(provider != null && sink != null, "the controller should expose both stock capabilities");
            helper.assertTrue(provider.count(new ItemStack(Items.COBBLESTONE)) == 80, "count should be 80, was " + provider.count(new ItemStack(Items.COBBLESTONE)));
            ItemStack taken = provider.extract(new ItemStack(Items.COBBLESTONE), 20);
            helper.assertTrue(taken.getCount() == 20 && full.count() == 60, "extract should take 20 and leave 60");
            ItemStack left = sink.insert(new ItemStack(Items.BREAD, 20));
            helper.assertTrue(left.isEmpty() && empty.count() == 20 && empty.type(0).is(Items.BREAD), "the sink should put the bread into the empty crate");
            helper.succeed();
        });
    }

    private static final BlockPos OS_CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos OS_STATION = new BlockPos(3, 1, 2);
    private static final BlockPos OS_FRONT = new BlockPos(4, 1, 2);
    private static final BlockPos OS_CRATE = new BlockPos(1, 1, 2);

    private static OutboundStationBlockEntity outboundSetup(GameTestHelper helper) {
        helper.setBlock(OS_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        // Facing west: the station's back, and so its machine, is the block east of it.
        helper.setBlock(OS_STATION, ServusStorageBlocks.OUTBOUND_STATION.get().defaultBlockState().setValue(OutboundStationBlock.FACING, Direction.WEST));
        helper.setBlock(OS_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(OS_STATION);
    }

    /** A keep slot pulls its amount out of storage into the buffer. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void outboundKeepsBufferStocked(GameTestHelper helper) {
        OutboundStationBlockEntity station = outboundSetup(helper);
        CrateBlockEntity crate = helper.getBlockEntity(OS_CRATE);
        crate.insert(new ItemStack(Items.STICK, 50), false);
        station.setKeep(0, new ItemStack(Items.STICK), 16);
        helper.runAfterDelay(StorageStatsManager.INSTANCE.outboundCheckInterval() + 20, () -> {
            helper.assertTrue(station.bufferCount(ItemKey.of(new ItemStack(Items.STICK))) == 16, "16 sticks should be in the buffer, had " + station.bufferCount(ItemKey.of(new ItemStack(Items.STICK))));
            helper.assertTrue(crate.count() == 34, "34 sticks should remain in the crate, had " + crate.count());
            helper.assertTrue(station.shortStack().isEmpty(), "nothing should be short");
            helper.succeed();
        });
    }

    /** What a hopper takes from an extract side is topped up again. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void outboundToppedUpAfterExtraction(GameTestHelper helper) {
        OutboundStationBlockEntity station = outboundSetup(helper);
        CrateBlockEntity crate = helper.getBlockEntity(OS_CRATE);
        crate.insert(new ItemStack(Items.STICK, 50), false);
        station.setKeep(0, new ItemStack(Items.STICK), 16);
        int interval = StorageStatsManager.INSTANCE.outboundCheckInterval();
        helper.runAfterDelay(interval + 20, () -> {
            helper.assertTrue(station.handlerFor(Direction.DOWN) == null, "the bottom is off by default");
            helper.assertTrue(station.cycleFace(Direction.DOWN) == SideMode.PUSH, "the first cycle turns the bottom to push");
            helper.assertTrue(station.cycleFace(Direction.DOWN) == SideMode.OUTPUT, "the second cycle turns the bottom to output");
            IItemHandler bottom = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(OS_STATION), Direction.DOWN);
            helper.assertTrue(bottom != null, "the extract side exposes a handler");
            helper.assertTrue(bottom.extractItem(0, 5, false).getCount() == 5, "5 sticks can be taken");
            helper.assertTrue(bottom.insertItem(1, new ItemStack(Items.COBBLESTONE, 3), false).getCount() == 3, "nothing goes in");
            helper.runAfterDelay(interval + 20, () -> {
                helper.assertTrue(station.bufferCount(ItemKey.of(new ItemStack(Items.STICK))) == 16, "the buffer should be topped up to 16, had " + station.bufferCount(ItemKey.of(new ItemStack(Items.STICK))));
                helper.assertTrue(crate.count() == 29, "29 sticks should remain in the crate, had " + crate.count());
                helper.succeed();
            });
        });
    }

    /** The buffer is pushed into the block in front. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void outboundPushesIntoFront(GameTestHelper helper) {
        OutboundStationBlockEntity station = outboundSetup(helper);
        helper.setBlock(OS_FRONT, Blocks.CHEST.defaultBlockState());
        station.buffer().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 8));
        helper.runAfterDelay(20, () -> {
            ChestBlockEntity chest = helper.getBlockEntity(OS_FRONT);
            helper.assertTrue(chest.countItem(Items.IRON_INGOT) == 8, "the chest should hold the 8 ingots, had " + chest.countItem(Items.IRON_INGOT));
            helper.assertTrue(station.isEmpty(), "the buffer should be empty");
            helper.succeed();
        });
    }

    /** Sides cycle push, output, off; off sides expose nothing, the back starts as push and the front is fixed off. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void outboundSideModes(GameTestHelper helper) {
        OutboundStationBlockEntity station = outboundSetup(helper);
        helper.assertTrue(station.sideMode(Direction.EAST) == SideMode.PUSH, "the back starts as push");
        helper.assertTrue(station.relativeSide(Direction.EAST) == RelativeSide.BACK, "east is the back of a station facing west");
        helper.assertTrue(station.handlerFor(Direction.EAST) != null, "a push side also lets neighbours extract");
        helper.assertTrue(station.sideMode(Direction.WEST) == SideMode.DISABLED && station.cycleFace(Direction.WEST) == SideMode.DISABLED, "the front is fixed off");
        helper.assertTrue(station.sideMode(Direction.UP) == SideMode.DISABLED && station.handlerFor(Direction.UP) == null, "other sides start off");
        helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.PUSH, "off cycles to push");
        helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.OUTPUT, "push cycles to output");
        helper.assertTrue(station.handlerFor(Direction.UP) != null, "an output side exposes the buffer");
        helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.DISABLED, "output cycles to off");
        helper.assertTrue(station.handlerFor(Direction.UP) == null, "an off side exposes nothing");
        station.sideConfig().set(RelativeSide.TOP, SideMode.BOTH);
        helper.assertTrue(station.sideMode(Direction.UP) == SideMode.DISABLED, "an unsupported mode falls back to off");
        helper.succeed();
    }

    /** A station in front of a Crafting Station reports its recipes. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void outboundReportsProducer(GameTestHelper helper) {
        outboundSetup(helper);
        craftingStationWithSticks(helper, OS_FRONT);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(OS_CONTROLLER);
        helper.runAfterDelay(5, () -> {
            List<WarehouseNetwork.Producer> producers = controller.network().orElseThrow().producers();
            helper.assertTrue(producers.size() == 1, "one station expected, got " + producers.size());
            helper.assertTrue(producers.get(0).producer() && producers.get(0).recipes().size() == 1
                    && producers.get(0).machine().equals(helper.absolutePos(OS_FRONT)), "the crafting station is the producer: " + producers.get(0));
            helper.succeed();
        });
    }

    /** A station facing a chest is listed without a producer. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void outboundFacingChestIsNoProducer(GameTestHelper helper) {
        outboundSetup(helper);
        helper.setBlock(OS_FRONT, Blocks.CHEST.defaultBlockState());
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(OS_CONTROLLER);
        helper.runAfterDelay(5, () -> {
            List<WarehouseNetwork.Producer> producers = controller.network().orElseThrow().producers();
            helper.assertTrue(producers.size() == 1 && !producers.get(0).producer() && producers.get(0).recipes().isEmpty(),
                    "the chest is no producer: " + producers);
            helper.succeed();
        });
    }

    /** Feeding a station from a crate touching the controller is instant. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void feedOutboundFromCrate(GameTestHelper helper) {
        OutboundStationBlockEntity station = outboundSetup(helper);
        CrateBlockEntity crate = helper.getBlockEntity(OS_CRATE);
        crate.insert(new ItemStack(Items.IRON_INGOT, 10), false);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(OS_CONTROLLER);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            String refused = network.feedStation(station, ItemKey.of(new ItemStack(Items.IRON_INGOT)), 4);
            helper.assertTrue(refused.isEmpty(), "the feed should go through, got " + refused);
            helper.assertTrue(station.bufferCount(ItemKey.of(new ItemStack(Items.IRON_INGOT))) == 4, "4 ingots should be in the buffer at once");
            helper.assertTrue(crate.count() == 6, "the crate should have 6 left");
            helper.assertTrue(network.feedStation(station, ItemKey.of(new ItemStack(Items.BREAD)), 4).equals("out_of_stock"), "nothing in stock is refused");
            helper.succeed();
        });
    }

    private static final BlockPos FD_CONTROLLER = new BlockPos(1, 1, 7);
    private static final BlockPos FD_RACK = new BlockPos(4, 1, 8);
    private static final BlockPos FD_GARAGE = new BlockPos(2, 1, 8);
    private static final BlockPos FD_STATION = new BlockPos(7, 1, 8);

    /** Feeding a station from a rack rides a shuttle. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void feedOutboundByShuttle(GameTestHelper helper) {
        helper.setBlock(FD_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (int x = 2; x <= 7; x++) {
            helper.setBlock(new BlockPos(x, 0, 7), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 7), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(FD_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        helper.setBlock(FD_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        helper.setBlock(FD_STATION, ServusStorageBlocks.OUTBOUND_STATION.get().defaultBlockState().setValue(OutboundStationBlock.FACING, Direction.SOUTH));
        HeavyRackBlockEntity rack = helper.getBlockEntity(FD_RACK);
        rack.items().insertItem(0, new ItemStack(Items.IRON_INGOT, 10), false);
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(FD_GARAGE);
        garage.addShuttle();
        OutboundStationBlockEntity station = helper.getBlockEntity(FD_STATION);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(FD_CONTROLLER);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            String refused = network.feedStation(station, ItemKey.of(new ItemStack(Items.IRON_INGOT)), 4);
            helper.assertTrue(refused.isEmpty(), "the feed should go through, got " + refused);
            helper.assertTrue(station.isEmpty(), "nothing arrives at once from a rack");
            helper.runAfterDelay(120, () -> {
                helper.assertTrue(station.bufferCount(ItemKey.of(new ItemStack(Items.IRON_INGOT))) == 4, "the shuttle should have brought 4 ingots, buffer has " + station.bufferCount(ItemKey.of(new ItemStack(Items.IRON_INGOT))));
                helper.succeed();
            });
        });
    }

    private static final BlockPos IS_STATION = new BlockPos(5, 1, 10);

    /** An inbound side switched off takes nothing until it is switched on again. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void inboundSideOff(GameTestHelper helper) {
        helper.setBlock(IS_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState().setValue(InboundStationBlock.FACING, Direction.NORTH));
        InboundStationBlockEntity station = helper.getBlockEntity(IS_STATION);
        helper.assertTrue(station.sideMode(Direction.NORTH) == SideMode.DISABLED && station.cycleFace(Direction.NORTH) == SideMode.DISABLED, "the front is fixed off");
        helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.PULL, "input cycles to pull");
        helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.DISABLED, "pull cycles to off");
        helper.setBlock(IS_STATION.above(), Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopper = helper.getBlockEntity(IS_STATION.above());
        hopper.setItem(0, new ItemStack(Items.BREAD, 10));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(station.isEmpty() && hopper.getItem(0).getCount() == 10, "the hopper should not get through an off side");
            helper.assertTrue(station.cycleFace(Direction.UP) == SideMode.INPUT, "off cycles to input");
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(!station.isEmpty(), "the hopper should feed the accepting side");
                helper.succeed();
            });
        });
    }

    private static final BlockPos IP_STATION = new BlockPos(8, 1, 10);

    /** A Pull side empties a chest into the buffer without a hopper. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void inboundPullsFromChest(GameTestHelper helper) {
        helper.setBlock(IP_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState().setValue(InboundStationBlock.FACING, Direction.NORTH));
        InboundStationBlockEntity station = helper.getBlockEntity(IP_STATION);
        helper.setBlock(IP_STATION.south(), Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(IP_STATION.south());
        chest.setItem(0, new ItemStack(Items.BREAD, 30));
        helper.assertTrue(station.relativeSide(Direction.SOUTH) == RelativeSide.BACK, "south is the back of a station facing north");
        station.sideConfig().set(RelativeSide.BACK, SideMode.PULL);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(chest.countItem(Items.BREAD) == 0, "the chest should be emptied, has " + chest.countItem(Items.BREAD));
            helper.assertTrue(station.buffer().getStackInSlot(0).getCount() == 30, "the bread should be in the buffer");
            helper.succeed();
        });
    }

    private static final BlockPos CS_STATION = new BlockPos(9, 1, 2);

    /** A Crafting Station side set to Off refuses a hopper; Pull takes used items from a chest. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void craftingStationSideOff(GameTestHelper helper) {
        CraftingStationBlockEntity station = craftingStationWithSticks(helper, CS_STATION);
        station.sideConfig().set(RelativeSide.TOP, SideMode.DISABLED);
        helper.setBlock(CS_STATION.above(), Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopper = helper.getBlockEntity(CS_STATION.above());
        hopper.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
        helper.setBlock(CS_STATION.west(), Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(CS_STATION.west());
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 6));
        chest.setItem(1, new ItemStack(Items.COBBLESTONE, 2));
        station.sideConfig().set(station.relativeSide(Direction.WEST), SideMode.PULL);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(hopper.getItem(0).getCount() == 4, "the hopper above an off side keeps its planks");
            helper.assertTrue(chest.countItem(Items.OAK_PLANKS) == 0 && chest.countItem(Items.COBBLESTONE) == 2, "the pull side takes planks and leaves cobblestone");
            helper.succeed();
        });
    }

    private static final BlockPos CR_STATION = new BlockPos(9, 1, 9);

    private static CraftingStationBlockEntity craftingStationWithSticks(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
        CraftingStationBlockEntity station = helper.getBlockEntity(pos);
        station.pattern().setStackInSlot(1, new ItemStack(Items.OAK_PLANKS));
        station.pattern().setStackInSlot(4, new ItemStack(Items.OAK_PLANKS));
        String saved = station.saveFromPattern();
        helper.assertTrue(saved.isEmpty(), "the stick pattern should save, got " + saved);
        return station;
    }

    /** A configured vanilla recipe is crafted from the inputs, one craft per work time. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void craftingStationCraftsConfiguredRecipe(GameTestHelper helper) {
        CraftingStationBlockEntity station = craftingStationWithSticks(helper, CR_STATION);
        helper.assertTrue(station.recipes().size() == 1 && station.recipes().get(0).result(helper.getLevel()).is(Items.STICK), "the sticks recipe should be configured");
        helper.assertTrue(station.saveFromPattern().equals("duplicate"), "saving the same pattern again is a duplicate");
        station.inputs().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 4));
        int ticks = station.workTicks();
        helper.runAfterDelay(ticks + 5, () -> {
            helper.assertTrue(station.outputs().getStackInSlot(0).is(Items.STICK) && station.outputs().getStackInSlot(0).getCount() == 4,
                    "four sticks should be in the outputs after one work time, got " + station.outputs().getStackInSlot(0));
            helper.assertTrue(station.inputs().getStackInSlot(0).getCount() == 2, "two planks should remain");
            helper.runAfterDelay(ticks + 2, () -> {
                helper.assertTrue(station.outputs().getStackInSlot(0).getCount() == 8, "eight sticks after the second craft, got " + station.outputs().getStackInSlot(0).getCount());
                helper.assertTrue(station.inputs().getStackInSlot(0).isEmpty(), "all planks used");
                station.removeRecipe(0);
                helper.assertTrue(station.recipes().isEmpty(), "the recipe should be removed");
                helper.succeed();
            });
        });
    }

    /** The station exposes its configured recipe through the Lib production capability. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void craftingStationReportsProduction(GameTestHelper helper) {
        CraftingStationBlockEntity station = craftingStationWithSticks(helper, CR_STATION);
        ProductionProvider provider = helper.getLevel().getCapability(ServusLibCapabilities.PRODUCTION, helper.absolutePos(CR_STATION), null);
        helper.assertTrue(provider != null && provider.recipes().size() == 1, "one recipe should be reported");
        ProductionRecipe sticks = provider.recipes().get(0);
        helper.assertTrue(sticks.inputs().size() == 1 && sticks.inputs().get(0).is(Items.OAK_PLANKS) && sticks.inputs().get(0).getCount() == 2,
                "two planks in, got " + sticks.inputs());
        helper.assertTrue(sticks.outputs().size() == 1 && sticks.outputs().get(0).is(Items.STICK) && sticks.outputs().get(0).getCount() == 4,
                "four sticks out, got " + sticks.outputs());
        helper.assertTrue(sticks.ticks() == station.workTicks(), "ticks should be the work time");
        helper.succeed();
    }

    /** Automation can insert only used items into the inputs and extract only from the outputs. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void craftingStationRefusesUnusedItems(GameTestHelper helper) {
        CraftingStationBlockEntity station = craftingStationWithSticks(helper, CR_STATION);
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(CR_STATION), Direction.UP);
        helper.assertTrue(handler != null, "the station should expose an item handler");
        helper.assertTrue(ItemHandlerHelper.insertItem(handler, new ItemStack(Items.COBBLESTONE, 5), false).getCount() == 5, "cobblestone is refused");
        helper.assertTrue(ItemHandlerHelper.insertItem(handler, new ItemStack(Items.OAK_PLANKS, 5), false).isEmpty(), "planks are accepted");
        helper.assertTrue(handler.extractItem(0, 5, false).isEmpty(), "inputs cannot be extracted");
        station.outputs().setStackInSlot(0, new ItemStack(Items.STICK, 3));
        helper.assertTrue(handler.extractItem(CraftingStationBlockEntity.INPUT_SLOTS, 3, false).getCount() == 3, "outputs can be extracted");
        helper.succeed();
    }

    private static final BlockPos DS_CONTROLLER = new BlockPos(2, 1, 5);
    private static final BlockPos DS_CRATE_A = new BlockPos(1, 1, 5);
    private static final BlockPos DS_OUTBOUND = new BlockPos(3, 1, 5);
    private static final BlockPos DS_STATION = new BlockPos(4, 1, 5);
    private static final BlockPos DS_CRATE_B = new BlockPos(3, 1, 6);
    private static final BlockPos DS_INBOUND = new BlockPos(4, 1, 6);

    /** An outbound station pushes planks into the station, an inbound station collects the sticks, which end in the crates. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void outboundServesCraftingStation(GameTestHelper helper) {
        helper.setBlock(DS_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(DS_CRATE_A, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(DS_CRATE_B, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(DS_OUTBOUND, ServusStorageBlocks.OUTBOUND_STATION.get().defaultBlockState().setValue(OutboundStationBlock.FACING, Direction.WEST));
        helper.setBlock(DS_INBOUND, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState().setValue(InboundStationBlock.FACING, Direction.SOUTH));
        CraftingStationBlockEntity station = craftingStationWithSticks(helper, DS_STATION);
        OutboundStationBlockEntity outbound = helper.getBlockEntity(DS_OUTBOUND);
        InboundStationBlockEntity inbound = helper.getBlockEntity(DS_INBOUND);
        CrateBlockEntity crateA = helper.getBlockEntity(DS_CRATE_A);
        CrateBlockEntity crateB = helper.getBlockEntity(DS_CRATE_B);
        outbound.buffer().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 4));
        helper.runAfterDelay(station.workTicks() * 2 + 80, () -> {
            int sticks = (crateA.count() > 0 && crateA.type(0).is(Items.STICK) ? crateA.count() : 0)
                    + (crateB.count() > 0 && crateB.type(0).is(Items.STICK) ? crateB.count() : 0);
            helper.assertTrue(sticks == 8, "eight sticks should have flowed into the crates, found " + sticks);
            helper.assertTrue(outbound.isEmpty() && inbound.isEmpty(), "both station buffers should be empty");
            helper.succeed();
        });
    }

    private static final BlockPos WO_CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos WO_TERMINAL = new BlockPos(2, 1, 3);
    private static final BlockPos WO_CRATE_A = new BlockPos(1, 1, 2);
    private static final BlockPos WO_CRATE_B = new BlockPos(1, 1, 3);
    private static final BlockPos WO_CRATE_C = new BlockPos(3, 1, 3);
    private static final BlockPos WO_OUTBOUND = new BlockPos(3, 1, 2);
    private static final BlockPos WO_INBOUND = new BlockPos(4, 1, 3);
    private static final BlockPos WO_STATION = new BlockPos(4, 1, 2);

    /** Controller, terminal, three crates, an outbound station facing a crafting station with the sticks recipe, an inbound station beside it. */
    private static CraftingStationBlockEntity orderSetup(GameTestHelper helper) {
        helper.setBlock(WO_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(WO_TERMINAL, ServusStorageBlocks.COUNTER_TERMINAL.get().defaultBlockState().setValue(CounterTerminalBlock.FACING, Direction.NORTH));
        for (BlockPos pos : List.of(WO_CRATE_A, WO_CRATE_B, WO_CRATE_C)) {
            helper.setBlock(pos, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        }
        helper.setBlock(WO_OUTBOUND, ServusStorageBlocks.OUTBOUND_STATION.get().defaultBlockState().setValue(OutboundStationBlock.FACING, Direction.WEST));
        helper.setBlock(WO_INBOUND, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState().setValue(InboundStationBlock.FACING, Direction.SOUTH));
        return craftingStationWithSticks(helper, WO_STATION);
    }

    /** A request that stock cannot fill creates an order; the station crafts and the sticks reach the output slots. */
    @GameTest(template = EMPTY, timeoutTicks = 500)
    public static void orderCraftsMissingItems(GameTestHelper helper) {
        orderSetup(helper);
        CrateBlockEntity planks = helper.getBlockEntity(WO_CRATE_A);
        planks.insert(new ItemStack(Items.OAK_PLANKS, 10), false);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(WO_CONTROLLER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = helper.getBlockEntity(WO_TERMINAL);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.STICK)), 8);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request should be accepted, got " + menu.stock().message());
            WarehouseNetwork network = controller.network().orElseThrow();
            List<WorkOrder> orders = network.orders().orders();
            helper.assertTrue(orders.size() == 1 && orders.get(0).amount() == 8 && orders.get(0).steps().size() == 1 && orders.get(0).steps().get(0).batches() == 2,
                    "one order with one sticks step of two batches, got " + orders);
            helper.assertTrue(orders.get(0).reserved().getOrDefault(ItemKey.of(new ItemStack(Items.OAK_PLANKS)), 0) == 4, "four planks should be reserved");
            List<RequestRow> rows = menu.stock().requests();
            helper.assertTrue(rows.size() == 1 && rows.get(0).state().equals("PRODUCING"), "the request should be producing, got " + rows);
            helper.runAfterDelay(400, () -> {
                helper.assertTrue(terminal.outputCount(Items.STICK) == 8, "eight sticks should have reached the output slots, had " + terminal.outputCount(Items.STICK));
                helper.assertTrue(planks.count() == 6, "four planks should have been used, crate has " + planks.count());
                helper.assertTrue(network.orders().orders().stream().noneMatch(WorkOrder::open), "the order should be done: " + network.orders().orders());
                helper.assertTrue(menu.stock().requests().isEmpty(), "the request should be settled");
                helper.succeed();
            });
        });
    }

    /** Nothing makes bread: no order, the request is refused as out of stock, a manual order says no producer. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void orderWithoutProducerIsRefused(GameTestHelper helper) {
        orderSetup(helper);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(WO_CONTROLLER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = helper.getBlockEntity(WO_TERMINAL);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 5);
            helper.assertTrue(menu.stock().message().equals("out_of_stock"), "bread cannot be made: " + menu.stock().message());
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.orders().orders().isEmpty() && menu.stock().requests().isEmpty(), "no order and no request");
            helper.assertTrue(network.order(ItemKey.of(new ItemStack(Items.BREAD)), 5).equals("no_producer"), "a manual bread order says no producer");
            helper.succeed();
        });
    }

    /** Sticks from planks from logs: the planks step runs first, then the sticks step, on the same dock. */
    @GameTest(template = EMPTY, timeoutTicks = 700)
    public static void twoLevelOrderRunsLeavesFirst(GameTestHelper helper) {
        CraftingStationBlockEntity station = orderSetup(helper);
        station.pattern().setStackInSlot(0, ItemStack.EMPTY);
        for (int i = 0; i < CraftingStationBlockEntity.GRID; i++) {
            station.pattern().setStackInSlot(i, ItemStack.EMPTY);
        }
        station.pattern().setStackInSlot(4, new ItemStack(Items.OAK_LOG));
        helper.assertTrue(station.saveFromPattern().isEmpty(), "the planks pattern should save");
        CrateBlockEntity logs = helper.getBlockEntity(WO_CRATE_A);
        logs.insert(new ItemStack(Items.OAK_LOG, 2), false);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(WO_CONTROLLER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            CounterTerminalBlockEntity terminal = helper.getBlockEntity(WO_TERMINAL);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.STICK)), 8);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request should be accepted, got " + menu.stock().message());
            WarehouseNetwork network = controller.network().orElseThrow();
            List<WorkOrder> orders = network.orders().orders();
            helper.assertTrue(orders.size() == 1 && orders.get(0).steps().size() == 2, "one order with two steps, got " + orders);
            List<OrderStep> steps = orders.get(0).steps();
            helper.assertTrue(steps.get(0).output().is(Items.OAK_PLANKS) && steps.get(0).batches() == 1, "planks first, one batch: " + steps.get(0).output());
            helper.assertTrue(steps.get(1).output().is(Items.STICK) && steps.get(1).batches() == 2, "sticks second, two batches: " + steps.get(1).output());
            helper.assertTrue(orders.get(0).reserved().getOrDefault(ItemKey.of(new ItemStack(Items.OAK_LOG)), 0) == 1, "one log should be reserved");
            helper.runAfterDelay(600, () -> {
                helper.assertTrue(terminal.outputCount(Items.STICK) == 8, "eight sticks should have reached the output slots, had " + terminal.outputCount(Items.STICK)
                        + " orders " + network.orders().orders() + " steps " + (network.orders().orders().isEmpty() ? "" : network.orders().orders().get(0).steps().stream().map(st -> st.state() + "/" + st.reason()).toList()));
                helper.assertTrue(logs.count() == 1, "one log should be left, crate has " + logs.count());
                helper.succeed();
            });
        });
    }

    /** A second order finds the first order's planks reserved; cancelling the first frees them. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void cancelReleasesReservation(GameTestHelper helper) {
        orderSetup(helper);
        CrateBlockEntity planks = helper.getBlockEntity(WO_CRATE_A);
        planks.insert(new ItemStack(Items.OAK_PLANKS, 4), false);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(WO_CONTROLLER);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            OrderBook book = network.orders();
            ItemKey sticks = ItemKey.of(new ItemStack(Items.STICK));
            ItemKey plankKey = ItemKey.of(new ItemStack(Items.OAK_PLANKS));
            OrderBook.Created first = book.create(sticks, 8, 0);
            OrderBook.Created second = book.create(sticks, 8, 0);
            helper.assertTrue(first.made() && second.made(), "both orders should be created");
            helper.assertTrue(first.order().reserved().getOrDefault(plankKey, 0) == 4, "the first order reserves the four planks");
            helper.assertTrue(second.order().state() == WorkOrder.State.BLOCKED && second.order().reason().startsWith("missing"),
                    "the second order finds no planks: " + second.order().state() + " " + second.order().reason());
            helper.assertTrue(book.reservedTotal().getOrDefault(plankKey, 0) == 4, "four planks reserved in total");
            helper.assertTrue(book.cancel(first.order().id()), "the first order can be cancelled");
            helper.assertTrue(book.reservedTotal().getOrDefault(plankKey, 0) == 0, "cancelling frees the planks");
            helper.assertTrue(first.order().state() == WorkOrder.State.CANCELLED, "the first order is cancelled");
            helper.succeed();
        });
    }

    private static RuleRow ruleRow(WarehouseNetwork network, Item item) {
        for (RuleRow row : network.ruleRows()) {
            if (row.stack().is(item)) {
                return row;
            }
        }
        return null;
    }

    /** A rule below its minimum orders up to the maximum and reads ok once the sticks are in stock. */
    @GameTest(template = EMPTY, timeoutTicks = 800)
    public static void stockRuleOrdersWhenLow(GameTestHelper helper) {
        orderSetup(helper);
        CrateBlockEntity planks = helper.getBlockEntity(WO_CRATE_A);
        planks.insert(new ItemStack(Items.OAK_PLANKS, 10), false);
        CrateBlockEntity sticks = helper.getBlockEntity(WO_CRATE_B);
        sticks.insert(new ItemStack(Items.STICK, 2), false);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(WO_CONTROLLER);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            network.setStockRule(ItemKey.of(new ItemStack(Items.STICK)), 8, 16);
            helper.runAfterDelay(30, () -> {
                List<WorkOrder> orders = network.orders().orders();
                helper.assertTrue(orders.size() == 1 && orders.get(0).amount() == 14 && orders.get(0).requestId() == io.github.dirnei.mc.servus.storage.order.OrderBook.RULE_ORIGIN,
                        "the rule should order 14 sticks, got " + orders);
                RuleRow row = ruleRow(network, Items.STICK);
                helper.assertTrue(row != null && row.state().equals("ordering"), "the rule should read ordering: " + row);
                helper.runAfterDelay(600, () -> {
                    helper.assertTrue(network.index().count(ItemKey.of(new ItemStack(Items.STICK))) >= 16,
                            "at least 16 sticks should be in stock, have " + network.index().count(ItemKey.of(new ItemStack(Items.STICK))));
                    RuleRow after = ruleRow(network, Items.STICK);
                    helper.assertTrue(after != null && after.state().equals("ok"), "the rule should read ok: " + after);
                    helper.succeed();
                });
            });
        });
    }

    /** A rule above its minimum orders nothing. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void stockRuleIgnoresWhenEnough(GameTestHelper helper) {
        orderSetup(helper);
        CrateBlockEntity sticks = helper.getBlockEntity(WO_CRATE_B);
        sticks.insert(new ItemStack(Items.STICK, 10), false);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(WO_CONTROLLER);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            network.setStockRule(ItemKey.of(new ItemStack(Items.STICK)), 8, 16);
            helper.runAfterDelay(150, () -> {
                helper.assertTrue(network.orders().orders().isEmpty(), "no order should be created");
                RuleRow row = ruleRow(network, Items.STICK);
                helper.assertTrue(row != null && row.state().equals("ok"), "the rule should read ok: " + row);
                helper.succeed();
            });
        });
    }

    /** A rule for an item nothing makes reads no producer and orders nothing. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void stockRuleWithoutProducerShowsReason(GameTestHelper helper) {
        orderSetup(helper);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(WO_CONTROLLER);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            network.setStockRule(ItemKey.of(new ItemStack(Items.BREAD)), 8, 16);
            helper.runAfterDelay(150, () -> {
                helper.assertTrue(network.orders().orders().isEmpty(), "no order should be created");
                RuleRow row = ruleRow(network, Items.BREAD);
                helper.assertTrue(row != null && row.state().equals("no_producer"), "the rule should read no producer: " + row);
                helper.assertTrue(network.removeStockRule(ItemKey.of(new ItemStack(Items.BREAD))), "the rule can be removed");
                helper.assertTrue(network.ruleRows().isEmpty(), "no rules left");
                helper.succeed();
            });
        });
    }

    /** Rules survive a save and load of the controller. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void stockRulesPersist(GameTestHelper helper) {
        helper.setBlock(WO_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(WO_CONTROLLER);
        controller.rules().set(new StockRule(ItemKey.of(new ItemStack(Items.STICK)), 8, 16));
        net.minecraft.nbt.CompoundTag tag = controller.saveWithoutMetadata(helper.getLevel().registryAccess());
        WarehouseControllerBlockEntity copy = new WarehouseControllerBlockEntity(helper.absolutePos(WO_CONTROLLER), controller.getBlockState());
        copy.loadWithComponents(tag, helper.getLevel().registryAccess());
        StockRule rule = copy.rules().get(ItemKey.of(new ItemStack(Items.STICK)));
        helper.assertTrue(rule != null && rule.min() == 8 && rule.max() == 16, "the rule should survive save and load: " + rule);
        helper.succeed();
    }

    private static final BlockPos WO_BOARD = new BlockPos(2, 1, 1);

    /** The board beside the controller lists the dock's station and an order with its step. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void orderBoardListsMachinesAndOrders(GameTestHelper helper) {
        orderSetup(helper);
        helper.setBlock(WO_BOARD, ServusStorageBlocks.ORDER_BOARD.get().defaultBlockState().setValue(OrderBoardBlock.FACING, Direction.NORTH));
        CrateBlockEntity planks = helper.getBlockEntity(WO_CRATE_A);
        planks.insert(new ItemStack(Items.OAK_PLANKS, 10), false);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(WO_CONTROLLER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            OrderBoardBlockEntity board = helper.getBlockEntity(WO_BOARD);
            OrderBoardMenu menu = new OrderBoardMenu(1, player.getInventory(), board);
            OrderBoardPayload before = menu.view();
            helper.assertTrue(before.online(), "the board should be online, reason " + before.reason());
            helper.assertTrue(before.machines().size() == 1 && before.machines().get(0).machine().equals("Crafting Station")
                    && before.machines().get(0).state().equals("idle"), "one idle Crafting Station expected, got " + before.machines());
            helper.assertTrue(before.orders().isEmpty(), "no orders yet");
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.order(ItemKey.of(new ItemStack(Items.STICK)), 8).isEmpty(), "the manual order should be accepted");
            helper.runAfterDelay(60, () -> {
                OrderBoardPayload view = menu.view();
                helper.assertTrue(view.orders().size() == 1, "one order expected, got " + view.orders());
                OrderRow order = view.orders().get(0);
                helper.assertTrue(order.item().is(Items.STICK) && order.amount() == 8 && order.requestId() == 0, "manual sticks order: " + order);
                helper.assertTrue(order.steps().size() == 1 && order.steps().get(0).machine().equals("Crafting Station")
                        && order.steps().get(0).output().is(Items.STICK) && order.steps().get(0).batches() == 2, "one sticks step on the station: " + order.steps());
                String stepState = order.steps().get(0).state();
                helper.assertTrue(stepState.equals("delivering") || stepState.equals("producing") || stepState.equals("done"), "the step should have started: " + stepState);
                boolean working = view.machines().get(0).state().equals("working") || stepState.equals("done");
                helper.assertTrue(working || stepState.equals("delivering"), "the station should be working or still being fed: " + view.machines());
                helper.succeed();
            });
        });
    }

    /** Cancelling through the board menu cancels the order and releases its reservation. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void orderBoardCancelsOrder(GameTestHelper helper) {
        orderSetup(helper);
        helper.setBlock(WO_BOARD, ServusStorageBlocks.ORDER_BOARD.get().defaultBlockState().setValue(OrderBoardBlock.FACING, Direction.NORTH));
        CrateBlockEntity planks = helper.getBlockEntity(WO_CRATE_A);
        planks.insert(new ItemStack(Items.OAK_PLANKS, 4), false);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(WO_CONTROLLER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            OrderBook.Created created = network.orders().create(ItemKey.of(new ItemStack(Items.STICK)), 8, 0);
            helper.assertTrue(created.made(), "the order should be created");
            OrderBoardBlockEntity board = helper.getBlockEntity(WO_BOARD);
            OrderBoardMenu menu = new OrderBoardMenu(1, player.getInventory(), board);
            helper.assertTrue(menu.view().orders().size() == 1 && menu.view().orders().get(0).open(), "the order is listed open");
            helper.assertTrue(menu.cancel(created.order().id()), "cancel should succeed");
            helper.assertTrue(menu.view().orders().get(0).state().equals("cancelled"), "the order shows cancelled: " + menu.view().orders());
            helper.assertTrue(network.orders().reservedTotal().isEmpty(), "reservations released");
            helper.assertTrue(!menu.cancel(created.order().id()), "cancelling twice does nothing");
            helper.succeed();
        });
    }

    private static final BlockPos RC_CONTROLLER = new BlockPos(1, 1, 3);
    private static final BlockPos RC_TERMINAL = new BlockPos(1, 1, 4);
    private static final BlockPos RC_GARAGE = new BlockPos(2, 1, 5);
    private static final BlockPos RC_RACK = new BlockPos(9, 1, 5);
    private static final BlockPos RC_CRATE = new BlockPos(1, 1, 5);

    /** Controller, terminal, a rail along z 4, a garage behind the rail with one shuttle and a crate beside it, a rack with cobblestone at the far end. */
    private static WarehouseControllerBlockEntity recallSetup(GameTestHelper helper) {
        helper.setBlock(RC_CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(RC_TERMINAL, ServusStorageBlocks.COUNTER_TERMINAL.get().defaultBlockState().setValue(CounterTerminalBlock.FACING, Direction.NORTH));
        for (int x = 2; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 0, 4), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(x, 1, 4), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(RC_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        // The crate keeps the garage in the warehouse when the rail behind it is broken.
        helper.setBlock(RC_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(RC_RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(RC_GARAGE);
        helper.assertTrue(garage.addShuttle(), "the garage takes a shuttle");
        HeavyRackBlockEntity rack = helper.getBlockEntity(RC_RACK);
        rack.items().insertItem(0, new ItemStack(Items.COBBLESTONE, 30), false);
        return helper.getBlockEntity(RC_CONTROLLER);
    }

    /** Recall brings a shuttle that is out on a pick back inside at once; the pick is queued again. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void garageRecallBringsShuttlesHome(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = recallSetup(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            CounterTerminalBlockEntity terminal = helper.getBlockEntity(RC_TERMINAL);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 30);
            helper.runAfterDelay(8, () -> {
                BlockPos garagePos = helper.absolutePos(RC_GARAGE);
                helper.assertTrue(network.garageOut(garagePos) == 1, "the shuttle should be out on the pick");
                network.recallGarage(garagePos);
                helper.assertTrue(network.garageOut(garagePos) == 0, "after the recall nothing is out");
                helper.assertTrue(network.shuttleViews(garagePos).isEmpty(), "the recalled shuttle sits inside and is not drawn");
                helper.runAfterDelay(160, () -> {
                    helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 30, "the queued pick is taken up again and delivered, got " + terminal.outputCount(Items.COBBLESTONE));
                    helper.succeed();
                });
            });
        });
    }

    private static final BlockPos PG_GARAGE_B = new BlockPos(8, 1, 5);

    /** A paused garage hands out no shuttle; the active one serves instead. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void pausedGarageTakesNoJobs(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = recallSetup(helper);
        helper.setBlock(PG_GARAGE_B, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
        ShuttleGarageBlockEntity garageB = helper.getBlockEntity(PG_GARAGE_B);
        helper.assertTrue(garageB.addShuttle(), "garage B takes a shuttle");
        garageB.setActive(false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.garageSummary(helper.absolutePos(PG_GARAGE_B)).getString().contains("paused"), "the summary says paused");
            CounterTerminalBlockEntity terminal = helper.getBlockEntity(RC_TERMINAL);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 30);
            helper.runAfterDelay(8, () -> {
                helper.assertTrue(network.garageOut(helper.absolutePos(PG_GARAGE_B)) == 0, "the paused garage, though nearer, sends nothing");
                helper.assertTrue(network.garageOut(helper.absolutePos(RC_GARAGE)) == 1, "the active garage sends its shuttle");
                garageB.setActive(true);
                helper.assertTrue(!network.garageSummary(helper.absolutePos(PG_GARAGE_B)).getString().contains("paused"), "resumed");
                helper.succeed();
            });
        });
    }

    /** Breaking the rail behind a garage while its shuttle is out brings the shuttle inside instead of freezing it. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void losingTheRailRecalls(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = recallSetup(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            CounterTerminalBlockEntity terminal = helper.getBlockEntity(RC_TERMINAL);
            CounterTerminalMenu menu = new CounterTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 30);
            helper.runAfterDelay(8, () -> {
                BlockPos garagePos = helper.absolutePos(RC_GARAGE);
                helper.assertTrue(network.garageOut(garagePos) == 1, "the shuttle should be out on the pick");
                helper.setBlock(RC_GARAGE.north(), Blocks.AIR.defaultBlockState());
                helper.runAfterDelay(5, () -> {
                    // The rail line split off, so the controller's part is a rebuilt network object.
                    WarehouseNetwork after = controller.network().orElseThrow();
                    ShuttleGarageBlockEntity garage = helper.getBlockEntity(RC_GARAGE);
                    helper.assertTrue(garage.shuttleCount() == 1, "the garage still holds its shuttle");
                    helper.assertTrue(after.garageOut(garagePos) == 0, "without a home the shuttle is recalled, out is " + after.garageOut(garagePos));
                    helper.assertTrue(after.shuttleViews(garagePos).isEmpty(), "nothing is drawn on the rails");
                    helper.assertTrue(after.garageSummary(garagePos).getString().contains("no rail"), "the summary says no rail: " + after.garageSummary(garagePos).getString());
                    helper.succeed();
                });
            });
        });
    }
}
