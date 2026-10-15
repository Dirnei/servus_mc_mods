package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.adapter.AdapterContent;
import io.github.dirnei.mc.servus.foundation.adapter.MachineAdapterBlock;
import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.data.packet.Delivery;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuOrders;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.foundation.data.packet.PacketInlets;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.order.WorkOrder;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/**
 * Data line packets reach Servus blocks through their packet inlet and
 * foreign machines through their item capability. A mini warehouse with its
 * terminal sends; data ducts run east of the terminal's duct at y 2 over a
 * Crafting Station, a Field Head (which takes no packets) and a Machine
 * Adapter on a furnace.
 */
public final class PacketInletGameTests {

    private static final BlockPos ORIGIN = new BlockPos(2, 1, 2);
    private static final BlockPos STATION = new BlockPos(4, 1, 3);
    private static final BlockPos HEAD = new BlockPos(5, 1, 3);
    private static final BlockPos ADAPTER = new BlockPos(6, 1, 3);
    private static final BlockPos FURNACE = new BlockPos(7, 1, 3);

    private PacketInletGameTests() {
    }

    /** The mini warehouse with its terminal and three more data ducts east of the terminal's duct. */
    private static MiniWarehouse line(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).withTerminal();
        for (int x = 4; x <= 6; x++) {
            TerminalWiring.dataDuct(helper, new BlockPos(x, 2, 3));
        }
        return warehouse;
    }

    private static Delivery send(GameTestHelper helper, BlockPos from, BlockPos to, ItemStack stack) {
        return DataPackets.send(helper.getLevel(), helper.absolutePos(from), helper.absolutePos(to), stack)
                .orElseThrow(() -> new AssertionError("the send should start"));
    }

    /** Packets into a Packing Station land in its To storage buffer and a shuttle puts them into the rack. */
    @ServusGameTest(timeoutTicks = 400)
    public static void packetsIntoPackingStation(GameTestHelper helper) {
        MiniWarehouse warehouse = line(helper);
        helper.runAfterDelay(10, () -> {
            Delivery delivery = send(helper, warehouse.terminalPos(), warehouse.packingPos(), new ItemStack(Items.COBBLESTONE, 16));
            Waits.when(helper, () -> warehouse.rackCount(Items.COBBLESTONE) == 16, 350,
                    () -> "a shuttle should put the 16 cobblestone away, rack has " + warehouse.rackCount(Items.COBBLESTONE) + ", " + delivery, () -> {
                helper.assertTrue(helper.getBlockEntity(warehouse.packingPos(), PackingStationBlockEntity.class).isEmpty(), "the To storage buffer is empty again");
                helper.succeed();
            });
        });
    }

    /** Packets of an item a request waits for land in the terminal's Output row. */
    @ServusGameTest(timeoutTicks = 200)
    public static void packetsIntoTerminalOutput(GameTestHelper helper) {
        MiniWarehouse warehouse = line(helper);
        StationJobs.craftingStation(helper, STATION);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = warehouse.terminal();
            terminal.newRequest(helper.getLevel(), ItemKey.of(new ItemStack(Items.STICK)), 4);
            send(helper, STATION, warehouse.terminalPos(), new ItemStack(Items.STICK, 4));
            Waits.when(helper, () -> terminal.outputCount(Items.STICK) == 4, 100,
                    () -> "the sticks should land in the output, it has " + terminal.outputCount(Items.STICK), helper::succeed);
        });
    }

    /** Packets no request waits for go into the terminal's Input row, which hands them on into storage. */
    @ServusGameTest(timeoutTicks = 400)
    public static void packetsIntoTerminalInput(GameTestHelper helper) {
        MiniWarehouse warehouse = line(helper);
        StationJobs.craftingStation(helper, STATION);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = warehouse.terminal();
            send(helper, STATION, warehouse.terminalPos(), new ItemStack(Items.BREAD, 5));
            Waits.when(helper, () -> warehouse.rackCount(Items.BREAD) == 5, 350,
                    () -> "the bread should go through the Input row into the rack, rack has " + warehouse.rackCount(Items.BREAD), () -> {
                helper.assertTrue(terminal.outputCount(Items.BREAD) == 0, "nothing landed in the output");
                helper.succeed();
            });
        });
    }

    /** Packets of planks reach a Crafting Station with a sticks job through its inlet. */
    @ServusGameTest(timeoutTicks = 100)
    public static void packetsFeedCraftingStation(GameTestHelper helper) {
        MiniWarehouse warehouse = line(helper);
        CraftingStationBlockEntity station = StationJobs.craftingStation(helper, STATION);
        StationJobs.sticksJob(station, 4);
        // Full outputs keep it from crafting, so the planks stay in the inputs.
        for (int slot = 0; slot < station.outputs().getSlots(); slot++) {
            station.outputs().setStackInSlot(slot, new ItemStack(Items.STICK, 64));
        }
        helper.runAfterDelay(10, () -> {
            send(helper, warehouse.terminalPos(), STATION, new ItemStack(Items.OAK_PLANKS, 6));
            Waits.when(helper, () -> station.inputs().getStackInSlot(0).getCount() == 6, 60,
                    () -> "the planks should reach the station, inputs " + station.inputs().getStackInSlot(0), helper::succeed);
        });
    }

    /** A Field Head under a data duct is not on the data line: it has no packet inlet and no upkeep. */
    @ServusGameTest(timeoutTicks = 40)
    public static void headIsNotOnTheDataLine(GameTestHelper helper) {
        line(helper);
        helper.setBlock(HEAD, ServusShuttlesBlocks.FIELD_HEAD.get().defaultBlockState());
        helper.runAfterDelay(5, () -> {
            BlockPos head = helper.absolutePos(HEAD);
            for (Direction side : Direction.values()) {
                helper.assertTrue(PacketInlets.handler(helper.getLevel(), head, side) == null, "the head has no packet inlet on " + side);
                helper.assertTrue(helper.getLevel().getCapability(ServusFoundationCapabilities.UPKEEP, head, side) == null,
                        "the head has no upkeep on " + side);
            }
            helper.succeed();
        });
    }

    /** A Machine Adapter on a furnace keeps taking packets through its item handler. */
    @ServusGameTest(timeoutTicks = 100)
    public static void adapterStillTakesPackets(GameTestHelper helper) {
        MiniWarehouse warehouse = line(helper);
        helper.setBlock(FURNACE, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.SOUTH));
        helper.setBlock(ADAPTER, AdapterContent.BLOCK.get().defaultBlockState().setValue(MachineAdapterBlock.FACING, Direction.EAST));
        AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(FURNACE, AbstractFurnaceBlockEntity.class);
        helper.runAfterDelay(10, () -> {
            send(helper, warehouse.terminalPos(), ADAPTER, new ItemStack(Items.COBBLESTONE, 4));
            Waits.when(helper, () -> furnace.getItem(0).is(Items.COBBLESTONE) && furnace.getItem(0).getCount() == 4, 60,
                    () -> "the cobblestone should reach the furnace, it has " + furnace.getItem(0), helper::succeed);
        });
    }

    /** A CPU order's sticks are collected from the station through its inlet and a shuttle puts them away. */
    @ServusGameTest(timeoutTicks = 1200)
    public static void cpuCollectsThroughInlet(GameTestHelper helper) {
        CpuLine.setup(helper, true);
        CpuLine.stock(helper, CpuLine.RACK_A, Items.OAK_PLANKS, 10);
        Waits.when(helper, () -> CpuOrders.cpus(helper.getLevel(), CpuLine.line(helper)).size() == 1, 60,
                () -> "the CPU should be on the line", () -> {
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), CpuLine.line(helper), CpuLine.STICK, 8, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            WorkOrder order = placed.created().order();
            Waits.when(helper, () -> order.state() == WorkOrder.State.DONE
                            && CpuLine.warehouse(helper).available(CpuLine.STICK) == 8, 1100,
                    () -> "the order should finish with 8 sticks in storage, state " + order.state() + ", stock "
                            + CpuLine.warehouse(helper).available(CpuLine.STICK), helper::succeed);
        });
    }
}
