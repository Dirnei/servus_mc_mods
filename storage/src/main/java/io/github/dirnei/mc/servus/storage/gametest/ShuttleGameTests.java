package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.machine.MachineTestSupport;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.ShuttleGarageMenu;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.rail.RailBlock;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.shuttle.ShuttleView;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlock;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.zone.PutAwayPlan;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Shuttles and garages: picks, put-aways, parking, pausing and recalls. */
public final class ShuttleGameTests {

    private static final BlockPos SH_CONTROLLER = new BlockPos(2, 1, 6);
    private static final BlockPos SH_TERMINAL = new BlockPos(3, 1, 7);
    private static final BlockPos SH_FAR_RACK = new BlockPos(7, 1, 7);
    private static final BlockPos SH_GARAGE = new BlockPos(3, 1, 5);
    private static final BlockPos PA_CONTROLLER = new BlockPos(2, 1, 1);
    private static final BlockPos PA_STATION = new BlockPos(4, 1, 2);
    private static final BlockPos PA_RACK = new BlockPos(8, 1, 2);
    private static final BlockPos PA_GARAGE = new BlockPos(3, 1, 2);
    private static final BlockPos GA_GARAGE = new BlockPos(5, 1, 11);
    private static final BlockPos AR_GARAGE = new BlockPos(5, 1, 6);
    private static final BlockPos TG_CONTROLLER = new BlockPos(1, 1, 3);
    private static final BlockPos TG_TERMINAL = new BlockPos(1, 1, 4);
    private static final BlockPos TG_GARAGE_A = new BlockPos(2, 1, 5);
    private static final BlockPos TG_GARAGE_B = new BlockPos(10, 1, 5);
    private static final BlockPos TG_RACK = new BlockPos(9, 1, 5);
    private static final BlockPos RS_CONTROLLER = new BlockPos(1, 1, 8);
    private static final BlockPos RS_TERMINAL = new BlockPos(1, 1, 9);
    private static final BlockPos RS_RACK = new BlockPos(1, 1, 7);
    private static final BlockPos SW_CONTROLLER = new BlockPos(1, 1, 11);
    private static final BlockPos SW_STATION = new BlockPos(3, 1, 10);
    private static final BlockPos SW_RACK = new BlockPos(5, 1, 10);
    private static final BlockPos SW_GARAGE = new BlockPos(6, 1, 10);
    private static final BlockPos RP_CONTROLLER = new BlockPos(8, 1, 10);
    private static final BlockPos RP_TERMINAL = new BlockPos(9, 1, 11);
    private static final BlockPos RP_RACK = new BlockPos(5, 1, 10);
    private static final BlockPos RP_GARAGE = new BlockPos(3, 1, 10);
    private static final BlockPos RC_CONTROLLER = new BlockPos(1, 1, 3);
    private static final BlockPos RC_TERMINAL = new BlockPos(1, 1, 4);
    private static final BlockPos RC_GARAGE = new BlockPos(2, 1, 5);
    private static final BlockPos RC_RACK = new BlockPos(9, 1, 5);
    private static final BlockPos PG_GARAGE_B = new BlockPos(8, 1, 5);

    private ShuttleGameTests() {
    }

