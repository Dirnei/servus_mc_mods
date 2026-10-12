package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlock;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlock;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlock;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Only blocks meant for pipes expose the item capability: the Packing Station,
 * Storage Terminal, Field Head and Crafting Station offer none, while the
 * Inbound and Outbound Station offer theirs on every side that is not Off.
 */
public final class PipeAccessGameTests {

    private PipeAccessGameTests() {
    }

    private static boolean itemCapability(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(pos), side) != null;
    }

    /** A Utility Duct carrying only the item line, its arms computed from the neighbours. */
    private static UtilityDuctBlockEntity itemDuct(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos, UtilityDuctBlockEntity.class);
        duct.addLine(Line.ITEM);
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
        duct.recomputeArms();
        return duct;
    }

    private static final BlockPos PACKING = new BlockPos(2, 1, 2);
    private static final BlockPos TERMINAL = new BlockPos(4, 1, 2);
    private static final BlockPos HEAD = new BlockPos(6, 1, 2);
    private static final BlockPos STATION = new BlockPos(8, 1, 2);

    private static void stations(GameTestHelper helper) {
        helper.setBlock(PACKING, ServusStorageBlocks.PACKING_STATION.get().defaultBlockState());
        helper.setBlock(TERMINAL, ServusStorageBlocks.STORAGE_TERMINAL.get().defaultBlockState().setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        helper.setBlock(HEAD, ServusStorageBlocks.FIELD_HEAD.get().defaultBlockState());
        helper.setBlock(STATION, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
    }

    /** No face of the Packing Station, terminal, Field Head or Crafting Station offers items. */
    @ServusGameTest(timeoutTicks = 20)
    public static void noItemCapabilityOnStations(GameTestHelper helper) {
        stations(helper);
        for (BlockPos pos : List.of(PACKING, TERMINAL, HEAD, STATION)) {
            helper.assertTrue(!itemCapability(helper, pos, null), "no item capability without a side at " + pos);
            for (Direction side : Direction.values()) {
                helper.assertTrue(!itemCapability(helper, pos, side), "no item capability on " + side + " at " + pos);
            }
        }
        helper.succeed();
    }

    /** A hopper above a Storage Terminal keeps its bread. */
    @ServusGameTest(timeoutTicks = 100)
    public static void hopperCannotFeedTerminal(GameTestHelper helper) {
        helper.setBlock(TERMINAL, ServusStorageBlocks.STORAGE_TERMINAL.get().defaultBlockState().setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        helper.setBlock(TERMINAL.above(), Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopper = helper.getBlockEntity(TERMINAL.above(), HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.BREAD, 5));
        helper.runAfterDelay(40, () -> {
            StorageTerminalBlockEntity terminal = helper.getBlockEntity(TERMINAL, StorageTerminalBlockEntity.class);
            helper.assertTrue(hopper.getItem(0).getCount() == 5, "the hopper keeps its bread");
            helper.assertTrue(terminal.input().getStackInSlot(0).isEmpty() && terminal.outputCount(Items.BREAD) == 0, "nothing reached the terminal");
            helper.succeed();
        });
    }

    /** A hopper above a Packing Station keeps its bread. */
    @ServusGameTest(timeoutTicks = 100)
    public static void hopperCannotFeedPackingStation(GameTestHelper helper) {
        helper.setBlock(PACKING, ServusStorageBlocks.PACKING_STATION.get().defaultBlockState());
        helper.setBlock(PACKING.above(), Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopper = helper.getBlockEntity(PACKING.above(), HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.BREAD, 5));
        helper.runAfterDelay(40, () -> {
            PackingStationBlockEntity point = helper.getBlockEntity(PACKING, PackingStationBlockEntity.class);
            helper.assertTrue(hopper.getItem(0).getCount() == 5, "the hopper keeps its bread");
            helper.assertTrue(point.isEmpty(), "nothing reached the Packing Station");
            helper.succeed();
        });
    }

    /** A duct with the Item Pipe between those blocks shows no item arm toward any of them. */
    @ServusGameTest(timeoutTicks = 20)
    public static void itemPipeShowsNoArmToStations(GameTestHelper helper) {
        BlockPos centre = new BlockPos(6, 2, 6);
        helper.setBlock(centre.west(), ServusStorageBlocks.PACKING_STATION.get().defaultBlockState());
        helper.setBlock(centre.east(), ServusStorageBlocks.STORAGE_TERMINAL.get().defaultBlockState().setValue(StorageTerminalBlock.FACING, Direction.EAST));
        helper.setBlock(centre.north(), ServusStorageBlocks.FIELD_HEAD.get().defaultBlockState());
        helper.setBlock(centre.south(), ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.SOUTH));
        helper.setBlock(centre.above(), ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState());
        UtilityDuctBlockEntity duct = itemDuct(helper, centre);
        helper.assertTrue(duct.arms(Line.ITEM) == 0, "no item arm toward the stations, got mask " + duct.arms(Line.ITEM));
        helper.succeed();
    }

    private static final BlockPos FEED_CHEST = new BlockPos(3, 1, 6);
    private static final BlockPos FEED_DUCT = new BlockPos(3, 1, 7);
    private static final BlockPos FEED_STATION = new BlockPos(3, 1, 8);

    /** A duct shows an arm toward an Inbound Station and feeds it straight from a chest. */
    @ServusGameTest(timeoutTicks = 200)
    public static void ductFeedsInboundStation(GameTestHelper helper) {
        helper.setBlock(FEED_CHEST, Blocks.CHEST.defaultBlockState());
        helper.getBlockEntity(FEED_CHEST, ChestBlockEntity.class).setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        // Facing south: the duct touches the station's back, an Input side.
        helper.setBlock(FEED_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState().setValue(InboundStationBlock.FACING, Direction.SOUTH));
        UtilityDuctBlockEntity duct = itemDuct(helper, FEED_DUCT);
        duct.setMode(Line.ITEM, Direction.NORTH, SideMode.INPUT);
        helper.assertTrue(duct.hasArm(Line.ITEM, Direction.SOUTH), "the duct shows an arm toward the station");
        InboundStationBlockEntity station = helper.getBlockEntity(FEED_STATION, InboundStationBlockEntity.class);
        TerminalWiring.when(helper, () -> station.buffer().getStackInSlot(0).getCount() == 16, 180,
                () -> "the cobblestone should reach the station's buffer, has " + station.buffer().getStackInSlot(0), helper::succeed);
    }

    /** An Inbound Station side set to Off offers nothing; Input and Pull sides offer the intake; the front is always off. */
    @ServusGameTest(timeoutTicks = 20)
    public static void inboundOffSideHasNoCapability(GameTestHelper helper) {
        helper.setBlock(FEED_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState().setValue(InboundStationBlock.FACING, Direction.NORTH));
        InboundStationBlockEntity station = helper.getBlockEntity(FEED_STATION, InboundStationBlockEntity.class);
        station.sideConfig().set(RelativeSide.TOP, SideMode.DISABLED);
        station.sideConfig().set(station.relativeSide(Direction.SOUTH), SideMode.PULL);
        helper.getLevel().invalidateCapabilities(helper.absolutePos(FEED_STATION));
        helper.assertTrue(!itemCapability(helper, FEED_STATION, Direction.UP), "an Off side offers nothing");
        helper.assertTrue(!itemCapability(helper, FEED_STATION, Direction.NORTH), "the front offers nothing");
        helper.assertTrue(itemCapability(helper, FEED_STATION, Direction.SOUTH), "a Pull side offers the intake");
        helper.assertTrue(itemCapability(helper, FEED_STATION, Direction.EAST), "an Input side offers the intake");
        helper.succeed();
    }

    /** A Pull side on the back empties the chest behind the station. */
    @ServusGameTest(timeoutTicks = 100)
    public static void inboundPullsFromChestOnBack(GameTestHelper helper) {
        helper.setBlock(FEED_STATION, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState().setValue(InboundStationBlock.FACING, Direction.NORTH));
        InboundStationBlockEntity station = helper.getBlockEntity(FEED_STATION, InboundStationBlockEntity.class);
        helper.setBlock(FEED_STATION.south(), Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(FEED_STATION.south(), ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.BREAD, 20));
        station.sideConfig().set(RelativeSide.BACK, SideMode.PULL);
        TerminalWiring.when(helper, () -> chest.countItem(Items.BREAD) == 0 && station.buffer().getStackInSlot(0).getCount() == 20, 80,
                () -> "the station should pull the bread, chest has " + chest.countItem(Items.BREAD), helper::succeed);
    }

    private static final BlockPos OUT_STATION = new BlockPos(7, 1, 6);
    private static final BlockPos OUT_DUCT = new BlockPos(7, 1, 7);
    private static final BlockPos OUT_CHEST = new BlockPos(7, 1, 8);

    /** An Outbound Station pushes its buffer into a duct, which shows an arm toward it and carries the items on. */
    @ServusGameTest(timeoutTicks = 200)
    public static void outboundPushesIntoDuct(GameTestHelper helper) {
        // Facing north: the back, a Push side, touches the duct.
        helper.setBlock(OUT_STATION, ServusStorageBlocks.OUTBOUND_STATION.get().defaultBlockState().setValue(OutboundStationBlock.FACING, Direction.NORTH));
        helper.setBlock(OUT_CHEST, Blocks.CHEST.defaultBlockState());
        UtilityDuctBlockEntity duct = itemDuct(helper, OUT_DUCT);
        helper.assertTrue(duct.hasArm(Line.ITEM, Direction.NORTH), "the duct shows an arm toward the station's Push side");
        OutboundStationBlockEntity station = helper.getBlockEntity(OUT_STATION, OutboundStationBlockEntity.class);
        station.buffer().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 8));
        ChestBlockEntity chest = helper.getBlockEntity(OUT_CHEST, ChestBlockEntity.class);
        TerminalWiring.when(helper, () -> chest.countItem(Items.IRON_INGOT) == 8 && station.isEmpty(), 180,
                () -> "the ingots should go through the duct into the chest, chest has " + chest.countItem(Items.IRON_INGOT), helper::succeed);
    }

    /** An Outbound Station's Off sides offer nothing; Push and Output sides offer the buffer. */
    @ServusGameTest(timeoutTicks = 20)
    public static void outboundOffSideHasNoCapability(GameTestHelper helper) {
        helper.setBlock(OUT_STATION, ServusStorageBlocks.OUTBOUND_STATION.get().defaultBlockState().setValue(OutboundStationBlock.FACING, Direction.WEST));
        OutboundStationBlockEntity station = helper.getBlockEntity(OUT_STATION, OutboundStationBlockEntity.class);
        helper.assertTrue(!itemCapability(helper, OUT_STATION, Direction.UP), "the top starts off and offers nothing");
        helper.assertTrue(!itemCapability(helper, OUT_STATION, Direction.WEST), "the front offers nothing");
        helper.assertTrue(itemCapability(helper, OUT_STATION, Direction.EAST), "the back, a Push side, offers the buffer");
        station.sideConfig().set(RelativeSide.TOP, SideMode.OUTPUT);
        helper.getLevel().invalidateCapabilities(helper.absolutePos(OUT_STATION));
        helper.assertTrue(itemCapability(helper, OUT_STATION, Direction.UP), "an Output side offers the buffer");
        helper.succeed();
    }

    private static final BlockPos CRAFT = new BlockPos(5, 1, 10);

    /** A Crafting Station pulls planks from a chest on a Pull side and pushes its sticks into a chest once the job ends. */
    @ServusGameTest(timeoutTicks = 300)
    public static void craftingStationPullsAndPushes(GameTestHelper helper) {
        helper.setBlock(CRAFT, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
        CraftingStationBlockEntity station = helper.getBlockEntity(CRAFT, CraftingStationBlockEntity.class);
        helper.setBlock(CRAFT.west(), Blocks.CHEST.defaultBlockState());
        ChestBlockEntity source = helper.getBlockEntity(CRAFT.west(), ChestBlockEntity.class);
        source.setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
        helper.setBlock(CRAFT.east(), Blocks.CHEST.defaultBlockState());
        ChestBlockEntity target = helper.getBlockEntity(CRAFT.east(), ChestBlockEntity.class);
        station.sideConfig().set(station.relativeSide(Direction.WEST), SideMode.PULL);
        helper.assertTrue(station.sideMode(Direction.EAST) == SideMode.PUSH, "sides start as Push");
        Object owner = StationJobs.sticksJob(station, 1);
        TerminalWiring.when(helper, () -> source.countItem(Items.OAK_PLANKS) == 0 && !station.working(), station.workTicks() + 120,
                () -> "the station should pull both planks and craft, chest has " + source.countItem(Items.OAK_PLANKS), () -> {
            helper.assertTrue(target.countItem(Items.STICK) == 0, "a held job keeps its sticks");
            station.release(owner);
            TerminalWiring.when(helper, () -> target.countItem(Items.STICK) == 4, 60,
                    () -> "the sticks should be pushed into the east chest, it has " + target.countItem(Items.STICK), helper::succeed);
        });
    }
}
