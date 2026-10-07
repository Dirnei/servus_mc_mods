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
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.rail.RailBlock;
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
        helper.setBlock(SH_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
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
        helper.setBlock(PA_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
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
        helper.setBlock(GA_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
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
        helper.setBlock(TG_GARAGE_A, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
        helper.setBlock(TG_GARAGE_B, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
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
                helper.setBlock(SW_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
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

            helper.setBlock(RP_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.SOUTH));
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
        helper.setBlock(RQ_GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
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
}