    /** A shuttle fetches from a rack at the end of the rail; what the rack lacks stays missing on the request. */
    @ServusGameTest(timeoutTicks = 400)
    public static void shuttlePicksFromRail(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, SH_CONTROLLER);
        TerminalWiring.place(helper, SH_TERMINAL);
        WarehouseFixtures.railRow(helper, 3, 7, 6);
        HeavyRackBlockEntity far = WarehouseFixtures.rack(helper, SH_FAR_RACK, Direction.NORTH);
        WarehouseFixtures.stock(helper, SH_FAR_RACK, new ItemStack(Items.COBBLESTONE, 40));
        ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, SH_GARAGE, Direction.NORTH, 1);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, SH_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online");
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64);
            helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0, "cobblestone must not arrive at once");
            helper.runAfterDelay(20, () -> helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0,
                    "shuttle is still on its way after 20 ticks"));
            Waits.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 40
                            && menu.stock().requests().size() == 1 && menu.stock().requests().get(0).state().equals("MISSING"), 360,
                    () -> "40 cobblestone should arrive, had " + terminal.outputCount(Items.COBBLESTONE) + ", rows " + menu.stock().requests(), () -> {
                helper.assertTrue(far.isEmpty(), "far rack should be empty");
                List<RequestRow> rows = menu.stock().requests();
                helper.assertTrue(rows.size() == 1 && rows.get(0).missing() == 24 && rows.get(0).state().equals("MISSING"),
                        "the cobblestone request shows 24 missing, got " + rows);
                helper.succeed();
            });
        });
    }

    /** A shuttle fetches a stack from the station and carries it along the rail to the rack. */
    @ServusGameTest(timeoutTicks = 400)
    public static void shuttleCarriesPutAway(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, PA_CONTROLLER);
        WarehouseFixtures.railRow(helper, 3, 8, 1);
        helper.setBlock(PA_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState()
                .setValue(InboundStationBlock.FACING, Direction.NORTH));
        WarehouseFixtures.rack(helper, PA_RACK, Direction.NORTH);
        InboundStationBlockEntity station = helper.getBlockEntity(PA_STATION, InboundStationBlockEntity.class);
        HeavyRackBlockEntity crate = helper.getBlockEntity(PA_RACK, HeavyRackBlockEntity.class);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(PA_CONTROLLER, WarehouseControllerBlockEntity.class);
        ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, PA_GARAGE, Direction.SOUTH, 1);

        helper.runAfterDelay(10, () -> {
            Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 40), false);
            helper.runAfterDelay(2, () -> helper.assertTrue(!station.isEmpty(), "the stack waits in the buffer until the shuttle arrives"));
            // The shuttle speeds up and brakes before it loads.
            helper.runAfterDelay(45, () -> {
                helper.assertTrue(station.isEmpty(), "the shuttle should have loaded the stack by now");
                helper.assertTrue(crate.isEmpty(), "the rack must still be empty while the shuttle travels");
            });
            helper.runAfterDelay(140, () -> {
                helper.assertTrue(WarehouseFixtures.count(crate, Items.COBBLESTONE) == 40, "the shuttle should have delivered 40 cobblestone, rack has "
                        + WarehouseFixtures.count(crate, Items.COBBLESTONE));
                helper.assertTrue(station.isEmpty(), "nothing should ride back");
                helper.assertTrue(controller.network().orElseThrow().summary().getString().contains("0 busy"),
                        "the shuttle should be home and idle: " + controller.network().orElseThrow().summary().getString());
                helper.succeed();
            });
        });
    }

    /** A rack on the station's own rail node: the shuttle's next leg has one node and must still complete. */
    @ServusGameTest(timeoutTicks = 400)
    public static void shuttlePutAwayOnStationRail(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, PA_CONTROLLER);
        WarehouseFixtures.railRow(helper, 3, 8, 1);
        BlockPos rackBeside = new BlockPos(4, 1, 0);
        helper.setBlock(PA_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState()
                .setValue(InboundStationBlock.FACING, Direction.NORTH));
        WarehouseFixtures.rack(helper, rackBeside, Direction.SOUTH);
        InboundStationBlockEntity station = helper.getBlockEntity(PA_STATION, InboundStationBlockEntity.class);
        HeavyRackBlockEntity crate = helper.getBlockEntity(rackBeside, HeavyRackBlockEntity.class);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(PA_CONTROLLER, WarehouseControllerBlockEntity.class);
        ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, PA_GARAGE, Direction.SOUTH, 1);

        helper.runAfterDelay(10, () -> {
            Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 40), false);
            helper.runAfterDelay(70, () -> {
                helper.assertTrue(WarehouseFixtures.count(crate, Items.COBBLESTONE) == 40, "the shuttle should have delivered 40 cobblestone, rack has "
                        + WarehouseFixtures.count(crate, Items.COBBLESTONE));
                helper.assertTrue(controller.network().orElseThrow().summary().getString().contains("0 busy"),
                        "the shuttle should be home and idle: " + controller.network().orElseThrow().summary().getString());
                helper.succeed();
            });
        });
    }

    /** A garage takes shuttle items up to its capacity, refuses to be dismantled while it holds them, and breaks when empty. */
    @ServusGameTest(timeoutTicks = 100)
    public static void garageHoldsShuttles(GameTestHelper helper) {
        ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, GA_GARAGE, Direction.SOUTH, 0);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int capacity = garage.capacity();
        ShuttleGarageMenu menu = new ShuttleGarageMenu(1, player.getInventory(), garage);
        player.getInventory().setItem(0, new ItemStack(ServusStorageItems.SHUTTLE_MK1.get(), capacity + 1));
        for (int i = 0; i <= capacity; i++) {
            menu.quickMoveStack(player, menu.inventoryStart() + 27);
        }
        helper.assertTrue(garage.shuttleCount() == capacity, "one shift click per shuttle should fill every slot, filled " + garage.shuttleCount());
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 1, "one shuttle item should not fit");
        helper.assertTrue(!garage.shuttles().isItemValid(0, new ItemStack(Items.BREAD)), "only shuttles fit the slots");
        helper.assertTrue(garage.takeShuttle() && garage.takeShuttle(), "two shuttles can be taken back");
        helper.assertTrue(garage.shuttleCount() == capacity - 2, "count should drop by two");

        player.setShiftKeyDown(true);
        MachineTestSupport.useCarrot(helper, player, GA_GARAGE, Direction.UP);
        helper.assertTrue(helper.getBlockState(GA_GARAGE).getBlock() instanceof ShuttleGarageBlock, "an occupied garage must not be dismantled");
        while (garage.takeShuttle()) {
            // empty it
        }
        MachineTestSupport.useCarrot(helper, player, GA_GARAGE, Direction.UP);
        helper.runAfterDelay(2, () -> {
            helper.assertBlockPresent(Blocks.AIR, GA_GARAGE);
            helper.assertItemEntityPresent(ServusStorageItems.SHUTTLE_GARAGE.get(), GA_GARAGE, 2.0);
            helper.succeed();
        });
    }

    /** The rail behind a garage links to it as a plain arm; a rail in front neither links nor docks. */
    @ServusGameTest(timeoutTicks = 100)
    public static void garageRailIsAnArm(GameTestHelper helper) {
        helper.setBlock(AR_GARAGE.north().below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(AR_GARAGE.south().below(), Blocks.STONE.defaultBlockState());
        WarehouseFixtures.garage(helper, AR_GARAGE, Direction.NORTH, 0);
        WarehouseFixtures.rail(helper, AR_GARAGE.north());
        WarehouseFixtures.rail(helper, AR_GARAGE.south());
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

    /** With two garages the nearer shuttle serves and each shuttle returns to its own garage. */
    @ServusGameTest(timeoutTicks = 400)
    public static void shuttlesParkAtTheirGarage(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, TG_CONTROLLER);
        TerminalWiring.place(helper, TG_TERMINAL);
        WarehouseFixtures.railRow(helper, 2, 10, 4);
        WarehouseFixtures.garage(helper, TG_GARAGE_A, Direction.SOUTH, 0);
        WarehouseFixtures.garage(helper, TG_GARAGE_B, Direction.SOUTH, 0);
        WarehouseFixtures.rack(helper, TG_RACK, Direction.NORTH);
        ShuttleGarageBlockEntity garageA = helper.getBlockEntity(TG_GARAGE_A, ShuttleGarageBlockEntity.class);
        ShuttleGarageBlockEntity garageB = helper.getBlockEntity(TG_GARAGE_B, ShuttleGarageBlockEntity.class);
        helper.assertTrue(garageA.addShuttle() && garageB.addShuttle(), "each garage takes a shuttle");
        WarehouseFixtures.stock(helper, TG_RACK, new ItemStack(Items.COBBLESTONE, 30));
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(TG_CONTROLLER, WarehouseControllerBlockEntity.class);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.summary().getString().contains("2 garages, 2 shuttles"), "summary should count both garages: " + network.summary().getString());
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, TG_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64);
            helper.runAfterDelay(6, () -> {
                List<ShuttleView> a = network.shuttleViews(helper.absolutePos(TG_GARAGE_A));
                List<ShuttleView> b = network.shuttleViews(helper.absolutePos(TG_GARAGE_B));
                helper.assertTrue(a.isEmpty(), "garage A's shuttle should stay inside its garage and not be drawn");
                helper.assertTrue(b.size() == 1 && b.get(0).moving(), "garage B's shuttle, the nearer one, should be on its way");
            });
            helper.runAfterDelay(220, () -> {
                helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 30, "the cobblestone should have been delivered into the output slots");
                helper.assertTrue(network.shuttleViews(helper.absolutePos(TG_GARAGE_B)).isEmpty(), "garage B's shuttle should be back inside its garage");
                helper.assertTrue(network.summary().getString().contains("2 shuttles, 0 busy"), "both shuttles idle: " + network.summary().getString());
                helper.succeed();
            });
        });
    }

    /** Without a garage nothing reaches a rack: a terminal push waits and the rack is untouched. */
    @ServusGameTest(timeoutTicks = 100)
    public static void rackNeedsShuttle(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, RS_CONTROLLER);
        TerminalWiring.place(helper, RS_TERMINAL);
        WarehouseFixtures.rack(helper, RS_RACK, Direction.NORTH);
        // A rail from the rack's front round to the controller joins the rack; there is no garage.
        for (BlockPos rail : List.of(RS_RACK.north(), RS_RACK.north().east(), RS_RACK.east(), RS_CONTROLLER.east())) {
            WarehouseFixtures.railOnStone(helper, rail);
        }
        HeavyRackBlockEntity rack = helper.getBlockEntity(RS_RACK, HeavyRackBlockEntity.class);
        rack.items().insertItem(0, new ItemStack(Items.BREAD, 40), false);
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(RS_CONTROLLER, WarehouseControllerBlockEntity.class);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.index().count(ItemKey.of(new ItemStack(Items.BREAD))) == 40, "the rack's bread is indexed");

            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, RS_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 20));
            menu.quickMoveStack(player, 27);
            helper.assertTrue(player.getInventory().getItem(0).getCount() == 20, "a terminal push must not fill the rack");
            helper.assertTrue(rack.items().getStackInSlot(0).getCount() == 40 && rack.items().getStackInSlot(1).isEmpty(), "the rack is untouched");
            helper.succeed();
        });
    }

    /** A station stack whose only room is a rack waits with the needs a shuttle reason until a garage with a shuttle can reach the rack. */
    @ServusGameTest(timeoutTicks = 400)
    public static void stationWaitsForShuttleOnRacks(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, SW_CONTROLLER);
        for (int x = 1; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 0, 11), Blocks.STONE.defaultBlockState());
        }
        for (int x = 2; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 1, 11), ServusStorageBlocks.RAIL.get().defaultBlockState());
        }
        helper.setBlock(SW_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState().setValue(InboundStationBlock.FACING, Direction.NORTH));
        // The rack faces south, towards the rail behind it at z 11.
        WarehouseFixtures.rack(helper, SW_RACK, Direction.SOUTH);
        InboundStationBlockEntity station = helper.getBlockEntity(SW_STATION, InboundStationBlockEntity.class);
        HeavyRackBlockEntity rack = helper.getBlockEntity(SW_RACK, HeavyRackBlockEntity.class);
        helper.runAfterDelay(10, () -> {
            Transfer.insertItem(station.intake(), new ItemStack(Items.BREAD, 10), false);
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(!station.isEmpty(), "the bread should wait: only a rack has room");
                helper.assertTrue(station.blockedReason() == PutAwayPlan.Reason.NO_SHUTTLE, "reason should be no shuttle, was " + station.blockedReason());
                helper.assertTrue(WarehouseFixtures.blocked(helper, SW_STATION), "the lamp should be red");
                ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, SW_GARAGE, Direction.NORTH, 1);
                helper.runAfterDelay(120, () -> {
                    int inRack = 0;
                    for (int i = 0; i < rack.items().getSlots(); i++) {
                        inRack += rack.items().getStackInSlot(i).is(Items.BREAD) ? rack.items().getStackInSlot(i).getCount() : 0;
                    }
                    helper.assertTrue(inRack == 10, "the shuttle should have carried the bread into the rack, rack has " + inRack);
                    helper.assertTrue(station.isEmpty(), "the buffer should be empty");
                    helper.assertTrue(!WarehouseFixtures.blocked(helper, SW_STATION), "the lamp should be normal again");
                    helper.succeed();
                });
            });
        });
    }

    /** A rack request is refused without a garage and delivered into the output slots once a shuttle can reach the rack. */
    @ServusGameTest(timeoutTicks = 400)
    public static void rackPickByShuttle(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, RP_CONTROLLER);
        WarehouseFixtures.railRow(helper, 3, 8, 11);
        TerminalWiring.place(helper, RP_TERMINAL);
        HeavyRackBlockEntity rack = WarehouseFixtures.rack(helper, RP_RACK, Direction.SOUTH);
        rack.items().insertItem(0, new ItemStack(Items.BREAD, 30), false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, RP_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "online");
            menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 30);
            helper.assertTrue(menu.stock().message().equals("needs_shuttle"), "a rack request without a garage is refused, got " + menu.stock().message());
            helper.assertTrue(menu.stock().requests().isEmpty(), "no request is created");

            ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, RP_GARAGE, Direction.NORTH, 1);
            helper.runAfterDelay(5, () -> {
                menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 30);
                helper.assertTrue(menu.stock().message().isEmpty(), "the request should go through now, got " + menu.stock().message());
                helper.assertTrue(terminal.outputCount(Items.BREAD) == 0, "nothing arrives at once from a rack");
                helper.runAfterDelay(220, () -> {
                    helper.assertTrue(terminal.outputCount(Items.BREAD) == 30, "the shuttle should have brought 30 bread, output has " + terminal.outputCount(Items.BREAD));
                    helper.assertTrue(rack.isEmpty(), "the rack should be empty");
                    helper.assertTrue(menu.stock().requests().isEmpty(), "the request should be settled");
                    helper.succeed();
                });
            });
        });
    }

    /** Without a garage a request is refused as needing a shuttle and nothing leaves the rack. */
    @ServusGameTest(timeoutTicks = 100)
    public static void requestWithoutGarageNeedsShuttle(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, new BlockPos(3, 1, 4), false).withTerminal().stock(new ItemStack(Items.COBBLESTONE, 30));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), warehouse.terminal());
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 10);
            helper.assertTrue(menu.stock().message().equals("needs_shuttle"), "the request needs a shuttle, got " + menu.stock().message());
            helper.assertTrue(menu.stock().requests().isEmpty(), "no request is created");
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(warehouse.rackCount(Items.COBBLESTONE) == 30 && warehouse.terminal().outputCount(Items.COBBLESTONE) == 0,
                        "the rack keeps its cobblestone");
                helper.succeed();
            });
        });
    }

    /** Without a garage a put-away waits in the Inbound Station, which says it needs a shuttle. */
    @ServusGameTest(timeoutTicks = 100)
    public static void putAwayWithoutGarageWaits(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, new BlockPos(3, 1, 4), false);
        InboundStationBlockEntity station = warehouse.withInbound();
        helper.runAfterDelay(10, () -> {
            Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 20), false);
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(!station.isEmpty() && warehouse.rack().isEmpty(), "the cobblestone waits in the buffer");
                helper.assertTrue(station.blockedReason() == PutAwayPlan.Reason.NO_SHUTTLE, "reason should be no shuttle, was " + station.blockedReason());
                helper.assertTrue(WarehouseFixtures.blocked(helper, warehouse.inboundPos()), "the lamp should be red");
                helper.succeed();
            });
        });
    }

    /** Controller, terminal, a rail along z 4, a garage behind the rail with one shuttle, a rack with cobblestone at the far end. */
    private static WarehouseControllerBlockEntity recallSetup(GameTestHelper helper) {
        WarehouseFixtures.controller(helper, RC_CONTROLLER);
        TerminalWiring.place(helper, RC_TERMINAL);
        WarehouseFixtures.railRow(helper, 2, 9, 4);
        WarehouseFixtures.garage(helper, RC_GARAGE, Direction.SOUTH, 0);
        WarehouseFixtures.rack(helper, RC_RACK, Direction.NORTH);
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(RC_GARAGE, ShuttleGarageBlockEntity.class);
        helper.assertTrue(garage.addShuttle(), "the garage takes a shuttle");
        HeavyRackBlockEntity rack = helper.getBlockEntity(RC_RACK, HeavyRackBlockEntity.class);
        rack.items().insertItem(0, new ItemStack(Items.COBBLESTONE, 30), false);
        return helper.getBlockEntity(RC_CONTROLLER, WarehouseControllerBlockEntity.class);
    }

    /** Recall brings a shuttle that is out on a pick back inside at once; the pick is queued again. */
    @ServusGameTest(timeoutTicks = 400)
    public static void garageRecallBringsShuttlesHome(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = recallSetup(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, RC_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 30);
            helper.runAfterDelay(8, () -> {
                BlockPos garagePos = helper.absolutePos(RC_GARAGE);
                helper.assertTrue(network.garageOut(garagePos) == 1, "the shuttle should be out on the pick");
                network.recallGarage(garagePos);
                helper.assertTrue(network.garageOut(garagePos) == 0, "after the recall nothing is out");
                helper.assertTrue(network.shuttleViews(garagePos).isEmpty(), "the recalled shuttle sits inside and is not drawn");
                helper.runAfterDelay(280, () -> {
                    helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 30, "the queued pick is taken up again and delivered, got " + terminal.outputCount(Items.COBBLESTONE));
                    helper.succeed();
                });
            });
        });
    }

    /** A paused garage hands out no shuttle; the active one serves instead. */
    @ServusGameTest(timeoutTicks = 300)
    public static void pausedGarageTakesNoJobs(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = recallSetup(helper);
        ShuttleGarageBlockEntity garageB = WarehouseFixtures.garage(helper, PG_GARAGE_B, Direction.SOUTH, 1);
        garageB.setActive(false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.garageSummary(helper.absolutePos(PG_GARAGE_B)).getString().contains("paused"), "the summary says paused");
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, RC_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
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
    @ServusGameTest(timeoutTicks = 300)
    public static void losingTheRailRecalls(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = recallSetup(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, RC_TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 30);
            helper.runAfterDelay(8, () -> {
                BlockPos garagePos = helper.absolutePos(RC_GARAGE);
                helper.assertTrue(network.garageOut(garagePos) == 1, "the shuttle should be out on the pick");
                helper.setBlock(RC_GARAGE.north(), Blocks.AIR.defaultBlockState());
                helper.runAfterDelay(5, () -> {
                    // The rail line split off, so the controller's part is a rebuilt network object.
                    WarehouseNetwork after = controller.network().orElseThrow();
                    ShuttleGarageBlockEntity garage = helper.getBlockEntity(RC_GARAGE, ShuttleGarageBlockEntity.class);
                    helper.assertTrue(garage.shuttleCount() == 1, "the garage still holds its shuttle");
                    helper.assertTrue(after.garageOut(garagePos) == 0, "without a home the shuttle is recalled, out is " + after.garageOut(garagePos));
                    helper.assertTrue(after.shuttleViews(garagePos).isEmpty(), "nothing is drawn on the rails");
                    // Garages join only through their back rail, so without it the garage has left the warehouse.
                    helper.assertTrue(after.garageSummary(garagePos).getString().contains("not in a warehouse"),
                            "the summary says the garage left: " + after.garageSummary(garagePos).getString());
                    helper.succeed();
                });
            });
        });
    }
}
