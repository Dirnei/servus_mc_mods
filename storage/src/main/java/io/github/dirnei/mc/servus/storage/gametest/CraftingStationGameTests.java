package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.data.packet.PacketInlets;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlock;
import io.github.dirnei.mc.servus.storage.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlock;
import io.github.dirnei.mc.servus.storage.station.InboundStationBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/** The Crafting Station on its own and served by stations. */
public final class CraftingStationGameTests {

    private static final BlockPos CS_STATION = new BlockPos(9, 1, 2);
    private static final BlockPos CR_STATION = new BlockPos(9, 1, 9);
    private static final BlockPos DS_ORIGIN = new BlockPos(2, 1, 4);
    private static final BlockPos DS_STATION = new BlockPos(3, 1, 6);
    private static final BlockPos DS_OUTBOUND = new BlockPos(2, 1, 6);
    private static final BlockPos DS_INBOUND = new BlockPos(3, 1, 7);

    private CraftingStationGameTests() {
    }

    /** A Crafting Station side set to Off refuses a hopper; Pull takes used items from a chest. */
    @ServusGameTest(timeoutTicks = 200)
    public static void craftingStationSideOff(GameTestHelper helper) {
        CraftingStationBlockEntity station = craftingStationWithSticks(helper, CS_STATION);
        station.sideConfig().set(RelativeSide.TOP, SideMode.DISABLED);
        helper.setBlock(CS_STATION.above(), Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopper = helper.getBlockEntity(CS_STATION.above(), HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
        helper.setBlock(CS_STATION.west(), Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(CS_STATION.west(), ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 6));
        chest.setItem(1, new ItemStack(Items.COBBLESTONE, 2));
        station.sideConfig().set(station.relativeSide(Direction.WEST), SideMode.PULL);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(hopper.getItem(0).getCount() == 4, "the hopper above an off side keeps its planks");
            helper.assertTrue(chest.countItem(Items.OAK_PLANKS) == 0 && chest.countItem(Items.COBBLESTONE) == 2, "the pull side takes planks and leaves cobblestone");
            helper.succeed();
        });
    }

    private static CraftingStationBlockEntity plainStation(GameTestHelper helper, BlockPos pos) {
        CraftingStationBlockEntity station = StationJobs.craftingStation(helper, pos);
        // No warehouse on the station's line pays for its work here.
        PowerFixtures.freeStation(station);
        return station;
    }

    /** A station with a long sticks job, so it takes planks and crafts them. */
    private static CraftingStationBlockEntity craftingStationWithSticks(GameTestHelper helper, BlockPos pos) {
        CraftingStationBlockEntity station = plainStation(helper, pos);
        StationJobs.sticksJob(station, 64);
        return station;
    }

