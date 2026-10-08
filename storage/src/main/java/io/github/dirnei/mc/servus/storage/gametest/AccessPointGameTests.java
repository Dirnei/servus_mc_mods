package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.access.AccessPointBlockEntity;
import io.github.dirnei.mc.servus.storage.access.ServusStorageShipping;
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
 * Access Points between a warehouse and a data line. Rails run along z 6 from
 * x 2 to 9 with the controller at x 1 and a crate beside it at x 0, a garage
 * north of the rail at x 2, Access Points south of the rail at x 3 (far) and
 * x 7 (near), a rack at x 9 facing the rail, data ducts along z 8 from x 3 to
 * 7 and a Crafting Station south of the ducts at x 5.
 */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AccessPointGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos CRATE = new BlockPos(0, 1, 6);
    private static final BlockPos CONTROLLER = new BlockPos(1, 1, 6);
    private static final BlockPos GARAGE = new BlockPos(2, 1, 5);
    private static final BlockPos FAR = new BlockPos(3, 1, 7);
    private static final BlockPos NEAR = new BlockPos(7, 1, 7);
    private static final BlockPos RACK = new BlockPos(9, 1, 7);
    private static final BlockPos STATION = new BlockPos(5, 1, 9);
    private static final BlockPos DUCT = new BlockPos(5, 1, 8);

    private AccessPointGameTests() {
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

    private static void accessPoint(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.ACCESS_POINT.get().defaultBlockState());
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
            accessPoint(helper, FAR);
        }
        if (near) {
            accessPoint(helper, NEAR);
        }
        helper.setBlock(STATION, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState()
                .setValue(CraftingStationBlock.FACING, Direction.SOUTH));
        CraftingStationBlockEntity station = helper.getBlockEntity(STATION);
        station.pattern().setStackInSlot(1, new ItemStack(Items.OAK_PLANKS));
        station.pattern().setStackInSlot(4, new ItemStack(Items.OAK_PLANKS));
        helper.assertTrue(station.saveFromPattern().isEmpty(), "the stick pattern should save");
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

    /** Both Access Points are link members of the data line. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void accessPointIsLinkMember(GameTestHelper helper) {
        setup(helper, 0, 0, true, true);
        helper.runAfterDelay(5, () -> {
            DataNetwork network = DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(DUCT)).orElseThrow();
            List<BlockPos> links = network.dataMembers(DataRole.LINK).stream().map(DataMember::pos).toList();
            helper.assertTrue(links.contains(helper.absolutePos(FAR)) && links.contains(helper.absolutePos(NEAR)),
                    "both Access Points should be link members, got " + network.dataMembers());
            helper.assertTrue(network.dataMembers(DataRole.PRODUCER).size() == 1, "the station is the producer");
            helper.succeed();
        });
    }

    /** An Access Point belongs to the warehouse of the rail beside it and holds no stock. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void accessPointJoinsWarehouse(GameTestHelper helper) {
        setup(helper, 8, 0, true, false);
        helper.runAfterDelay(10, () -> {
            AccessPointBlockEntity point = helper.getBlockEntity(FAR);
            point.outgoing().setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
            WarehouseNetwork network = point.network().orElseThrow(() -> new AssertionError("the Access Point should be in a warehouse"));
            helper.assertTrue(network.status() == WarehouseNetwork.Status.ONLINE, "the warehouse should be online");
            helper.assertTrue(network.index().count(io.github.dirnei.mc.servus.storage.network.ItemKey.of(new ItemStack(Items.DIAMOND))) == 0,
                    "items in the Access Point are not stock");
            helper.succeed();
        });
    }

    /** Planks sent as packets into an Access Point end up in the crate. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void packetsArePutAway(GameTestHelper helper) {
        Setup setup = setup(helper, 32, 0, true, false);
        helper.runAfterDelay(10, () -> {
            DataPackets.send(helper.getLevel(), helper.absolutePos(STATION), helper.absolutePos(FAR), new ItemStack(Items.OAK_PLANKS, 16))
                    .orElseThrow(() -> new AssertionError("the send should start"));
            AccessPointBlockEntity point = helper.getBlockEntity(FAR);
            helper.succeedWhen(() -> {
                helper.assertTrue(setup.crate().count(0) == 48, "48 planks should be in the crate, got " + setup.crate().count(0));
                helper.assertTrue(point.isEmpty(), "the intake should be empty again");
            });
        });
    }

    /** Planks in a crate are shipped at once to the Access Point and travel as packets to the station. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void shipFromCrate(GameTestHelper helper) {
        Setup setup = setup(helper, 32, 0, true, false);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork.Shipping shipping = ship(helper, 16);
            helper.assertTrue(shipping.shipped(), "shipping should start, refused " + shipping.refusal());
            AccessPointBlockEntity.Shipment shipment = shipping.shipment().orElseThrow();
            helper.assertTrue(shipment.planned() == 16, "16 planned, got " + shipment.planned());
            helper.succeedWhen(() -> {
                helper.assertTrue(count(setup.station().inputs(), Items.OAK_PLANKS) == 16, "16 planks should reach the station");
                helper.assertTrue(shipment.delivered() == 16 && shipment.ended(), "the shipment should end with 16 delivered");
                helper.assertTrue(setup.crate().count(0) == 16, "16 planks stay in the crate");
            });
        });
    }

    /** Planks only in a rack ride a shuttle to the Access Point first. */
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

    /** With two Access Points the one nearer to the rack serves the shipment. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void nearerAccessPointWins(GameTestHelper helper) {
        Setup setup = setup(helper, 0, 16, true, true);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork.Shipping shipping = ship(helper, 16);
            helper.assertTrue(shipping.shipped(), "shipping should start, refused " + shipping.refusal());
            helper.assertTrue(helper.absolutePos(NEAR).equals(shipping.accessPoint()),
                    "the near Access Point should be chosen, got " + shipping.accessPoint());
            helper.succeedWhen(() -> helper.assertTrue(count(setup.station().inputs(), Items.OAK_PLANKS) == 16,
                    "16 planks should reach the station"));
        });
    }

    /** Without an Access Point on the data line nothing leaves the warehouse. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void shipRefusedWithoutAccessPoint(GameTestHelper helper) {
        Setup setup = setup(helper, 32, 0, false, false);
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork.Shipping shipping = ship(helper, 16);
            helper.assertTrue(shipping.refusal().equals("no_access_point"), "refused with no_access_point, got " + shipping.refusal());
            helper.assertTrue(setup.crate().count(0) == 32, "the planks stay in the crate");
            helper.succeed();
        });
    }

    /** A broken Access Point drops both buffers (the block itself drops by its loot table when mined with a pickaxe). */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void brokenAccessPointDropsBuffers(GameTestHelper helper) {
        accessPoint(helper, FAR);
        AccessPointBlockEntity point = helper.getBlockEntity(FAR);
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
}
