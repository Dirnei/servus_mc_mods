package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.data.DataSummary;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The Utility Duct's data line with Storage blocks as its members. */
public final class DataCableGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos A = new BlockPos(3, 1, 3);
    private static final BlockPos B = new BlockPos(4, 1, 3);
    private static final BlockPos C = new BlockPos(5, 1, 3);
    private static final BlockPos WEST_END = new BlockPos(2, 1, 3);
    private static final BlockPos EAST_END = new BlockPos(6, 1, 3);
    private static final BlockPos ABOVE_B = new BlockPos(4, 2, 3);

    private DataCableGameTests() {
    }

    private static UtilityDuctBlockEntity duct(GameTestHelper helper, BlockPos pos, Line... lines) {
        helper.setBlock(pos, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos, UtilityDuctBlockEntity.class);
        for (Line line : lines) {
            duct.addLine(line);
        }
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
        return duct;
    }

    private static UtilityDuctBlockEntity data(GameTestHelper helper, BlockPos pos) {
        return duct(helper, pos, Line.DATA);
    }

    private static void dataRow(GameTestHelper helper) {
        data(helper, A);
        data(helper, B);
        data(helper, C);
    }

    private static Optional<DataNetwork> network(GameTestHelper helper, BlockPos pos) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(pos));
    }

    private static DataNetwork requireNetwork(GameTestHelper helper, BlockPos pos) {
        return network(helper, pos).orElseThrow(() -> new AssertionError("no data network at " + pos));
    }

    private static boolean dataArm(GameTestHelper helper, BlockPos pos, Direction side) {
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos, UtilityDuctBlockEntity.class);
        return duct.hasArm(Line.DATA, side);
    }

    private static CraftingStationBlockEntity sticksStation(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
        CraftingStationBlockEntity station = helper.getBlockEntity(pos, CraftingStationBlockEntity.class);
        return station;
    }

    private static void controller(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        PowerFixtures.fill(helper, pos);
    }

    /** Three data ducts in a row form one network and show data arms toward each other. */
    @ServusGameTest(timeoutTicks = 40)
    public static void dataCableRowIsOneNetwork(GameTestHelper helper) {
        dataRow(helper);
        helper.runAfterDelay(2, () -> {
            DataNetwork network = requireNetwork(helper, A);
            helper.assertTrue(network == requireNetwork(helper, C), "both ends should be in one network");
            helper.assertTrue(network.cables() == 3, "three ducts, got " + network.cables());
            helper.assertTrue(dataArm(helper, A, Direction.EAST) && dataArm(helper, B, Direction.WEST) && dataArm(helper, B, Direction.EAST),
                    "data ducts should show data arms toward each other");
            helper.succeed();
        });
    }

    /** Breaking the middle duct splits the row into two networks. */
    @ServusGameTest(timeoutTicks = 40)
    public static void dataCableSplitsWhenBroken(GameTestHelper helper) {
        dataRow(helper);
        helper.runAfterDelay(2, () -> {
            helper.setBlock(B, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(2, () -> {
                DataNetwork west = requireNetwork(helper, A);
                DataNetwork east = requireNetwork(helper, C);
                helper.assertTrue(west != east, "the ends should be two networks after the split");
                helper.assertTrue(west.cables() == 1 && east.cables() == 1, "one duct each");
                helper.assertTrue(!dataArm(helper, A, Direction.EAST), "the arm toward the gap should be gone");
                helper.succeed();
            });
        });
    }

    /** A middle duct without the data line does not carry data on. */
    @ServusGameTest(timeoutTicks = 40)
    public static void dataCableGapInTheLine(GameTestHelper helper) {
        duct(helper, A, Line.ITEM, Line.DATA);
        duct(helper, B, Line.ITEM);
        duct(helper, C, Line.ITEM, Line.DATA);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(requireNetwork(helper, A) != requireNetwork(helper, C), "the gap should split the data line");
            helper.assertTrue(network(helper, B).isEmpty(), "the item only duct has no data network");
            helper.succeed();
        });
    }

    /** A Crafting Station is a producer and a Warehouse Controller a stock member; both get a data arm. */
    @ServusGameTest(timeoutTicks = 40)
    public static void dataCableFindsStationAndController(GameTestHelper helper) {
        sticksStation(helper, WEST_END);
        controller(helper, EAST_END);
        dataRow(helper);
        helper.runAfterDelay(2, () -> {
            DataNetwork network = requireNetwork(helper, B);
            helper.assertTrue(network.dataMembers(DataRole.PRODUCER).size() == 1
                            && network.dataMembers(DataRole.PRODUCER).get(0).pos().equals(helper.absolutePos(WEST_END)),
                    "the station should be the one producer, got " + network.dataMembers());
            helper.assertTrue(network.dataMembers(DataRole.STOCK).size() == 1
                            && network.dataMembers(DataRole.STOCK).get(0).pos().equals(helper.absolutePos(EAST_END)),
                    "the controller should be the one stock member, got " + network.dataMembers());
            helper.assertTrue(dataArm(helper, A, Direction.WEST) && dataArm(helper, C, Direction.EAST), "data arms toward both members");
            helper.succeed();
        });
    }

    /** A chest is no member and gets no arm. */
    @ServusGameTest(timeoutTicks = 40)
    public static void dataCableIgnoresChest(GameTestHelper helper) {
        helper.setBlock(EAST_END, Blocks.CHEST.defaultBlockState());
        dataRow(helper);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(requireNetwork(helper, B).dataMembers().isEmpty(), "a chest must not be a member");
            UtilityDuctBlockEntity east = helper.getBlockEntity(C, UtilityDuctBlockEntity.class);
            helper.assertTrue((east.connections() & 1 << Direction.EAST.ordinal()) == 0, "no arm toward the chest");
            helper.succeed();
        });
    }

    /** A member placed later joins, and leaves again when it is broken. */
    @ServusGameTest(timeoutTicks = 40)
    public static void dataCableMemberBrokenLeaves(GameTestHelper helper) {
        dataRow(helper);
        helper.runAfterDelay(2, () -> {
            sticksStation(helper, EAST_END);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(requireNetwork(helper, A).dataMembers(DataRole.PRODUCER).size() == 1, "the station placed later should join");
                helper.assertTrue(dataArm(helper, C, Direction.EAST), "the arm toward the new station should show");
                helper.setBlock(EAST_END, Blocks.AIR.defaultBlockState());
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(requireNetwork(helper, A).dataMembers().isEmpty(), "the broken station should leave");
                    helper.assertTrue(!dataArm(helper, C, Direction.EAST), "the arm should go with it");
                    helper.succeed();
                });
            });
        });
    }

    /** Two ducts of one network touching the same controller count it once. */
    @ServusGameTest(timeoutTicks = 40)
    public static void dataCableTouchedTwiceCountsOnce(GameTestHelper helper) {
        controller(helper, B);
        data(helper, A);
        data(helper, ABOVE_B);
        data(helper, A.above());
        helper.runAfterDelay(2, () -> {
            DataNetwork network = requireNetwork(helper, A);
            helper.assertTrue(network == requireNetwork(helper, ABOVE_B), "the three ducts are one network");
            helper.assertTrue(network.dataMembers().size() == 1, "one member, got " + network.dataMembers());
            helper.succeed();
        });
    }

    /** Turning a data side off drops the member there and splits the link to the next duct. */
    @ServusGameTest(timeoutTicks = 60)
    public static void dataCableSideOff(GameTestHelper helper) {
        controller(helper, EAST_END);
        dataRow(helper);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(requireNetwork(helper, A).dataMembers().size() == 1, "the controller starts as a member");
            UtilityDuctBlockEntity east = helper.getBlockEntity(C, UtilityDuctBlockEntity.class);
            east.setMode(Line.DATA, Direction.EAST, SideMode.DISABLED);
            UtilityDuctBlockEntity middle = helper.getBlockEntity(B, UtilityDuctBlockEntity.class);
            middle.setMode(Line.DATA, Direction.WEST, SideMode.DISABLED);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(requireNetwork(helper, C).dataMembers().isEmpty(), "an off side reaches no member");
                helper.assertTrue(!dataArm(helper, C, Direction.EAST), "no data arm toward the controller");
                helper.assertTrue(requireNetwork(helper, A) != requireNetwork(helper, B), "an off side between ducts splits the network");
                helper.assertTrue(east.mode(Line.DATA, Direction.EAST) == SideMode.DISABLED, "the side stays off");
                helper.succeed();
            });
        });
    }

    /** The click summary counts ducts, producers and stock members. */
    @ServusGameTest(timeoutTicks = 40)
    public static void dataCableSummaryCounts(GameTestHelper helper) {
        sticksStation(helper, WEST_END);
        controller(helper, EAST_END);
        dataRow(helper);
        helper.runAfterDelay(2, () -> {
            Component summary = DataSummary.of(requireNetwork(helper, B));
            helper.assertTrue(summary.getContents() instanceof TranslatableContents contents
                            && contents.getKey().equals(DataSummary.KEY)
                            && Arrays.equals(contents.getArgs(), new Object[] {3, 1, 1}),
                    "summary should say 3 cables, 1 producer, 1 stock, got " + summary.getString());
            helper.succeed();
        });
    }

    /** A data duct linking a controller and a sticks station moves nothing on its own; the planks in a rack beside stay. */
    @ServusGameTest(timeoutTicks = 200)
    public static void dataCableMovesNothing(GameTestHelper helper) {
        BlockPos rackPos = new BlockPos(7, 1, 3);
        helper.setBlock(rackPos, ServusStorageBlocks.HEAVY_RACK.get().defaultBlockState().setValue(HeavyRackBlock.FACING, Direction.EAST));
        MiniWarehouse.stock(helper, rackPos, new ItemStack(Items.OAK_PLANKS, 32));
        HeavyRackBlockEntity planks = helper.getBlockEntity(rackPos, HeavyRackBlockEntity.class);
        controller(helper, EAST_END);
        CraftingStationBlockEntity station = sticksStation(helper, WEST_END);
        dataRow(helper);
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(MiniWarehouse.count(planks, Items.OAK_PLANKS) == 32, "the planks should stay in the rack, got "
                    + MiniWarehouse.count(planks, Items.OAK_PLANKS));
            for (int slot = 0; slot < station.inputs().getSlots(); slot++) {
                helper.assertTrue(station.inputs().getStackInSlot(slot).isEmpty(), "the station inputs should stay empty");
            }
            helper.succeed();
        });
    }

    /** The query finds the network from a member position and nothing at a stone block. */
    @ServusGameTest(timeoutTicks = 40)
    public static void dataNetworksAtMemberAndStone(GameTestHelper helper) {
        controller(helper, EAST_END);
        dataRow(helper);
        helper.setBlock(ABOVE_B.above(), Blocks.STONE.defaultBlockState());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(network(helper, EAST_END).orElse(null) == requireNetwork(helper, A), "the controller should find the duct network");
            helper.assertTrue(network(helper, ABOVE_B.above()).isEmpty(), "stone has no network");
            helper.succeed();
        });
    }

    /** A sneak click with a Data Cable on the top of a straight data run, east of centre, places past the east end. */
    @ServusGameTest(timeoutTicks = 40)
    public static void dataCableSneakExtendsRun(GameTestHelper helper) {
        dataRow(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        BlockPos middle = helper.absolutePos(B);
        ItemStack stack = new ItemStack(ServusLogisticsItems.DATA_CABLE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(new Vec3(middle.getX() + 0.7, middle.getY() + 0.625, middle.getZ() + 0.5), Direction.UP, middle, false)));
        helper.assertBlockPresent(ServusLogisticsBlocks.UTILITY_DUCT.get(), EAST_END);
        UtilityDuctBlockEntity placed = helper.getBlockEntity(EAST_END, UtilityDuctBlockEntity.class);
        helper.assertTrue(placed.has(Line.DATA), "the new duct should carry the data line");
        helper.assertBlockNotPresent(ServusLogisticsBlocks.UTILITY_DUCT.get(), ABOVE_B);
        helper.succeed();
    }

    /** A broken duct with the Item Pipe and the data line drops one of each. */
    @ServusGameTest(timeoutTicks = 40)
    public static void dataCableDropsFromBrokenDuct(GameTestHelper helper) {
        duct(helper, B, Line.ITEM, Line.DATA);
        helper.getLevel().destroyBlock(helper.absolutePos(B), true);
        helper.runAfterDelay(1, () -> {
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(B)).inflate(2));
            boolean pipe = drops.stream().anyMatch(drop -> drop.getItem().is(ServusLogisticsItems.ITEM_PIPE.get()));
            boolean cable = drops.stream().anyMatch(drop -> drop.getItem().is(ServusLogisticsItems.DATA_CABLE.get()));
            helper.assertTrue(pipe && cable, "one Item Pipe and one Data Cable should drop, got " + drops);
            helper.succeed();
        });
    }
}
