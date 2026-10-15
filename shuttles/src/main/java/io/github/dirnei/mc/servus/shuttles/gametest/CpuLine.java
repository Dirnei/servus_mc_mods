package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuOrders;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.order.WorkOrder;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternStorageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The CPU test line: the warehouse is a controller at (2,1,2) and a Packing
 * Station at (2,1,3) on rails at y 0, two racks west of those rails and a
 * garage east of them, a data duct on the Packing Station and the Storage
 * Terminal on top. Data ducts run along y 2 from the Packing Station east to a
 * Crafting Station at (4,1,2) and on to the CPU at x 6 to 8, z 1 to 3.
 */
final class CpuLine {

    static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);

    static final BlockPos ACCESS = new BlockPos(2, 1, 3);

    /** Two racks under the controller's row, facing the rails at y 0 that link the controller and the Packing Station. */
    static final BlockPos RACK_A = new BlockPos(1, 0, 2);

    static final BlockPos RACK_B = new BlockPos(1, 0, 3);

    /** The garage east of the rail under the Packing Station, with one shuttle. */
    static final BlockPos GARAGE = new BlockPos(3, 0, 3);

    static final BlockPos STATION = new BlockPos(4, 1, 2);

    static final BlockPos PATTERNS = new BlockPos(4, 3, 2);

    static final BlockPos CPU_A = new BlockPos(6, 1, 1);
    /** The data ducts from the Packing Station to the Crafting Station and on toward the CPU. */
    static final List<BlockPos> DUCTS = List.of(new BlockPos(3, 2, 3), new BlockPos(4, 2, 3), new BlockPos(4, 2, 2), new BlockPos(5, 2, 2));

    static final ItemKey STICK = ItemKey.of(new ItemStack(Items.STICK));

    static final ItemKey PLANKS = ItemKey.of(new ItemStack(Items.OAK_PLANKS));

    static final ItemKey CRAFTING_TABLE = ItemKey.of(new ItemStack(Items.CRAFTING_TABLE));

    private CpuLine() {
    }

    static CpuBlockEntity cpu(GameTestHelper helper, BlockPos origin) {
        return CpuBuild.cpu(helper, origin);
    }

    /** A plain station and a Pattern Storage with the sticks pattern on the line. */
    static CraftingStationBlockEntity sticksStation(GameTestHelper helper) {
        StationJobs.craftingStation(helper, STATION);
        PatternStorageBlockEntity patterns = StationJobs.patternStorage(helper, PATTERNS);
        patterns.set(0, StationJobs.sticks());
        return helper.getBlockEntity(STATION, CraftingStationBlockEntity.class);
    }

    /** Warehouse, terminal, station and the ducts to the first CPU's spot; the CPU itself when {@code withCpu}. */
    static void setup(GameTestHelper helper, boolean withCpu) {
        WarehouseFixtures.controller(helper, CONTROLLER);
        for (BlockPos rail : List.of(CONTROLLER.below(), ACCESS.below())) {
            WarehouseFixtures.rail(helper, rail);
        }
        for (BlockPos pos : List.of(RACK_A, RACK_B)) {
            WarehouseFixtures.rack(helper, pos, Direction.EAST);
        }
        WarehouseFixtures.garage(helper, GARAGE, Direction.EAST, 1);
        TerminalWiring.place(helper, ACCESS);
        sticksStation(helper);
        for (BlockPos pos : DUCTS) {
            TerminalWiring.dataDuct(helper, pos);
        }
        if (withCpu) {
            CpuBuild.formCube(helper, CPU_A);
        }
    }

    /**
     * The CPU test line with a 2x2x2 CPU of {@code 8 - bufferBlocks} CPU blocks and
     * {@code bufferBlocks} Buffer blocks, the planks and crafting table patterns
     * next to the sticks pattern and {@code logs} oak logs in the first rack.
     */
    static CpuBlockEntity bufferedSetup(GameTestHelper helper, int bufferBlocks, int logs) {
        setup(helper, false);
        CpuBuild.box(helper, CPU_A, 2, 2, 2, 8 - bufferBlocks);
        CpuBlockEntity cpu = CpuBuild.formed(helper, CPU_A);
        PatternStorageBlockEntity patterns = helper.getBlockEntity(PATTERNS, PatternStorageBlockEntity.class);
        patterns.set(1, StationJobs.planks());
        patterns.set(2, StationJobs.craftingTable());
        stock(helper, RACK_A, Items.OAK_LOG, logs);
        return cpu;
    }

    /** Whether the line has the station and the CPU, so orders can be planned. */
    static boolean lineReady(GameTestHelper helper) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 2)))
                .map(network -> !network.dataMembers(DataRole.PRODUCER).isEmpty()
                        && CpuOrders.cpus(helper.getLevel(), network).size() == 1)
                .orElse(false);
    }

    /** The most of the key the warehouse and its Packing Station's buffers held at any tick so far, in {@code seen[0]}. */
    static int[] watchStorage(GameTestHelper helper, Item item) {
        int[] seen = {0};
        ItemKey key = ItemKey.of(new ItemStack(item));
        helper.onEachTick(() -> {
            int count = 0;
            if (helper.getBlockEntity(CONTROLLER, BlockEntity.class) instanceof WarehouseControllerBlockEntity controller) {
                count += controller.network().map(network -> (int) network.available(key)).orElse(0);
            }
            if (helper.getBlockEntity(ACCESS, BlockEntity.class) instanceof PackingStationBlockEntity point) {
                count += WarehouseFixtures.count(point.buffer(), item) + WarehouseFixtures.count(point.outgoing(), item);
            }
            seen[0] = Math.max(seen[0], count);
        });
        return seen;
    }

    /** Runs {@code then} once, on the first tick the order holds the key in its CPU's buffer. */
    static void whenBuffered(GameTestHelper helper, CpuBlockEntity cpu, WorkOrder order, ItemKey key, Runnable then) {
        // Polled every tick; onEachTick cannot be added while the test ticks.
        Waits.when(helper, () -> cpu.buffer().held(order.id(), key) > 0, 1200,
                () -> "the order should hold " + key.stack(1).getHoverName().getString() + " in the buffer, order "
                        + order.state() + " " + order.steps().stream().map(step -> step.state() + "/" + step.reason()).toList()
                        + " buffer " + cpu.buffer(), then);
    }

    static WarehouseNetwork warehouse(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = helper.getBlockEntity(CONTROLLER, WarehouseControllerBlockEntity.class);
        return controller.network().orElseThrow();
    }

    static DataNetwork line(GameTestHelper helper) {
        return DataLines.at(helper, new BlockPos(4, 2, 2));
    }

    static HeavyRackBlockEntity stock(GameTestHelper helper, BlockPos pos, Item item, int count) {
        if (count > 0) {
            WarehouseFixtures.stock(helper, pos, new ItemStack(item, count));
        }
        return helper.getBlockEntity(pos, HeavyRackBlockEntity.class);
    }
}
