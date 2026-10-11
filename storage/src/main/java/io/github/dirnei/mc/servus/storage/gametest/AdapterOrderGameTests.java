package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.adapter.AdapterContent;
import io.github.dirnei.mc.servus.foundation.adapter.MachineAdapterBlock;
import io.github.dirnei.mc.servus.foundation.adapter.MachineAdapterBlockEntity;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.cpu.CpuShape;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.order.OrderStep;
import io.github.dirnei.mc.servus.storage.order.WorkOrder;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/** A Machine Adapter on a vanilla furnace as a producer of the CPU, with its fuel refilled from the warehouse. */
public final class AdapterOrderGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);
    private static final BlockPos COBBLE_CRATE = new BlockPos(1, 1, 2);
    private static final BlockPos COAL_CRATE = new BlockPos(2, 1, 1);
    private static final BlockPos ACCESS = new BlockPos(2, 1, 3);
    private static final BlockPos ADAPTER = new BlockPos(4, 1, 3);
    private static final BlockPos FURNACE = new BlockPos(5, 1, 3);
    private static final BlockPos CPU = new BlockPos(6, 1, 1);
    private static final BlockPos LINE = new BlockPos(4, 2, 3);
    private static final ItemKey STONE = ItemKey.of(new ItemStack(Items.STONE));

    private AdapterOrderGameTests() {
    }

    /** Warehouse with cobblestone and coal, Packing Station, an adapter on a furnace and a CPU on one data line. */
    private static void setup(GameTestHelper helper) {
        setup(helper, new ItemStack(Items.COBBLESTONE, 16));
    }

    /** The same line with {@code material} instead of cobblestone in the first crate. */
    private static void setup(GameTestHelper helper, ItemStack material) {
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        for (BlockPos pos : List.of(COBBLE_CRATE, COAL_CRATE)) {
            helper.setBlock(pos, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        }
        CrateBlockEntity cobble = helper.getBlockEntity(COBBLE_CRATE, CrateBlockEntity.class);
        cobble.insert(material, false);
        CrateBlockEntity coal = helper.getBlockEntity(COAL_CRATE, CrateBlockEntity.class);
        coal.insert(new ItemStack(Items.COAL, 32), false);
        TerminalWiring.place(helper, ACCESS);
        helper.setBlock(FURNACE, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.SOUTH));
        helper.setBlock(ADAPTER, AdapterContent.BLOCK.get().defaultBlockState().setValue(MachineAdapterBlock.FACING, Direction.EAST));
        for (BlockPos pos : List.of(new BlockPos(3, 2, 3), LINE, new BlockPos(5, 2, 3))) {
            TerminalWiring.dataDuct(helper, pos);
        }
        for (int part = 0; part < CpuShape.PARTS; part++) {
            CpuShape.Vec offset = CpuShape.offset(part);
            helper.setBlock(CPU.offset(offset.x(), offset.y(), offset.z()), ServusStorageBlocks.CRAFTING_CPU.get().defaultBlockState());
        }
        helper.assertTrue(ServusStorageBlocks.CRAFTING_CPU.get().tryForm(helper.getLevel(), helper.absolutePos(CPU)), "the cube should form");
    }

    private static boolean ready(GameTestHelper helper) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(LINE))
                .map(network -> network.dataMembers(DataRole.PRODUCER).size() == 1
                        && CpuOrders.cpus(helper.getLevel(), network).size() == 1)
                .orElse(false);
    }

    private static DataNetwork line(GameTestHelper helper) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(LINE)).orElseThrow();
    }

    private static AbstractFurnaceBlockEntity furnace(GameTestHelper helper) {
        return helper.getBlockEntity(FURNACE, AbstractFurnaceBlockEntity.class);
    }

    /** The adapter asks the warehouse for its coal until the furnace holds the profile's level of 16. */
    @ServusGameTest(timeoutTicks = 600)
    public static void adapterUpkeepFromWarehouse(GameTestHelper helper) {
        setup(helper);
        MachineAdapterBlockEntity adapter = helper.getBlockEntity(ADAPTER, MachineAdapterBlockEntity.class);
        TerminalWiring.when(helper, () -> furnace(helper).getItem(1).is(Items.COAL) && furnace(helper).getItem(1).getCount() == 16, 500,
                () -> "the furnace should get 16 coal, has " + furnace(helper).getItem(1) + ", expected " + adapter.expected(), () -> {
            helper.assertTrue(adapter.upkeepItem().is(Items.COAL) && adapter.upkeepLevel() == 16, "coal at 16 from the profile");
            helper.succeed();
        });
    }

    /** An order for stone runs on the adapter: cobblestone in, stone back, no coal in the step. */
    @ServusGameTest(timeoutTicks = 2400)
    public static void adapterSmeltsAnOrder(GameTestHelper helper) {
        setup(helper);
        TerminalWiring.when(helper, () -> ready(helper), 60, () -> "the adapter and the CPU should join the line", () -> {
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), STONE, 4, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            WorkOrder order = placed.created().order();
            OrderStep step = order.steps().get(0);
            helper.assertTrue(step.producer().equals(helper.absolutePos(ADAPTER)), "the step runs on the adapter, got " + step.producer());
            helper.assertTrue(step.inputs().size() == 1 && step.inputs().containsKey(ItemKey.of(new ItemStack(Items.COBBLESTONE))),
                    "the step needs cobblestone only, got " + step.inputs());
            TerminalWiring.when(helper, () -> order.state() == WorkOrder.State.DONE, 2300,
                    () -> "the order should finish, state " + order.state() + " " + order.reason() + ", step " + step.state()
                            + " " + step.reason() + ", fed " + step.fed(ItemKey.of(new ItemStack(Items.COBBLESTONE)))
                            + ", collected " + step.collected(STONE) + ", furnace " + furnace(helper).getItem(0) + " / "
                            + furnace(helper).getItem(1) + " / " + furnace(helper).getItem(2), helper::succeed);
        });
    }

    private static final BlockPos SECOND_CRATE = new BlockPos(3, 1, 2);
    private static final ItemKey CHARCOAL = ItemKey.of(new ItemStack(Items.CHARCOAL));

    /** Charcoal with only birch logs in stock runs on birch, not on the first log the furnace knows. */
    @ServusGameTest(timeoutTicks = 2400)
    public static void charcoalFromBirchOnly(GameTestHelper helper) {
        setup(helper, new ItemStack(Items.BIRCH_LOG, 8));
        TerminalWiring.when(helper, () -> ready(helper) && CpuOrders.preview(helper.getLevel(), line(helper), CHARCOAL, 4).missing().isEmpty(),
                100, () -> "the line and the indexed birch logs should be ready", () -> {
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), line(helper), CHARCOAL, 4, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            WorkOrder order = placed.created().order();
            helper.assertTrue(order.steps().size() == 1, "one step, got " + order.steps().size());
            OrderStep step = order.steps().get(0);
            helper.assertTrue(step.inputs().keySet().equals(java.util.Set.of(ItemKey.of(new ItemStack(Items.BIRCH_LOG)))),
                    "the step should smelt birch logs, got " + step.inputs());
            TerminalWiring.when(helper, () -> order.state() == WorkOrder.State.DONE, 2300,
                    () -> "the order should finish, state " + order.state() + " " + order.reason() + ", step " + step.state()
                            + " " + step.reason() + ", collected " + step.collected(CHARCOAL), helper::succeed);
        });
    }

    /** With 6 oak and 4 birch logs the preview for 10 charcoal takes both from stock and misses nothing. */
    @ServusGameTest(timeoutTicks = 200)
    public static void charcoalPreviewSplitsLogs(GameTestHelper helper) {
        setup(helper, new ItemStack(Items.OAK_LOG, 6));
        helper.setBlock(SECOND_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity birch = helper.getBlockEntity(SECOND_CRATE, CrateBlockEntity.class);
        birch.insert(new ItemStack(Items.BIRCH_LOG, 4), false);
        TerminalWiring.when(helper, () -> ready(helper), 60, () -> "the adapter and the CPU should join the line", () -> {
            TerminalWiring.when(helper, () -> CpuOrders.preview(helper.getLevel(), line(helper), CHARCOAL, 10).missing().isEmpty(), 100,
                    () -> "nothing should be missing, got " + CpuOrders.preview(helper.getLevel(), line(helper), CHARCOAL, 10), () -> {
                var preview = CpuOrders.preview(helper.getLevel(), line(helper), CHARCOAL, 10);
                helper.assertTrue(preview.fromStock().getOrDefault(ItemKey.of(new ItemStack(Items.OAK_LOG)), 0) == 6
                                && preview.fromStock().getOrDefault(ItemKey.of(new ItemStack(Items.BIRCH_LOG)), 0) == 4,
                        "6 oak and 4 birch logs from stock, got " + preview.fromStock());
                helper.assertTrue(preview.toCraft().getOrDefault(CHARCOAL, 0) == 10, "10 charcoal to craft, got " + preview.toCraft());
                helper.succeed();
            });
        });
    }
}