    /** A sticks job is crafted from the inputs, one craft per work time, and ends with no crafts left. */
    @ServusGameTest(timeoutTicks = 200)
    public static void craftingStationCraftsJob(GameTestHelper helper) {
        CraftingStationBlockEntity station = plainStation(helper, CR_STATION);
        station.inputs().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 6));
        Object owner = StationJobs.sticksJob(station, 2);
        helper.assertTrue(station.working() && station.jobLeft() == 2, "the job should have 2 crafts left");
        int ticks = station.workTicks();
        helper.runAfterDelay(ticks + 5, () -> {
            helper.assertTrue(station.outputs().getStackInSlot(0).is(Items.STICK) && station.outputs().getStackInSlot(0).getCount() == 4,
                    "four sticks should be in the outputs after one work time, got " + station.outputs().getStackInSlot(0));
            helper.assertTrue(station.inputs().getStackInSlot(0).getCount() == 4, "four planks should remain");
            helper.runAfterDelay(ticks * 2 + 4, () -> {
                helper.assertTrue(station.outputs().getStackInSlot(0).getCount() == 8, "eight sticks after the second craft and no third, got "
                        + station.outputs().getStackInSlot(0).getCount());
                helper.assertTrue(!station.working() && station.jobLeft() == 0, "no crafts left");
                helper.assertTrue(station.inputs().getStackInSlot(0).getCount() == 2, "the leftover planks stay");
                station.release(owner);
                helper.assertTrue(station.job().isEmpty(), "released, the station is idle");
                helper.succeed();
            });
        });
    }

    /** Without a job the station crafts nothing, reports no recipes and takes nothing through its packet inlet. */
    @ServusGameTest(timeoutTicks = 100)
    public static void craftingStationIdleWithoutJob(GameTestHelper helper) {
        CraftingStationBlockEntity station = plainStation(helper, CR_STATION);
        station.inputs().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 4));
        ProductionProvider provider = helper.getLevel().getCapability(ServusFoundationCapabilities.PRODUCTION, helper.absolutePos(CR_STATION), null);
        helper.assertTrue(provider != null && provider.recipes().isEmpty() && provider.active().isEmpty(), "no recipes and nothing active");
        ItemView handler = new ItemView(station.packetInlet());
        helper.assertTrue(handler.insertItem(new ItemStack(Items.OAK_PLANKS, 5), false).getCount() == 5, "planks are refused without a job");
        helper.runAfterDelay(station.workTicks() + 5, () -> {
            helper.assertTrue(station.outputs().getStackInSlot(0).isEmpty(), "nothing is crafted without a job");
            StationJobs.sticksJob(station, 1);
            ProductionProvider working = helper.getLevel().getCapability(ServusFoundationCapabilities.PRODUCTION, helper.absolutePos(CR_STATION), null);
            helper.assertTrue(working.recipes().isEmpty() && working.active().map(id -> id.equals(Identifier.withDefaultNamespace("stick"))).orElse(false),
                    "with a job the sticks recipe is active");
            helper.succeed();
        });
    }

    /** Packets can bring only used items into the inputs and take only from the outputs. */
    @ServusGameTest(timeoutTicks = 100)
    public static void craftingStationRefusesUnusedItems(GameTestHelper helper) {
        CraftingStationBlockEntity station = craftingStationWithSticks(helper, CR_STATION);
        ItemView handler = PacketInlets.handler(helper.getLevel(), helper.absolutePos(CR_STATION), Direction.UP);
        helper.assertTrue(handler != null, "the station should expose a packet inlet");
        helper.assertTrue(handler.insertItem(new ItemStack(Items.COBBLESTONE, 5), false).getCount() == 5, "cobblestone is refused");
        helper.assertTrue(handler.insertItem(new ItemStack(Items.OAK_PLANKS, 5), false).isEmpty(), "planks are accepted");
        helper.assertTrue(handler.extractItem(0, 5, false).isEmpty(), "inputs cannot be extracted");
        station.outputs().setStackInSlot(0, new ItemStack(Items.STICK, 3));
        helper.assertTrue(handler.extractItem(CraftingStationBlockEntity.INPUT_SLOTS, 3, false).getCount() == 3, "outputs can be extracted");
        helper.succeed();
    }

    /** The station pulls planks from an Outbound Station and pushes the sticks into an Inbound Station; a shuttle puts them into the rack. */
    @ServusGameTest(timeoutTicks = 600)
    public static void outboundServesCraftingStation(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, DS_ORIGIN);
        // Rails beside both stations: one south of the first rail, a run down the east side.
        for (BlockPos rail : List.of(new BlockPos(2, 1, 5), new BlockPos(4, 1, 5), new BlockPos(4, 1, 6), new BlockPos(4, 1, 7))) {
            WarehouseFixtures.railOnStone(helper, rail);
        }
        helper.setBlock(DS_OUTBOUND, ServusStorageBlocks.OUTBOUND_STATION.get().defaultBlockState().setValue(OutboundStationBlock.FACING, Direction.WEST));
        helper.setBlock(DS_INBOUND, ServusStorageBlocks.INBOUND_STATION.get().defaultBlockState().setValue(InboundStationBlock.FACING, Direction.SOUTH));
        CraftingStationBlockEntity station = plainStation(helper, DS_STATION);
        // The station has no item capability: it pulls the planks from the Outbound Station itself.
        station.sideConfig().set(station.relativeSide(Direction.WEST), SideMode.PULL);
        Object owner = StationJobs.sticksJob(station, 2);
        OutboundStationBlockEntity outbound = helper.getBlockEntity(DS_OUTBOUND, OutboundStationBlockEntity.class);
        InboundStationBlockEntity inbound = helper.getBlockEntity(DS_INBOUND, InboundStationBlockEntity.class);
        outbound.buffer().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 4));
        // A held job keeps its outputs for the CPU; the sticks leave once the job ends.
        Waits.when(helper, () -> !station.working(), station.workTicks() * 2 + 80, () -> "both crafts should be done", () -> {
            helper.assertTrue(station.outputs().getStackInSlot(0).getCount() == 8, "the sticks wait in the outputs while the job is held");
            station.release(owner);
            Waits.when(helper, () -> warehouse.rackCount(Items.STICK) == 8, MiniWarehouse.TRIP_TICKS * 3,
                    () -> "eight sticks should have flowed into the rack, found " + warehouse.rackCount(Items.STICK), () -> {
                helper.assertTrue(outbound.isEmpty() && inbound.isEmpty(), "both station buffers should be empty");
                helper.succeed();
            });
        });
    }
}
