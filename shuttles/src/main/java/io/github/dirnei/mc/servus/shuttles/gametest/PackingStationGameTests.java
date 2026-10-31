package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.data.packet.Delivery;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.lock.LockedSlots;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLock;
import io.github.dirnei.mc.servus.shuttles.menu.PackingStationMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.packing.ServusShuttlesShipping;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows;
import io.github.dirnei.mc.servus.shuttles.production.ProductionView;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.shuttle.PickJob;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleView;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlock;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
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

    private static void packingStation(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusShuttlesBlocks.PACKING_STATION.get().defaultBlockState());
    }

    private static Setup setup(GameTestHelper helper, int rackPlanks, boolean far, boolean near) {
        WarehouseFixtures.controller(helper, CONTROLLER);
        for (int x = 2; x <= 9; x++) {
            WarehouseFixtures.railOnStone(helper, new BlockPos(x, 1, 6));
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
        helper.setBlock(STATION, ServusShuttlesBlocks.CRAFTING_STATION.get().defaultBlockState()
                .setValue(CraftingStationBlock.FACING, Direction.SOUTH));
        CraftingStationBlockEntity station = helper.getBlockEntity(STATION, CraftingStationBlockEntity.class);
        StationJobs.sticksJob(station, 64);
        for (int slot = 0; slot < station.outputs().getSlots(); slot++) {
            station.outputs().setStackInSlot(slot, new ItemStack(Items.STICK, 64));
        }
        for (int x = 3; x <= 7; x++) {
            TerminalWiring.dataDuct(helper, new BlockPos(x, 1, 8));
        }
        return new Setup(station, rack);
    }

    private static WarehouseNetwork.Shipping ship(GameTestHelper helper, int amount) {
        return ServusShuttlesShipping.ship(helper.getLevel(), helper.absolutePos(CONTROLLER), helper.absolutePos(STATION),
                new ItemStack(Items.OAK_PLANKS), amount);
    }

    /** Both Packing Stations are link members of the data line. */
    @ServusGameTest(timeoutTicks = 60)
    public static void packingStationIsLinkMember(GameTestHelper helper) {
        setup(helper, 0, true, true);
        helper.runAfterDelay(5, () -> {
            DataNetwork network = DataLines.at(helper, DUCT);
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
            helper.assertTrue(network.index().count(ItemKey.of(new ItemStack(Items.DIAMOND))) == 0,
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
                helper.assertTrue(WarehouseFixtures.count(setup.rack().items(), Items.OAK_PLANKS) == 48, "48 planks should be in the rack, got "
                        + WarehouseFixtures.count(setup.rack().items(), Items.OAK_PLANKS));
                helper.assertTrue(point.isEmpty(), "the intake should be empty again");
            });
        });
    }

    /** Bread put into a Storage Terminal's input travels to the Packing Station and a shuttle puts it into the rack. */
    @ServusGameTest(timeoutTicks = 600)
    public static void inputReachesRack(GameTestHelper helper) {
        Setup setup = setup(helper, 0, false, true);
        BlockPos terminalPos = new BlockPos(4, 1, 9);
        helper.setBlock(terminalPos, ServusShuttlesBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = helper.getBlockEntity(terminalPos, StorageTerminalBlockEntity.class);
            terminal.input().setStackInSlot(0, new ItemStack(Items.BREAD, 10));
            helper.succeedWhen(() -> {
                helper.assertTrue(terminal.input().getStackInSlot(0).isEmpty(), "the input should be empty");
                helper.assertTrue(WarehouseFixtures.count(setup.rack().items(), Items.BREAD) == 10,
                        "the rack should hold 10 bread, got " + WarehouseFixtures.count(setup.rack().items(), Items.BREAD));
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
                helper.assertTrue(WarehouseFixtures.count(setup.station().inputs(), Items.OAK_PLANKS) == 16, "16 planks should reach the station");
                helper.assertTrue(shipment.delivered() == 16 && shipment.ended(), "the shipment should end with 16 delivered");
                helper.assertTrue(WarehouseFixtures.count(setup.rack().items(), Items.OAK_PLANKS) == 16, "16 planks stay in the rack");
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
            helper.assertTrue(WarehouseFixtures.count(setup.station().inputs(), Items.OAK_PLANKS) == 0, "rack planks must not arrive at once");
            helper.succeedWhen(() -> {
                helper.assertTrue(WarehouseFixtures.count(setup.station().inputs(), Items.OAK_PLANKS) == 16, "16 planks should reach the station");
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
            helper.succeedWhen(() -> helper.assertTrue(WarehouseFixtures.count(setup.station().inputs(), Items.OAK_PLANKS) == 16,
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
            helper.assertTrue(WarehouseFixtures.count(setup.rack().items(), Items.OAK_PLANKS) == 32, "the planks stay in the rack");
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
        return rate() + ticks * rate() / 20 + 1;
    }

    /** The base rate from data, in items per second. */
    private static int rate() {
        return (int) ShuttlesStatsManager.INSTANCE.packingItemsPerSecond();
    }

    /** Fewest items expected in {@code ticks}: the rate minus one push interval of start up. */
    private static int atLeast(int ticks) {
        return (ticks - 10) * rate() / 20;
    }

    /** Loads a station's To network buffer with planks and opens a shipment of them to the Crafting Station. */
    private static PackingStationBlockEntity.Shipment preload(GameTestHelper helper, BlockPos pos, int planks) {
        PackingStationBlockEntity point = helper.getBlockEntity(pos, PackingStationBlockEntity.class);
        point.outgoing().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, planks));
        return point.addShipment(helper.absolutePos(STATION), ItemKey.of(
                new ItemStack(Items.OAK_PLANKS)), planks);
    }

    /** How many of the preloaded planks have left a station's To network buffer. */
    private static int sent(GameTestHelper helper, BlockPos pos, int preloaded) {
        PackingStationBlockEntity point = helper.getBlockEntity(pos, PackingStationBlockEntity.class);
        return preloaded - WarehouseFixtures.count(point.outgoing(), Items.OAK_PLANKS);
    }

    /** A shipment of 64 planks leaves the Packing Station at about its rate from data. */
    @ServusGameTest(timeoutTicks = 400)
    public static void drainsAtTheRate(GameTestHelper helper) {
        Setup setup = setup(helper, 0, true, false);
        helper.runAfterDelay(10, () -> {
            long start = helper.getTick();
            preload(helper, FAR, 64);
            PackingStationBlockEntity point = helper.getBlockEntity(FAR, PackingStationBlockEntity.class);
            helper.assertTrue(WarehouseFixtures.count(point.outgoing(), Items.OAK_PLANKS) == 64, "the planks wait in the To network buffer");
            helper.runAfterDelay(80, () -> {
                int arrived = WarehouseFixtures.count(setup.station().inputs(), Items.OAK_PLANKS);
                helper.assertTrue(arrived >= atLeast(80) && arrived <= atMost(80),
                        "about " + 4 * rate() + " planks should have left after 4 seconds, got " + arrived);
                helper.assertTrue(WarehouseFixtures.count(point.outgoing(), Items.OAK_PLANKS) == 64 - arrived, "the rest waits in the To network buffer");
            });
            helper.succeedWhen(() -> {
                helper.assertTrue(WarehouseFixtures.count(setup.station().inputs(), Items.OAK_PLANKS) == 64, "all 64 planks should arrive");
                long took = helper.getTick() - start;
                int expected = (64 - rate()) * 20 / rate();
                helper.assertTrue(took >= expected, "64 planks at " + rate() + " per second take at least " + expected + " ticks, took " + took);
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
                        "both stations should send about " + 4 * rate() + " planks in 4 seconds, got " + fromFar + " and " + fromNear);
                helper.assertTrue(fromFar + fromNear > atMost(80),
                        "together they should send more than one station can, got " + (fromFar + fromNear));
                helper.succeed();
            });
        });
    }

    /**
     * Items into storage and items out of storage each move at the full rate.
     * A first cobblestone calls the shuttle out of its garage; the transfers
     * start once it is through the doorway, so it reaches the station before
     * the To storage buffer fills up.
     */
    @ServusGameTest(timeoutTicks = 300)
    public static void bothDirectionsAtOnce(GameTestHelper helper) {
        setup(helper, 0, true, false);
        helper.runAfterDelay(10, () -> Transfer.insertItem(helper.getBlockEntity(FAR, PackingStationBlockEntity.class).intake(),
                new ItemStack(Items.COBBLESTONE), false));
        Waits.when(helper, () -> throughDoorway(helper), 100, () -> "the shuttle comes for the first cobblestone", () -> {
            preload(helper, FAR, 64);
            Delivery in = DataPackets.send(helper.getLevel(),
                    helper.absolutePos(STATION), helper.absolutePos(FAR), new ItemStack(Items.COBBLESTONE, 64))
                    .orElseThrow(() -> new AssertionError("the send should start"));
            helper.runAfterDelay(80, () -> {
                int sent = sent(helper, FAR, 64);
                helper.assertTrue(sent >= atLeast(80) && sent <= atMost(80),
                        "about " + 4 * rate() + " planks should go out in 4 seconds, got " + sent);
                helper.assertTrue(in.delivered() >= atLeast(80) && in.delivered() <= atMost(80),
                        "about " + 4 * rate() + " cobblestone should come in in 4 seconds, got " + in.delivered());
                helper.succeed();
            });
        });
    }

    /** Whether the garage's shuttle is out of its garage, on its way to the station. */
    private static boolean throughDoorway(GameTestHelper helper) {
        WarehouseNetwork network = helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class).network().orElse(null);
        if (network == null) {
            return false;
        }
        List<ShuttleView> views = network.shuttleViews(helper.absolutePos(GARAGE));
        return !views.isEmpty() && views.get(0).segment() > 0;
    }

    /** Neither row of the Packing Station GUI gives items out or takes items in. */
    @ServusGameTest(timeoutTicks = 60)
    public static void guiRefusesExtraction(GameTestHelper helper) {
        packingStation(helper, FAR);
        PackingStationBlockEntity point = helper.getBlockEntity(FAR, PackingStationBlockEntity.class);
        point.outgoing().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 16));
        point.buffer().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 5));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        PackingStationMenu menu =
                new PackingStationMenu(1, player.getInventory(), point);
        int toStorage = 0;
        int toNetwork = menu.toStorageSlots();
        for (ContainerInput click : List.of(ContainerInput.PICKUP,
                ContainerInput.QUICK_MOVE, ContainerInput.THROW,
                ContainerInput.SWAP)) {
            menu.clicked(toNetwork, 0, click, player);
            menu.clicked(toStorage, 0, click, player);
        }
        helper.assertTrue(point.outgoing().getStackInSlot(0).getCount() == 16, "the 16 planks stay in the To network row");
        helper.assertTrue(point.buffer().getStackInSlot(0).getCount() == 5, "the 5 cobblestone stay in the To storage row");
        helper.assertTrue(menu.getCarried().isEmpty(), "nothing lands on the cursor");
        helper.assertTrue(player.getInventory().countItem(Items.OAK_PLANKS) == 0 && player.getInventory().countItem(Items.COBBLESTONE) == 0,
                "nothing lands in the player inventory");
        menu.setCarried(new ItemStack(Items.DIAMOND, 3));
        menu.clicked(toNetwork + 1, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(point.outgoing().getStackInSlot(1).isEmpty() && menu.getCarried().getCount() == 3,
                "nothing can be put into the To network row");
        menu.setCarried(ItemStack.EMPTY);
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
        menu.clicked(menu.inventoryStart() + 27, 0, ContainerInput.QUICK_MOVE, player);
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 3, "shift click from the inventory moves nothing");
        helper.succeed();
    }

    /** One Speed Upgrade doubles the station's rate: twice the base rate leaves each second, and the GUI shows it. */
    @ServusGameTest(timeoutTicks = 200)
    public static void speedUpgradeDoubles(GameTestHelper helper) {
        setup(helper, 0, true, false);
        PackingStationBlockEntity point = helper.getBlockEntity(FAR, PackingStationBlockEntity.class);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        PackingStationMenu menu =
                new PackingStationMenu(1, player.getInventory(), point);
        player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 4));
        player.getInventory().setItem(1, new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get(), 1));
        int hotbar = menu.inventoryStart() + 27;
        menu.quickMoveStack(player, hotbar);
        menu.quickMoveStack(player, hotbar + 1);
        helper.assertTrue(point.upgrades().getStackInSlot(0).is(ServusFoundationItems.SPEED_UPGRADE.get()),
                "shift click puts the Speed Upgrade into the upgrade row");
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 4, "cobblestone stays in the inventory");
        int doubled = 2 * rate();
        helper.assertTrue(point.rate() == doubled, "the rate is " + doubled + " per second, got " + point.rate());
        helper.assertTrue(point.status().rate() == doubled, "the GUI is told " + doubled + "/s, got " + point.status().rate());
        helper.runAfterDelay(10, () -> {
            preload(helper, FAR, 128);
            helper.runAfterDelay(80, () -> {
                int sent = sent(helper, FAR, 128);
                helper.assertTrue(sent >= 70 * doubled / 20 && sent <= doubled + 4 * doubled + 1,
                        "about " + 4 * doubled + " planks should go out in 4 seconds, got " + sent);
                helper.succeed();
            });
        });
    }

    /** Upgrades drop when the station is broken. */
    @ServusGameTest(timeoutTicks = 60)
    public static void upgradesDropWithStation(GameTestHelper helper) {
        packingStation(helper, FAR);
        PackingStationBlockEntity point = helper.getBlockEntity(FAR, PackingStationBlockEntity.class);
        point.upgrades().setStackInSlot(1, new ItemStack(ServusFoundationItems.SPEED_UPGRADE.get()));
        helper.destroyBlock(FAR);
        helper.runAfterDelay(2, () -> {
            AABB area = new AABB(helper.absolutePos(FAR)).inflate(3);
            int upgrades = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).stream()
                    .filter(e -> e.getItem().is(ServusFoundationItems.SPEED_UPGRADE.get()))
                    .mapToInt(e -> e.getItem().getCount()).sum();
            helper.assertTrue(upgrades == 1, "the Speed Upgrade drops, got " + upgrades);
            helper.succeed();
        });
    }

    /** A station sending at its full rate shows its rate to network marked busy and nothing to storage. */
    @ServusGameTest(timeoutTicks = 200)
    public static void stationRowShowsUse(GameTestHelper helper) {
        setup(helper, 0, true, false);
        helper.runAfterDelay(10, () -> {
            preload(helper, FAR, 128);
            helper.runAfterDelay(60, () -> {
                DataNetwork line = DataNetworks
                        .networkAt(helper.getLevel(), helper.absolutePos(DUCT)).orElseThrow(() -> new AssertionError("a data line"));
                List<ProductionRows.PackingRow> rows =
                        ProductionView.packing(helper.getLevel(), line);
                helper.assertTrue(rows.size() == 1 && rows.get(0).pos().equals(helper.absolutePos(FAR)), "one Packing Station row, got " + rows);
                var row = rows.get(0);
                helper.assertTrue(row.rate() == rate(), "rate " + rate() + ", got " + row.rate());
                helper.assertTrue(row.toNetwork() >= rate() - 1 && row.toNetwork() <= rate() + 1 && row.busyToNetwork(),
                        "about the full rate to network and busy, got " + row);
                helper.assertTrue(row.toStorage() == 0 && !row.busyToStorage(), "nothing to storage, got " + row);
                helper.succeed();
            });
        });
    }

    /** Places a Storage Terminal on the data line at x 4 and hands in a stack once it joined. */
    private static StorageTerminalBlockEntity handInTerminal(GameTestHelper helper) {
        BlockPos terminalPos = new BlockPos(4, 1, 9);
        helper.setBlock(terminalPos, ServusShuttlesBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(terminalPos, StorageTerminalBlockEntity.class);
    }

    /** While a handed in stack arrives nothing is put away; once the slot is full one shuttle job takes all 64. */
    @ServusGameTest(timeoutTicks = 1200)
    public static void inputStackIsOneTrip(GameTestHelper helper) {
        Setup setup = setup(helper, 0, false, true);
        StorageTerminalBlockEntity terminal = handInTerminal(helper);
        PackingStationBlockEntity point = helper.getBlockEntity(NEAR, PackingStationBlockEntity.class);
        Set<Integer> jobs = new HashSet<>();
        boolean[] earlyJob = {false};
        helper.onEachTick(() -> {
            for (var locked : point.buffer().locked()) {
                if (locked.lock().kind() == SlotLock.Kind.OUT) {
                    jobs.add(locked.lock().jobId());
                    earlyJob[0] |= WarehouseFixtures.count(point.buffer(), Items.OAK_LOG) < 64
                            && WarehouseFixtures.count(setup.rack().items(), Items.OAK_LOG) == 0;
                }
            }
        });
        helper.runAfterDelay(10, () -> {
            terminal.input().setStackInSlot(0, new ItemStack(Items.OAK_LOG, 64));
            helper.succeedWhen(() -> {
                helper.assertTrue(WarehouseFixtures.count(setup.rack().items(), Items.OAK_LOG) == 64, "the rack should hold 64 logs, got "
                        + WarehouseFixtures.count(setup.rack().items(), Items.OAK_LOG));
                helper.assertTrue(!earlyJob[0], "no put-away should be planned before the slot is full");
                helper.assertTrue(jobs.size() == 1, "one put-away job should take the stack, got " + jobs.size());
            });
        });
    }

    /** The Input row goes to the Packing Station with the cheaper trip to the rack, and to the other one when that one is full. */
    @ServusGameTest(timeoutTicks = 400)
    public static void inputGoesToCheaperStation(GameTestHelper helper) {
        setup(helper, 0, true, true);
        StorageTerminalBlockEntity terminal = handInTerminal(helper);
        PackingStationBlockEntity far = helper.getBlockEntity(FAR, PackingStationBlockEntity.class);
        PackingStationBlockEntity near = helper.getBlockEntity(NEAR, PackingStationBlockEntity.class);
        boolean[] farGotLogs = {false};
        helper.onEachTick(() -> farGotLogs[0] |= WarehouseFixtures.count(far.buffer(), Items.OAK_LOG) > 0);
        helper.runAfterDelay(10, () -> {
            terminal.input().setStackInSlot(0, new ItemStack(Items.OAK_LOG, 16));
            Waits.when(helper, () -> terminal.input().getStackInSlot(0).isEmpty(), 200, () -> "the logs should leave the Input row", () -> {
                helper.assertTrue(!farGotLogs[0], "the logs should go to the station near the rack, not the far one");
                for (int slot = 0; slot < near.buffer().getSlots(); slot++) {
                    near.buffer().setStackInSlot(slot, new ItemStack(Items.DIAMOND, 64));
                }
                terminal.input().setStackInSlot(0, new ItemStack(Items.OAK_LOG, 16));
                Waits.when(helper, () -> farGotLogs[0], 100, () -> "with the near station full the logs should go to the far one",
                        helper::succeed);
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
            helper.setBlock(new BlockPos(5, 1, 7), ServusShuttlesBlocks.PACKING_STATION.get().defaultBlockState());
        }
        BlockPos terminalPos = new BlockPos(4, 1, 9);
        helper.setBlock(terminalPos, ServusShuttlesBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = helper.getBlockEntity(terminalPos, StorageTerminalBlockEntity.class);
            terminal.input().setStackInSlot(0, new ItemStack(Items.OAK_LOG, 64));
            helper.succeedWhen(() -> {
                helper.assertTrue(WarehouseFixtures.count(setup.rack().items(), Items.OAK_LOG) == 64, "the rack should hold 64 logs, got "
                        + WarehouseFixtures.count(setup.rack().items(), Items.OAK_LOG) + ", input " + terminal.input().getStackInSlot(0).getCount()
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
        helper.setBlock(terminalPos, ServusShuttlesBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        for (int x = 1; x <= 2; x++) {
            TerminalWiring.dataDuct(helper, new BlockPos(x, 1, 8));
        }
        BlockPos wirelessPos = new BlockPos(0, 1, 5);
        helper.setBlock(wirelessPos, ServusShuttlesBlocks.PACKING_STATION.get().defaultBlockState());
        var wireless = helper.getBlockEntity(wirelessPos, PackingStationBlockEntity.class);
        wireless.outgoing().setStackInSlot(0, new ItemStack(Items.EMERALD, 7));
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = helper.getBlockEntity(terminalPos, StorageTerminalBlockEntity.class);
            terminal.output().setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));
            terminal.input().setStackInSlot(0, new ItemStack(Items.OAK_LOG, 64));
            WarehouseNetwork network = helper.getBlockEntity(NEAR, PackingStationBlockEntity.class).network().orElseThrow();
            ItemKey logs = ItemKey.of(new ItemStack(Items.OAK_LOG));
            helper.succeedWhen(() -> {
                helper.assertTrue(network.index().count(ItemKey.of(new ItemStack(Items.EMERALD))) == 0,
                        "the second Packing Station's buffer is no stock");
                helper.assertTrue(WarehouseFixtures.count(setup.rack().items(), Items.OAK_LOG) == 64, "the rack should hold 64 logs, got "
                        + WarehouseFixtures.count(setup.rack().items(), Items.OAK_LOG) + ", input " + terminal.input().getStackInSlot(0).getCount());
                helper.assertTrue(terminal.input().getStackInSlot(0).isEmpty(), "the input should be empty");
                helper.assertTrue(network.index().count(ItemKey.of(new ItemStack(Items.DIAMOND))) == 0,
                        "the Output row is no stock");
                helper.assertTrue(network.index().count(logs) == 64, "stock counts the 64 logs once, got " + network.index().count(logs));
            });
        });
    }

    private static final ItemKey COAL = ItemKey.of(new ItemStack(Items.COAL));
    private static final ItemKey PLANKS = ItemKey.of(new ItemStack(Items.OAK_PLANKS));
    /** Job ids no fleet hands out in these tests. */
    private static final int FIRST_TEST_JOB = 1_000_000;

    /** The slots of the To network buffer that IN locks of the job name. */
    private static Set<Integer> inSlots(PackingStationBlockEntity point, int jobId) {
        Set<Integer> slots = new HashSet<>();
        point.outgoing().locked().stream()
                .filter(entry -> entry.lock().jobId() == jobId && entry.lock().kind() == SlotLock.Kind.IN)
                .forEach(entry -> slots.add(entry.slot()));
        return slots;
    }

    /** Small coal picks into the To network buffer all join one slot; an iron pick after them takes a free slot. */
    @ServusGameTest(timeoutTicks = 60)
    public static void smallPicksShareOneSlot(GameTestHelper helper) {
        setup(helper, 0, true, false);
        helper.runAfterDelay(5, () -> {
            PackingStationBlockEntity point = helper.getBlockEntity(FAR, PackingStationBlockEntity.class);
            int[] amounts = {3, 1, 2, 3, 2, 3};
            for (int i = 0; i < amounts.length; i++) {
                int job = FIRST_TEST_JOB + i;
                helper.assertTrue(point.lockDelivery(job, 0, COAL, amounts[i]) == amounts[i], "pick " + i + " should find room");
                helper.assertTrue(inSlots(point, job).equals(Set.of(0)), "pick " + i + " should lock slot 0, got " + inSlots(point, job));
            }
            for (int i = 0; i < amounts.length; i++) {
                ItemStack left = point.outgoing().insertAs(FIRST_TEST_JOB + i, COAL.stack(amounts[i]));
                helper.assertTrue(left.isEmpty(), "pick " + i + " should land whole, left " + left);
            }
            helper.assertTrue(point.outgoing().getStackInSlot(0).is(Items.COAL) && point.outgoing().getStackInSlot(0).getCount() == 14,
                    "all 14 coal should sit in slot 0, got " + point.outgoing().getStackInSlot(0));
            for (int slot = 1; slot < point.outgoing().getSlots(); slot++) {
                helper.assertTrue(point.outgoing().getStackInSlot(slot).isEmpty(), "slot " + slot + " should stay empty");
            }
            helper.assertTrue(point.outgoing().locked().isEmpty(), "every pick released its lock on landing");
            int iron = FIRST_TEST_JOB + 10;
            ItemKey ironKey = ItemKey.of(new ItemStack(Items.IRON_INGOT));
            helper.assertTrue(point.lockDelivery(iron, 0, ironKey, 16) == 16, "the iron pick should find room");
            Set<Integer> ironSlots = inSlots(point, iron);
            helper.assertTrue(ironSlots.size() == 1 && !ironSlots.contains(0), "the iron pick should lock a free slot, got " + ironSlots);
            helper.succeed();
        });
    }

    /** The station sends the planks a slot holds while another pick is on its way into that slot, and the pick still lands. */
    @ServusGameTest(timeoutTicks = 400)
    public static void sendsWhileAPickIsOnItsWay(GameTestHelper helper) {
        Setup setup = setup(helper, 0, true, false);
        helper.runAfterDelay(10, () -> {
            PackingStationBlockEntity point = helper.getBlockEntity(FAR, PackingStationBlockEntity.class);
            int job = FIRST_TEST_JOB;
            point.outgoing().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 6));
            point.outgoing().lock(0, new SlotLock(SlotLock.Kind.IN, PLANKS, 4, job));
            point.addShipment(helper.absolutePos(STATION), PLANKS, 10);
            boolean[] landed = {false};
            helper.succeedWhen(() -> {
                if (!landed[0]) {
                    int arrived = WarehouseFixtures.count(setup.station().inputs(), Items.OAK_PLANKS);
                    helper.assertTrue(arrived >= 6, "the 6 planks should leave past the pick on its way, got " + arrived);
                    helper.assertTrue(point.outgoing().getStackInSlot(0).isEmpty(), "slot 0 should be sent empty");
                    ItemStack left = point.outgoing().insertAs(job, PLANKS.stack(4));
                    helper.assertTrue(left.isEmpty(), "the pick on its way should still find its room, left " + left);
                    landed[0] = true;
                }
                int arrived = WarehouseFixtures.count(setup.station().inputs(), Items.OAK_PLANKS);
                helper.assertTrue(arrived == 10, "all 10 planks should arrive, got " + arrived);
            });
        });
    }

    /**
     * Picks ask for room in the To network buffer before they go and never
     * reserve more than it has left: with room for 20 planks, over a partial
     * stack and a slot a pick on its way already reserves most of, two picks
     * of 10 go and the rest wait with no locks, a pick of 32 waits whole, and
     * once 10 planks left the buffer one more pick of 10 goes.
     */
    @ServusGameTest(timeoutTicks = 60)
    public static void picksNeverExceedBufferRoom(GameTestHelper helper) {
        setup(helper, 0, true, false);
        helper.runAfterDelay(5, () -> {
            PackingStationBlockEntity point = helper.getBlockEntity(FAR, PackingStationBlockEntity.class);
            WarehouseNetwork network = point.network().orElseThrow(() -> new AssertionError("the Packing Station should be in a warehouse"));
            LockedSlots outgoing = point.outgoing();
            outgoing.setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 54));
            outgoing.lock(1, new SlotLock(SlotLock.Kind.IN, PLANKS, 54, FIRST_TEST_JOB));
            for (int slot = 2; slot < outgoing.getSlots(); slot++) {
                outgoing.setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
            }
            int room = 2 * 64;
            // Wireless shipments keep the demand open without a target grant; nothing ticks inside this callback.
            BlockPos receiver = helper.absolutePos(NEAR);
            point.addWirelessShipment(receiver, "elsewhere", PLANKS, 54 + 54 + 50 + 32 + 10, null);
            point.addWirelessShipment(receiver, "elsewhere", ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64 * 7, null);
            BlockPos at = helper.absolutePos(FAR);
            BlockPos source = helper.absolutePos(RACK);
            int passed = 0;
            for (int i = 1; i <= 5; i++) {
                PickJob pick = new PickJob(FIRST_TEST_JOB + i, 0, PLANKS, 10, source, at);
                boolean ready = network.readyToSend(pick);
                helper.assertTrue(ready == (i <= 2), "pick " + i + " of 10 should " + (i <= 2 ? "go" : "wait"));
                int locked = outgoing.lockedAmount(pick.id(), SlotLock.Kind.IN);
                helper.assertTrue(locked == (ready ? 10 : 0), "pick " + i + " should lock " + (ready ? 10 : 0) + ", locked " + locked);
                passed += ready ? 1 : 0;
                assertWithinRoom(helper, outgoing, room);
            }
            helper.assertTrue(passed == 2, "two picks should go, got " + passed);
            PickJob big = new PickJob(FIRST_TEST_JOB + 6, 0, PLANKS, 32, source, at);
            helper.assertTrue(!network.readyToSend(big) && !outgoing.holds(big.id()), "the pick of 32 should wait whole, reserving nothing");
            ItemStack sent = outgoing.extractItem(0, 10, false);
            helper.assertTrue(sent.getCount() == 10, "the station takes 10 planks out of the slot picks still come into, got " + sent);
            PickJob next = new PickJob(FIRST_TEST_JOB + 7, 0, PLANKS, 10, source, at);
            helper.assertTrue(network.readyToSend(next) && outgoing.lockedAmount(next.id(), SlotLock.Kind.IN) == 10,
                    "once 10 planks left, one more pick of 10 should go");
            assertWithinRoom(helper, outgoing, room);
            helper.succeed();
        });
    }

    /** Planks held in the To network buffer plus room IN locks reserve for planks never exceed the planks' room there. */
    private static void assertWithinRoom(GameTestHelper helper, LockedSlots outgoing, int room) {
        int reserved = 0;
        for (LockedSlots.Locked entry : outgoing.locked()) {
            if (entry.lock().kind() == SlotLock.Kind.IN && entry.lock().key().equals(PLANKS)) {
                reserved += entry.lock().amount();
            }
        }
        int held = WarehouseFixtures.count(outgoing, Items.OAK_PLANKS);
        helper.assertTrue(held + reserved <= room, "planks held " + held + " plus reserved " + reserved + " exceed the room " + room);
        for (int slot = 0; slot < outgoing.getSlots(); slot++) {
            int inSlot = outgoing.getStackInSlot(slot).getCount() + outgoing.reserved(slot, SlotLock.Kind.IN, 0);
            helper.assertTrue(inSlot <= 64, "slot " + slot + " holds and reserves " + inSlot + ", more than a stack");
        }
    }
}
