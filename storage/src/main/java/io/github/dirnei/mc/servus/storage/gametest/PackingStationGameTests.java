package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;

import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlock;

import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.packing.ServusStorageShipping;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Packing Stations between a warehouse and a data line. Rails run along z 6 from
 * x 2 to 9 with the controller at x 1 and a crate beside it at x 0, a garage
 * north of the rail at x 2, Packing Stations south of the rail at x 3 (far) and
 * x 7 (near), a rack at x 9 facing the rail, data ducts along z 8 from x 3 to
 * 7 and a Crafting Station south of the ducts at x 5.
 */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PackingStationGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos CRATE = new BlockPos(0, 1, 6);
    private static final BlockPos CONTROLLER = new BlockPos(1, 1, 6);
    private static final BlockPos GARAGE = new BlockPos(2, 1, 5);
    private static final BlockPos FAR = new BlockPos(3, 1, 7);
    private static final BlockPos NEAR = new BlockPos(7, 1, 7);
    private static final BlockPos RACK = new BlockPos(9, 1, 7);
    private static final BlockPos STATION = new BlockPos(5, 1, 9);
    private static final BlockPos DUCT = new BlockPos(5, 1, 8);

    private PackingStationGameTests() {
    }

    private record Setup(CrateBlockEntity crate, CraftingStationBlockEntity station, HeavyRackBlockEntity rack) {
    }

    private static void data(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos);
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

    private static Setup setup(GameTestHelper helper, int cratePlanks, int rackPlanks, boolean far, boolean near) {
        helper.setBlock(CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity crate = helper.getBlockEntity(CRATE);
        if (cratePlanks > 0) {
            crate.insert(new ItemStack(Items.OAK_PLANKS, cratePlanks), false);
        }
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (int x = 2; x <= 9; x++) {
            rail(helper, new BlockPos(x, 1, 6));
        }
        helper.setBlock(GARAGE, ServusStorageBlocks.SHUTTLE_GARAGE.get().defaultBlockState().setValue(ShuttleGarageBlock.FACING, Direction.NORTH));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(GARAGE);
        helper.assertTrue(garage.addShuttle(), "one shuttle fits");
        helper.setBlock(RACK, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.NORTH));
        HeavyRackBlockEntity rack = helper.getBlockEntity(RACK);
        if (rackPlanks > 0) {
            rack.items().insertItem(0, new ItemStack(Items.OAK_PLANKS, rackPlanks), false);
        }
        if (far) {
            packingStation(helper, FAR);
        }
        if (near) {
            packingStation(helper, NEAR);
        }
        helper.setBlock(STATION, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState()
                .setValue(CraftingStationBlock.FACING, Direction.SOUTH));
        CraftingStationBlockEntity station = helper.getBlockEntity(STATION);
        StationJobs.sticksJob(station, 64);
        for (int slot = 0; slot < station.outputs().getSlots(); slot++) {
            station.outputs().setStackInSlot(slot, new ItemStack(Items.STICK, 64));
        }
        for (int x = 3; x <= 7; x++) {
            data(helper, new BlockPos(x, 1, 8));
        }
        return new Setup(crate, station, rack);
    }

    private static int count(ItemStackHandler handler, Item item) {
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
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void packingStationIsLinkMember(GameTestHelper helper) {
        setup(helper, 0, 0, true, true);
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
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void packingStationJoinsWarehouse(GameTestHelper helper) {
        setup(helper, 8, 0, true, false);
        helper.runAfterDelay(10, () -> {
            PackingStationBlockEntity point = helper.getBlockEntity(FAR);
            point.outgoing().setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
            WarehouseNetwork network = point.network().orElseThrow(() -> new AssertionError("the Packing Station should be in a warehouse"));
            helper.assertTrue(network.status() == WarehouseNetwork.Status.ONLINE, "the warehouse should be online");
            helper.assertTrue(network.index().count(io.github.dirnei.mc.servus.storage.network.ItemKey.of(new ItemStack(Items.DIAMOND))) == 0,
                    "items in the Packing Station are not stock");
            helper.succeed();
        });
    }

    /** Planks sent as packets into a Packing Station end up in the crate. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void packetsArePutAway(GameTestHelper helper) {
        Setup setup = setup(helper, 32, 0, true, false);
        helper.runAfterDelay(10, () -> {
            DataPackets.send(helper.getLevel(), helper.absolutePos(STATION), helper.absolutePos(FAR), new ItemStack(Items.OAK_PLANKS, 16))
                    .orElseThrow(() -> new AssertionError("the send should start"));
            PackingStationBlockEntity point = helper.getBlockEntity(FAR);
            helper.succeedWhen(() -> {
                helper.assertTrue(setup.crate().count(0) == 48, "48 planks should be in the crate, got " + setup.crate().count(0));
                helper.assertTrue(point.isEmpty(), "the intake should be empty again");
            });
        });
    }

    /** Bread put into a Storage Terminal's input travels to the Packing Station and a shuttle puts it into the rack. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void inputReachesRack(GameTestHelper helper) {
        Setup setup = setup(helper, 0, 0, false, true);
        setup.crate().insert(new ItemStack(Items.COBBLESTONE, setup.crate().capacity()), false);
        BlockPos terminalPos = new BlockPos(4, 1, 9);
        helper.setBlock(terminalPos, ServusStorageBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = helper.getBlockEntity(terminalPos);
            terminal.input().setStackInSlot(0, new ItemStack(Items.BREAD, 10));
            helper.succeedWhen(() -> {
                helper.assertTrue(terminal.input().getStackInSlot(0).isEmpty(), "the input should be empty");
                helper.assertTrue(count(setup.rack().items(), Items.BREAD) == 10,
                        "the rack should hold 10 bread, got " + count(setup.rack().items(), Items.BREAD));
            });
        });
    }

    /** Planks in a crate are shipped at once to the Packing Station and travel as packets to the station. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void shipFromCrate(GameTestHelper helper) {
        Setup setup = setup(helper, 32, 0, true, false);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork.Shipping shipping = ship(helper, 16);
            helper.assertTrue(shipping.shipped(), "shipping should start, refused " + shipping.refusal());
            PackingStationBlockEntity.Shipment shipment = shipping.shipment().orElseThrow();
            helper.assertTrue(shipment.planned() == 16, "16 planned, got " + shipment.planned());
            helper.succeedWhen(() -> {
                helper.assertTrue(count(setup.station().inputs(), Items.OAK_PLANKS) == 16, "16 planks should reach the station");
                helper.assertTrue(shipment.delivered() == 16 && shipment.ended(), "the shipment should end with 16 delivered");
                helper.assertTrue(setup.crate().count(0) == 16, "16 planks stay in the crate");
            });
        });
    }

    /** Planks only in a rack ride a shuttle to the Packing Station first. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void shipFromRackByShuttle(GameTestHelper helper) {
        Setup setup = setup(helper, 0, 16, false, true);
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
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void nearerPackingStationWins(GameTestHelper helper) {
        Setup setup = setup(helper, 0, 16, true, true);
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
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void shipRefusedWithoutPackingStation(GameTestHelper helper) {
        Setup setup = setup(helper, 32, 0, false, false);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork.Shipping shipping = ship(helper, 16);
            helper.assertTrue(shipping.refusal().equals("no_packing_station"), "refused with no_packing_station, got " + shipping.refusal());
            helper.assertTrue(setup.crate().count(0) == 32, "the planks stay in the crate");
            helper.succeed();
        });
    }

    /** A broken Packing Station drops both buffers (the block itself drops by its loot table when mined with a pickaxe). */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void brokenPackingStationDropsBuffers(GameTestHelper helper) {
        packingStation(helper, FAR);
        PackingStationBlockEntity point = helper.getBlockEntity(FAR);
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
        PackingStationBlockEntity point = helper.getBlockEntity(pos);
        point.outgoing().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, planks));
        return point.addShipment(helper.absolutePos(STATION), io.github.dirnei.mc.servus.storage.network.ItemKey.of(
                new ItemStack(Items.OAK_PLANKS)), planks);
    }

    /** How many of the preloaded planks have left a station's To network buffer. */
    private static int sent(GameTestHelper helper, BlockPos pos, int preloaded) {
        PackingStationBlockEntity point = helper.getBlockEntity(pos);
        return preloaded - count(point.outgoing(), Items.OAK_PLANKS);
    }

    /** A shipment of 64 planks leaves the Packing Station at about 8 per second. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void drainsAtEightPerSecond(GameTestHelper helper) {
        Setup setup = setup(helper, 64, 0, true, false);
        helper.runAfterDelay(10, () -> {
            long start = helper.getTick();
            WarehouseNetwork.Shipping shipping = ship(helper, 64);
            helper.assertTrue(shipping.shipped(), "shipping should start, refused " + shipping.refusal());
            PackingStationBlockEntity point = helper.getBlockEntity(FAR);
            helper.assertTrue(count(point.outgoing(), Items.OAK_PLANKS) == 64, "the picks wait in the To network buffer");
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
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void twoStationsDoubleThroughput(GameTestHelper helper) {
        setup(helper, 0, 0, true, true);
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
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void bothDirectionsAtOnce(GameTestHelper helper) {
        setup(helper, 0, 0, true, false);
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
}
