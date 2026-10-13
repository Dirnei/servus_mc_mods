package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.packing.ServusStorageShipping;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlock;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Packing Stations between a warehouse and a data line. Rails run along z 6 from
 * x 2 to 9 with the controller at x 1, a garage
 * north of the rail at x 2, Packing Stations south of the rail at x 3 (far) and
 * x 7 (near), a rack at x 9 facing the rail, data ducts along z 8 from x 3 to
 * 7 and a Crafting Station south of the ducts at x 5.
 */
public final class PackingStationGameTests {

    private static final BlockPos CONTROLLER = new BlockPos(1, 1, 6);
    private static final BlockPos GARAGE = new BlockPos(2, 1, 5);
    private static final BlockPos FAR = new BlockPos(3, 1, 7);
    private static final BlockPos NEAR = new BlockPos(7, 1, 7);
    private static final BlockPos RACK = new BlockPos(9, 1, 7);
    private static final BlockPos STATION = new BlockPos(5, 1, 9);
    private static final BlockPos DUCT = new BlockPos(5, 1, 8);

    private PackingStationGameTests() {
    }

    private record Setup(CraftingStationBlockEntity station, HeavyRackBlockEntity rack) {
    }

    private static void data(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos, UtilityDuctBlockEntity.class);
        duct.addLine(Line.DATA);
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
    }

    private static void rail(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos.below(), Blocks.STONE.defaultBlockState());
        BlockState state = Block.updateFromNeighbourShapes(ServusStorageBlocks.RAIL.get().defaultBlockState(),
                helper.getLevel(), helper.absolutePos(pos));
        helper.setBlock(pos, state);
    }

    private static void packingStation(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.PACKING_STATION.get().defaultBlockState());
    }

    private static Setup setup(GameTestHelper helper, int rackPlanks, boolean far, boolean near) {
        WarehouseFixtures.controller(helper, CONTROLLER);
        for (int x = 2; x <= 9; x++) {
            rail(helper, new BlockPos(x, 1, 6));
        }
        ShuttleGarageBlockEntity garage = WarehouseFixtures.garage(helper, GARAGE, Direction.NORTH, 1);
        HeavyRackBlockEntity rack = WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        if (rackPlanks > 0) {
            WarehouseFixtures.stock(helper, RACK, new ItemStack(Items.OAK_PLANKS, rackPlanks));
        }
        if (far) {
            packingStation(helper, FAR);
        }
        if (near) {
            packingStation(helper, NEAR);
        }
        helper.setBlock(STATION, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState()
                .setValue(CraftingStationBlock.FACING, Direction.SOUTH));
        CraftingStationBlockEntity station = helper.getBlockEntity(STATION, CraftingStationBlockEntity.class);
        StationJobs.sticksJob(station, 64);
        for (int slot = 0; slot < station.outputs().getSlots(); slot++) {
            station.outputs().setStackInSlot(slot, new ItemStack(Items.STICK, 64));
        }
        for (int x = 3; x <= 7; x++) {
            data(helper, new BlockPos(x, 1, 8));
        }
        return new Setup(station, rack);
    }

    private static int count(ItemSlots handler, Item item) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.getStackInSlot(slot).is(item)) {
                total += handler.getStackInSlot(slot).getCount();
            }
        }
        return total;
    }

    private static WarehouseNetwork.Shipping ship(GameTestHelper helper, int amount) {
        return ServusStorageShipping.ship(helper.getLevel(), helper.absolutePos(CONTROLLER), helper.absolutePos(STATION),
                new ItemStack(Items.OAK_PLANKS), amount);
    }

    /** Both Packing Stations are link members of the data line. */
    @ServusGameTest(timeoutTicks = 60)
    public static void packingStationIsLinkMember(GameTestHelper helper) {
        setup(helper, 0, true, true);
        helper.runAfterDelay(5, () -> {
            DataNetwork network = DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(DUCT)).orElseThrow();
            List<BlockPos> links = network.dataMembers(DataRole.LINK).stream().map(DataMember::pos).toList();
            helper.assertTrue(links.contains(helper.absolutePos(FAR)) && links.contains(helper.absolutePos(NEAR)),
                    "both Packing Stations should be link members, got " + network.dataMembers());
            helper.assertTrue(network.dataMembers(DataRole.PRODUCER).size() == 1, "the station is the producer");
            helper.succeed();
        });
    }

    /** A Packing Station belongs to the warehouse of the rail beside it and holds no stock. */
    @ServusGameTest(timeoutTicks = 60)
    public static void packingStationJoinsWarehouse(GameTestHelper helper) {
        setup(helper, 8, true, false);
        helper.runAfterDelay(10, () -> {
            PackingStationBlockEntity point = helper.getBlockEntity(FAR, PackingStationBlockEntity.class);
            point.outgoing().setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
            WarehouseNetwork network = point.network().orElseThrow(() -> new AssertionError("the Packing Station should be in a warehouse"));
            helper.assertTrue(network.status() == WarehouseNetwork.Status.ONLINE, "the warehouse should be online");
            helper.assertTrue(network.index().count(io.github.dirnei.mc.servus.storage.network.ItemKey.of(new ItemStack(Items.DIAMOND))) == 0,
                    "items in the Packing Station are not stock");
            helper.succeed();
        });
    }

    /** Planks sent as packets into a Packing Station end up in the rack by shuttle. */
    @ServusGameTest(timeoutTicks = 300)
    public static void packetsArePutAway(GameTestHelper helper) {
        Setup setup = setup(helper, 32, true, false);
        helper.runAfterDelay(10, () -> {
            DataPackets.send(helper.getLevel(), helper.absolutePos(STATION), helper.absolutePos(FAR), new ItemStack(Items.OAK_PLANKS, 16))
                    .orElseThrow(() -> new AssertionError("the send should start"));
            PackingStationBlockEntity point = helper.getBlockEntity(FAR, PackingStationBlockEntity.class);
            helper.succeedWhen(() -> {
                helper.assertTrue(count(setup.rack().items(), Items.OAK_PLANKS) == 48, "48 planks should be in the rack, got "
                        + count(setup.rack().items(), Items.OAK_PLANKS));
                helper.assertTrue(point.isEmpty(), "the intake should be empty again");
            });
        });
    }

    /** Bread put into a Storage Terminal's input travels to the Packing Station and a shuttle puts it into the rack. */
    @ServusGameTest(timeoutTicks = 600)
    public static void inputReachesRack(GameTestHelper helper) {
        Setup setup = setup(helper, 0, false, true);
        BlockPos terminalPos = new BlockPos(4, 1, 9);
        helper.setBlock(terminalPos, ServusStorageBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = helper.getBlockEntity(terminalPos, StorageTerminalBlockEntity.class);
            terminal.input().setStackInSlot(0, new ItemStack(Items.BREAD, 10));
            helper.succeedWhen(() -> {
                helper.assertTrue(terminal.input().getStackInSlot(0).isEmpty(), "the input should be empty");
                helper.assertTrue(count(setup.rack().items(), Items.BREAD) == 10,
                        "the rack should hold 10 bread, got " + count(setup.rack().items(), Items.BREAD));
            });
        });
    }

    /** Planks in a rack ride a shuttle to the Packing Station and travel as packets to the station; the rest stays in the rack. */
    @ServusGameTest(timeoutTicks = 600)
    public static void shipFromRack(GameTestHelper helper) {
        Setup setup = setup(helper, 32, true, false);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork.Shipping shipping = ship(helper, 16);
            helper.assertTrue(shipping.shipped(), "shipping should start, refused " + shipping.refusal());
            PackingStationBlockEntity.Shipment shipment = shipping.shipment().orElseThrow();
            helper.assertTrue(shipment.planned() == 16, "16 planned, got " + shipment.planned());
            helper.succeedWhen(() -> {
                helper.assertTrue(count(setup.station().inputs(), Items.OAK_PLANKS) == 16, "16 planks should reach the station");
                helper.assertTrue(shipment.delivered() == 16 && shipment.ended(), "the shipment should end with 16 delivered");
                helper.assertTrue(count(setup.rack().items(), Items.OAK_PLANKS) == 16, "16 planks stay in the rack");
            });
        });
    }

    /** Planks only in a rack ride a shuttle to the Packing Station first. */
    @ServusGameTest(timeoutTicks = 600)
    public static void shipFromRackByShuttle(GameTestHelper helper) {
        Setup setup = setup(helper, 16, false, true);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork.Shipping shipping = ship(helper, 16);
            helper.assertTrue(shipping.shipped(), "shipping should start, refused " + shipping.refusal());
            helper.assertTrue(count(setup.station().inputs(), Items.OAK_PLANKS) == 0, "rack planks must not arrive at once");
            helper.succeedWhen(() -> {
                helper.assertTrue(count(setup.station().inputs(), Items.OAK_PLANKS) == 16, "16 planks should reach the station");
                helper.assertTrue(setup.rack().items().getStackInSlot(0).isEmpty(), "the rack should be empty");
            });
        });
    }

    /** With two Packing Stations the one nearer to the rack serves the shipment. */
    @ServusGameTest(timeoutTicks = 600)
    public static void nearerPackingStationWins(GameTestHelper helper) {
        Setup setup = setup(helper, 16, true, true);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork.Shipping shipping = ship(helper, 16);
            helper.assertTrue(shipping.shipped(), "shipping should start, refused " + shipping.refusal());
            helper.assertTrue(helper.absolutePos(NEAR).equals(shipping.packingStation()),
                    "the near Packing Station should be chosen, got " + shipping.packingStation());
            helper.succeedWhen(() -> helper.assertTrue(count(setup.station().inputs(), Items.OAK_PLANKS) == 16,
                    "16 planks should reach the station"));
        });
    }

    /** Without a Packing Station on the data line nothing leaves the warehouse. */
    @ServusGameTest(timeoutTicks = 60)
    public static void shipRefusedWithoutPackingStation(GameTestHelper helper) {
        Setup setup = setup(helper, 32, false, false);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork.Shipping shipping = ship(helper, 16);
            helper.assertTrue(shipping.refusal().equals("no_packing_station"), "refused with no_packing_station, got " + shipping.refusal());
            helper.assertTrue(count(setup.rack().items(), Items.OAK_PLANKS) == 32, "the planks stay in the rack");
            helper.succeed();
        });
    }

    /** A broken Packing Station drops both buffers (the block itself drops by its loot table when mined with a pickaxe). */
    @ServusGameTest(timeoutTicks = 60)
    public static void brokenPackingStationDropsBuffers(GameTestHelper helper) {
        packingStation(helper, FAR);
        PackingStationBlockEntity point = helper.getBlockEntity(FAR, PackingStationBlockEntity.class);
        point.outgoing().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 5));
        point.buffer().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 3));
        helper.destroyBlock(FAR);
        helper.runAfterDelay(2, () -> {
            AABB area = new AABB(helper.absolutePos(FAR)).inflate(3);
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area);
            int planks = drops.stream().filter(e -> e.getItem().is(Items.OAK_PLANKS)).mapToInt(e -> e.getItem().getCount()).sum();
            int stone = drops.stream().filter(e -> e.getItem().is(Items.COBBLESTONE)).mapToInt(e -> e.getItem().getCount()).sum();
            helper.assertTrue(planks == 5 && stone == 3, "the planks and the cobblestone of both buffers should drop, got " + drops);
            helper.succeed();
        });
    }

    /** Most items a station at 8 per second can convert in {@code ticks}: one second of saved up budget plus the rate. */
    private static int atMost(int ticks) {
        return 8 + ticks * 8 / 20 + 1;
    }

    /** Fewest items expected in {@code ticks}: the rate minus one push interval of start up. */
    private static int atLeast(int ticks) {
        return (ticks - 10) * 8 / 20;
    }

    /** Loads a station's To network buffer with planks and opens a shipment of them to the Crafting Station. */
    private static PackingStationBlockEntity.Shipment preload(GameTestHelper helper, BlockPos pos, int planks) {
        PackingStationBlockEntity point = helper.getBlockEntity(pos, PackingStationBlockEntity.class);
        point.outgoing().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, planks));
        return point.addShipment(helper.absolutePos(STATION), io.github.dirnei.mc.servus.storage.network.ItemKey.of(
                new ItemStack(Items.OAK_PLANKS)), planks);
    }

    /** How many of the preloaded planks have left a station's To network buffer. */
    private static int sent(GameTestHelper helper, BlockPos pos, int preloaded) {
        PackingStationBlockEntity point = helper.getBlockEntity(pos, PackingStationBlockEntity.class);
        return preloaded - count(point.outgoing(), Items.OAK_PLANKS);
    }

    /** A shipment of 64 planks leaves the Packing Station at about 8 per second. */
    @ServusGameTest(timeoutTicks = 400)
    public static void drainsAtEightPerSecond(GameTestHelper helper) {
        Setup setup = setup(helper, 0, true, false);
        helper.runAfterDelay(10, () -> {
            long start = helper.getTick();
            preload(helper, FAR, 64);
            PackingStationBlockEntity point = helper.getBlockEntity(FAR, PackingStationBlockEntity.class);
            helper.assertTrue(count(point.outgoing(), Items.OAK_PLANKS) == 64, "the planks wait in the To network buffer");
            helper.runAfterDelay(80, () -> {
                int arrived = count(setup.station().inputs(), Items.OAK_PLANKS);
                helper.assertTrue(arrived >= atLeast(80) && arrived <= atMost(80),
                        "about 32 planks should have left after 4 seconds, got " + arrived);
                helper.assertTrue(count(point.outgoing(), Items.OAK_PLANKS) == 64 - arrived, "the rest waits in the To network buffer");
            });
            helper.succeedWhen(() -> {
                helper.assertTrue(count(setup.station().inputs(), Items.OAK_PLANKS) == 64, "all 64 planks should arrive");
                long took = helper.getTick() - start;
                helper.assertTrue(took >= 130, "64 planks at 8 per second take about 8 seconds, took " + took + " ticks");
            });
        });
    }

    /** Two Packing Stations each add their own rate. */
    @ServusGameTest(timeoutTicks = 200)
    public static void twoStationsDoubleThroughput(GameTestHelper helper) {
        setup(helper, 0, true, true);
        helper.runAfterDelay(10, () -> {
            preload(helper, FAR, 64);
            preload(helper, NEAR, 64);
            helper.runAfterDelay(80, () -> {
                int fromFar = sent(helper, FAR, 64);
                int fromNear = sent(helper, NEAR, 64);
                helper.assertTrue(fromFar >= atLeast(80) && fromNear >= atLeast(80),
                        "both stations should send about 32 planks in 4 seconds, got " + fromFar + " and " + fromNear);
                helper.assertTrue(fromFar + fromNear > atMost(80),
                        "together they should send more than one station can, got " + (fromFar + fromNear));
                helper.succeed();
            });
        });
    }

    /** Items into storage and items out of storage each move at the full rate. */
    @ServusGameTest(timeoutTicks = 200)
    public static void bothDirectionsAtOnce(GameTestHelper helper) {
        setup(helper, 0, true, false);
        helper.runAfterDelay(10, () -> {
            preload(helper, FAR, 64);
            io.github.dirnei.mc.servus.foundation.data.packet.Delivery in = DataPackets.send(helper.getLevel(),
                    helper.absolutePos(STATION), helper.absolutePos(FAR), new ItemStack(Items.COBBLESTONE, 64))
                    .orElseThrow(() -> new AssertionError("the send should start"));
            helper.runAfterDelay(80, () -> {
                int sent = sent(helper, FAR, 64);
                helper.assertTrue(sent >= atLeast(80) && sent <= atMost(80),
                        "about 32 planks should go out in 4 seconds, got " + sent);
                helper.assertTrue(in.delivered() >= atLeast(80) && in.delivered() <= atMost(80),
                        "about 32 cobblestone should come in in 4 seconds, got " + in.delivered());
                helper.succeed();
            });
        });
    }

    /** Neither row of the Packing Station GUI gives items out or takes items in. */
    @ServusGameTest(timeoutTicks = 60)
    public static void guiRefusesExtraction(GameTestHelper helper) {
        packingStation(helper, FAR);
        PackingStationBlockEntity point = helper.getBlockEntity(FAR, PackingStationBlockEntity.class);
        point.outgoing().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 16));
        point.buffer().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 5));
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        io.github.dirnei.mc.servus.storage.menu.PackingStationMenu menu =
                new io.github.dirnei.mc.servus.storage.menu.PackingStationMenu(1, player.getInventory(), point);
        int toStorage = 0;
        int toNetwork = menu.toStorageSlots();
        for (net.minecraft.world.inventory.ContainerInput click : List.of(net.minecraft.world.inventory.ContainerInput.PICKUP,
                net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, net.minecraft.world.inventory.ContainerInput.THROW,
                net.minecraft.world.inventory.ContainerInput.SWAP)) {
            menu.clicked(toNetwork, 0, click, player);
            menu.clicked(toStorage, 0, click, player);
        }
        helper.assertTrue(point.outgoing().getStackInSlot(0).getCount() == 16, "the 16 planks stay in the To network row");
        helper.assertTrue(point.buffer().getStackInSlot(0).getCount() == 5, "the 5 cobblestone stay in the To storage row");
        helper.assertTrue(menu.getCarried().isEmpty(), "nothing lands on the cursor");
        helper.assertTrue(player.getInventory().countItem(Items.OAK_PLANKS) == 0 && player.getInventory().countItem(Items.COBBLESTONE) == 0,
                "nothing lands in the player inventory");
        menu.setCarried(new ItemStack(Items.DIAMOND, 3));
        menu.clicked(toNetwork + 1, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        helper.assertTrue(point.outgoing().getStackInSlot(1).isEmpty() && menu.getCarried().getCount() == 3,
                "nothing can be put into the To network row");
        menu.setCarried(ItemStack.EMPTY);
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
        menu.clicked(menu.inventoryStart() + 27, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, player);
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 3, "shift click from the inventory moves nothing");
        helper.succeed();
    }

    /** One Speed Upgrade doubles the station's rate: about 16 planks per second leave, and the GUI shows 16/s. */
    @ServusGameTest(timeoutTicks = 200)
    public static void speedUpgradeDoubles(GameTestHelper helper) {
        setup(helper, 0, true, false);
        PackingStationBlockEntity point = helper.getBlockEntity(FAR, PackingStationBlockEntity.class);
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        io.github.dirnei.mc.servus.storage.menu.PackingStationMenu menu =
                new io.github.dirnei.mc.servus.storage.menu.PackingStationMenu(1, player.getInventory(), point);
        player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 4));
        player.getInventory().setItem(1, new ItemStack(io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems.SPEED_UPGRADE.get(), 1));
        int hotbar = menu.inventoryStart() + 27;
        menu.quickMoveStack(player, hotbar);
        menu.quickMoveStack(player, hotbar + 1);
        helper.assertTrue(point.upgrades().getStackInSlot(0).is(io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems.SPEED_UPGRADE.get()),
                "shift click puts the Speed Upgrade into the upgrade row");
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 4, "cobblestone stays in the inventory");
        helper.assertTrue(point.rate() == 16, "the rate is 16 per second, got " + point.rate());
        helper.assertTrue(point.status().rate() == 16, "the GUI is told 16/s, got " + point.status().rate());
        helper.runAfterDelay(10, () -> {
            preload(helper, FAR, 128);
            helper.runAfterDelay(80, () -> {
                int sent = sent(helper, FAR, 128);
                helper.assertTrue(sent >= 56 && sent <= 16 + 64 + 1, "about 64 planks should go out in 4 seconds, got " + sent);
                helper.succeed();
            });
        });
    }

    /** Upgrades drop when the station is broken. */
    @ServusGameTest(timeoutTicks = 60)
    public static void upgradesDropWithStation(GameTestHelper helper) {
        packingStation(helper, FAR);
        PackingStationBlockEntity point = helper.getBlockEntity(FAR, PackingStationBlockEntity.class);
        point.upgrades().setStackInSlot(1, new ItemStack(io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems.SPEED_UPGRADE.get()));
        helper.destroyBlock(FAR);
        helper.runAfterDelay(2, () -> {
            AABB area = new AABB(helper.absolutePos(FAR)).inflate(3);
            int upgrades = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).stream()
                    .filter(e -> e.getItem().is(io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems.SPEED_UPGRADE.get()))
                    .mapToInt(e -> e.getItem().getCount()).sum();
            helper.assertTrue(upgrades == 1, "the Speed Upgrade drops, got " + upgrades);
            helper.succeed();
        });
    }

    /** A station sending at its full rate shows 8 of 8 to network marked busy and 0 of 8 to storage. */
    @ServusGameTest(timeoutTicks = 200)
    public static void stationRowShowsUse(GameTestHelper helper) {
        setup(helper, 0, true, false);
        helper.runAfterDelay(10, () -> {
            preload(helper, FAR, 128);
            helper.runAfterDelay(60, () -> {
                io.github.dirnei.mc.servus.foundation.data.DataNetwork line = io.github.dirnei.mc.servus.foundation.data.DataNetworks
                        .networkAt(helper.getLevel(), helper.absolutePos(DUCT)).orElseThrow(() -> new AssertionError("a data line"));
                List<io.github.dirnei.mc.servus.storage.production.ProductionRows.PackingRow> rows =
                        io.github.dirnei.mc.servus.storage.production.ProductionView.packing(helper.getLevel(), line);
                helper.assertTrue(rows.size() == 1 && rows.get(0).pos().equals(helper.absolutePos(FAR)), "one Packing Station row, got " + rows);
                var row = rows.get(0);
                helper.assertTrue(row.rate() == 8, "rate 8, got " + row.rate());
                helper.assertTrue(row.toNetwork() >= 7 && row.toNetwork() <= 9 && row.busyToNetwork(),
                        "about 8 of 8 to network and busy, got " + row);
                helper.assertTrue(row.toStorage() == 0 && !row.busyToStorage(), "0 of 8 to storage, got " + row);
                helper.succeed();
            });
        });
    }

    /** A full stack of logs in the Input row reaches the rack by shuttle, all 64 of them. */
    @ServusGameTest(timeoutTicks = 1200)
    public static void inputStackReachesRack(GameTestHelper helper) {
        inputStack(helper, false);
    }

    /** The same with a second Packing Station without a data line on the rails: the input still arrives, all 64. */
    @ServusGameTest(timeoutTicks = 1200)
    public static void inputStackReachesRackBesidePackingStation(GameTestHelper helper) {
        inputStack(helper, true);
    }

    private static void inputStack(GameTestHelper helper, boolean second) {
        Setup setup = setup(helper, 0, false, true);
        if (second) {
            helper.setBlock(new BlockPos(5, 1, 7), ServusStorageBlocks.PACKING_STATION.get().defaultBlockState());
        }
        BlockPos terminalPos = new BlockPos(4, 1, 9);
        helper.setBlock(terminalPos, ServusStorageBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = helper.getBlockEntity(terminalPos, StorageTerminalBlockEntity.class);
            terminal.input().setStackInSlot(0, new ItemStack(Items.OAK_LOG, 64));
            helper.succeedWhen(() -> {
                helper.assertTrue(count(setup.rack().items(), Items.OAK_LOG) == 64, "the rack should hold 64 logs, got "
                        + count(setup.rack().items(), Items.OAK_LOG) + ", input " + terminal.input().getStackInSlot(0).getCount()
                        + ", near intake " + helper.getBlockEntity(NEAR, PackingStationBlockEntity.class).isEmpty());
                helper.assertTrue(terminal.input().getStackInSlot(0).isEmpty(), "the input should be empty");
            });
        });
    }

    /**
     * A terminal touching the controller and a second Packing Station
     * beside it are no foreign inventories: neither the terminal's
     * rows nor the station's buffers count as stock, and the Input row's logs
     * all reach the rack.
     */
    @ServusGameTest(timeoutTicks = 1200)
    public static void terminalBesideControllerIsNoStock(GameTestHelper helper) {
        Setup setup = setup(helper, 0, false, true);
        BlockPos terminalPos = new BlockPos(1, 1, 7);
        helper.setBlock(terminalPos, ServusStorageBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        for (int x = 1; x <= 2; x++) {
            data(helper, new BlockPos(x, 1, 8));
        }
        BlockPos wirelessPos = new BlockPos(0, 1, 5);
        helper.setBlock(wirelessPos, ServusStorageBlocks.PACKING_STATION.get().defaultBlockState());
        var wireless = helper.getBlockEntity(wirelessPos, PackingStationBlockEntity.class);
        wireless.outgoing().setStackInSlot(0, new ItemStack(Items.EMERALD, 7));
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = helper.getBlockEntity(terminalPos, StorageTerminalBlockEntity.class);
            terminal.output().setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));
            terminal.input().setStackInSlot(0, new ItemStack(Items.OAK_LOG, 64));
            WarehouseNetwork network = helper.getBlockEntity(NEAR, PackingStationBlockEntity.class).network().orElseThrow();
            io.github.dirnei.mc.servus.storage.network.ItemKey logs = io.github.dirnei.mc.servus.storage.network.ItemKey.of(new ItemStack(Items.OAK_LOG));
            helper.succeedWhen(() -> {
                helper.assertTrue(network.index().count(io.github.dirnei.mc.servus.storage.network.ItemKey.of(new ItemStack(Items.EMERALD))) == 0,
                        "the second Packing Station's buffer is no stock");
                helper.assertTrue(count(setup.rack().items(), Items.OAK_LOG) == 64, "the rack should hold 64 logs, got "
                        + count(setup.rack().items(), Items.OAK_LOG) + ", input " + terminal.input().getStackInSlot(0).getCount());
                helper.assertTrue(terminal.input().getStackInSlot(0).isEmpty(), "the input should be empty");
                helper.assertTrue(network.index().count(io.github.dirnei.mc.servus.storage.network.ItemKey.of(new ItemStack(Items.DIAMOND))) == 0,
                        "the Output row is no stock");
                helper.assertTrue(network.index().count(logs) == 64, "stock counts the 64 logs once, got " + network.index().count(logs));
            });
        });
    }
}
