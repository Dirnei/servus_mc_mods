package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.data.packet.Delivery;
import io.github.dirnei.mc.servus.foundation.data.packet.DeliveryReason;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
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
 * Packets over the data line between a Warehouse Controller (a crate of
 * planks beside it) and a Crafting Station: controller at x 2, six data
 * ducts at x 3 to 8, station at x 9, all at y 1, z 3.
 */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DataPacketGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos CRATE = new BlockPos(1, 1, 3);
    private static final BlockPos SPARE_CRATE = new BlockPos(2, 1, 2);
    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 3);
    private static final BlockPos STATION = new BlockPos(9, 1, 3);
    private static final int FIRST_DUCT = 3;
    private static final int LAST_DUCT = 8;

    private DataPacketGameTests() {
    }

    private record Setup(CrateBlockEntity crate, CraftingStationBlockEntity station) {
    }

    private static void data(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos);
        duct.addLine(Line.DATA);
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
    }

    /** Controller with 32 planks, six data ducts, a sticks station whose outputs are full so it never crafts. */
    private static Setup setup(GameTestHelper helper, boolean ducts) {
        helper.setBlock(CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity crate = helper.getBlockEntity(CRATE);
        crate.insert(new ItemStack(Items.OAK_PLANKS, 32), false);
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(STATION, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState()
                .setValue(CraftingStationBlock.FACING, Direction.NORTH));
        CraftingStationBlockEntity station = helper.getBlockEntity(STATION);
        station.pattern().setStackInSlot(1, new ItemStack(Items.OAK_PLANKS));
        station.pattern().setStackInSlot(4, new ItemStack(Items.OAK_PLANKS));
        helper.assertTrue(station.saveFromPattern().isEmpty(), "the stick pattern should save");
        for (int slot = 0; slot < station.outputs().getSlots(); slot++) {
            station.outputs().setStackInSlot(slot, new ItemStack(Items.STICK, 64));
        }
        if (ducts) {
            for (int x = FIRST_DUCT; x <= LAST_DUCT; x++) {
                data(helper, new BlockPos(x, 1, 3));
            }
        }
        return new Setup(crate, station);
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

    private static int dropped(GameTestHelper helper, Item item) {
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item))
                .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    private static Delivery move(GameTestHelper helper, BlockPos from, BlockPos to, Item item, int amount) {
        return DataPackets.move(helper.getLevel(), helper.absolutePos(from), helper.absolutePos(to), new ItemStack(item), amount)
                .orElseThrow(() -> new AssertionError("the move should start"));
    }

    /** Sixteen planks move from the warehouse into the station in two packets. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void packetMoveControllerToStation(GameTestHelper helper) {
        Setup setup = setup(helper, true);
        helper.runAfterDelay(2, () -> {
            Delivery delivery = move(helper, CONTROLLER, STATION, Items.OAK_PLANKS, 16);
            helper.succeedWhen(() -> {
                helper.assertTrue(delivery.state() == Delivery.State.DONE, "delivery should be done, is " + delivery);
                helper.assertTrue(setup.crate().count(0) == 16, "16 planks should remain, got " + setup.crate().count(0));
                helper.assertTrue(count(setup.station().inputs(), Items.OAK_PLANKS) == 16, "16 planks should be in the station");
                helper.assertTrue(delivery.delivered() == 16, "delivered 16, got " + delivery.delivered());
            });
        });
    }

    /** Seven blocks at four per second take 35 ticks. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void packetTravelTime(GameTestHelper helper) {
        Setup setup = setup(helper, true);
        helper.runAfterDelay(2, () -> {
            Delivery delivery = move(helper, CONTROLLER, STATION, Items.OAK_PLANKS, 8);
            helper.runAfterDelay(30, () -> helper.assertTrue(count(setup.station().inputs(), Items.OAK_PLANKS) == 0,
                    "the packet should still be on the way after 30 ticks"));
            helper.runAfterDelay(42, () -> {
                helper.assertTrue(count(setup.station().inputs(), Items.OAK_PLANKS) == 8, "the packet should have arrived after 42 ticks");
                helper.assertTrue(delivery.state() == Delivery.State.DONE, "delivery done");
                helper.succeed();
            });
        });
    }

    /** A sent stack of 20 travels as 8, 8 and 4, one packet at a time. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void packetSendSplits(GameTestHelper helper) {
        Setup setup = setup(helper, true);
        helper.runAfterDelay(2, () -> {
            Delivery delivery = DataPackets.send(helper.getLevel(), helper.absolutePos(CONTROLLER), helper.absolutePos(STATION),
                    new ItemStack(Items.OAK_PLANKS, 20)).orElseThrow();
            helper.runAfterDelay(45, () -> helper.assertTrue(count(setup.station().inputs(), Items.OAK_PLANKS) == 8,
                    "only the first packet of 8 should have arrived, got " + count(setup.station().inputs(), Items.OAK_PLANKS)));
            helper.runAfterDelay(80, () -> helper.assertTrue(count(setup.station().inputs(), Items.OAK_PLANKS) == 16,
                    "two packets after 80 ticks"));
            helper.succeedWhen(() -> {
                helper.assertTrue(delivery.state() == Delivery.State.DONE && delivery.delivered() == 20, "all 20 delivered, " + delivery);
                helper.assertTrue(count(setup.station().inputs(), Items.OAK_PLANKS) == 20, "20 planks in the station");
                helper.assertTrue(setup.crate().count(0) == 32, "a send takes nothing from the warehouse");
            });
        });
    }

    /** A target with room for 3 takes 3; the other 5 wait and go in once room frees up. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void packetTargetFullRetries(GameTestHelper helper) {
        Setup setup = setup(helper, true);
        ItemStackHandler inputs = setup.station().inputs();
        for (int slot = 0; slot < inputs.getSlots(); slot++) {
            inputs.setStackInSlot(slot, new ItemStack(Items.OAK_PLANKS, slot == 0 ? 61 : 64));
        }
        helper.runAfterDelay(2, () -> {
            Delivery delivery = move(helper, CONTROLLER, STATION, Items.OAK_PLANKS, 8);
            helper.runAfterDelay(45, () -> {
                helper.assertTrue(delivery.delivered() == 3 && !delivery.ended(), "3 delivered and 5 waiting, " + delivery);
                inputs.setStackInSlot(1, ItemStack.EMPTY);
                helper.runAfterDelay(25, () -> {
                    helper.assertTrue(delivery.state() == Delivery.State.DONE && delivery.delivered() == 8, "all 8 delivered after the retry, " + delivery);
                    helper.succeed();
                });
            });
        });
    }

    /** A move of 16 sticks from a station holding 4 ends done with 4. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void packetSourceShortEndsDone(GameTestHelper helper) {
        Setup setup = setup(helper, true);
        helper.setBlock(SPARE_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        ItemStackHandler outputs = setup.station().outputs();
        for (int slot = 0; slot < outputs.getSlots(); slot++) {
            outputs.setStackInSlot(slot, slot == 0 ? new ItemStack(Items.STICK, 4) : ItemStack.EMPTY);
        }
        helper.runAfterDelay(2, () -> {
            Delivery delivery = move(helper, STATION, CONTROLLER, Items.STICK, 16);
            helper.succeedWhen(() -> {
                helper.assertTrue(delivery.state() == Delivery.State.DONE && delivery.delivered() == 4, "done with 4, " + delivery);
                CrateBlockEntity spare = helper.getBlockEntity(SPARE_CRATE);
                helper.assertTrue(spare.count(0) == 4, "the 4 sticks should be in the spare crate, got " + spare.count(0));
            });
        });
    }

    /** Without a route the move fails at once and the warehouse keeps its planks. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void packetNoRouteKeepsItems(GameTestHelper helper) {
        Setup setup = setup(helper, true);
        helper.setBlock(new BlockPos(5, 1, 3), Blocks.AIR.defaultBlockState());
        helper.runAfterDelay(3, () -> {
            Delivery delivery = move(helper, CONTROLLER, STATION, Items.OAK_PLANKS, 8);
            helper.assertTrue(delivery.state() == Delivery.State.FAILED && delivery.reason() == DeliveryReason.NO_ROUTE,
                    "the move should fail with no route, " + delivery);
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(setup.crate().count(0) == 32, "the warehouse keeps its planks");
                helper.succeed();
            });
        });
    }

    /** Breaking a duct on the route sends the packet round a detour. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void packetDetourArrives(GameTestHelper helper) {
        Setup setup = setup(helper, true);
        data(helper, new BlockPos(4, 1, 4));
        data(helper, new BlockPos(5, 1, 4));
        data(helper, new BlockPos(6, 1, 4));
        helper.runAfterDelay(2, () -> {
            Delivery delivery = move(helper, CONTROLLER, STATION, Items.OAK_PLANKS, 8);
            helper.runAfterDelay(6, () -> helper.setBlock(new BlockPos(5, 1, 3), Blocks.AIR.defaultBlockState()));
            helper.succeedWhen(() -> {
                helper.assertTrue(delivery.state() == Delivery.State.DONE && delivery.delivered() == 8, "delivered by the detour, " + delivery);
                helper.assertTrue(count(setup.station().inputs(), Items.OAK_PLANKS) == 8, "8 planks in the station");
            });
        });
    }

    /** Breaking the only route keeps all planks: they land in the station, the warehouse or on the ground. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void packetBrokenRouteKeepsItems(GameTestHelper helper) {
        Setup setup = setup(helper, true);
        helper.runAfterDelay(2, () -> {
            Delivery delivery = move(helper, CONTROLLER, STATION, Items.OAK_PLANKS, 8);
            helper.runAfterDelay(6, () -> helper.setBlock(new BlockPos(5, 1, 3), Blocks.AIR.defaultBlockState()));
            helper.runAfterDelay(15, () -> {
                helper.assertTrue(delivery.state() == Delivery.State.FAILED && delivery.reason() == DeliveryReason.ROUTE_BROKEN,
                        "the delivery should fail with route broken, " + delivery);
                int total = setup.crate().count(0) + count(setup.station().inputs(), Items.OAK_PLANKS) + dropped(helper, Items.OAK_PLANKS);
                helper.assertTrue(total == 32, "all 32 planks should still exist, got " + total);
                helper.succeed();
            });
        });
    }
}
