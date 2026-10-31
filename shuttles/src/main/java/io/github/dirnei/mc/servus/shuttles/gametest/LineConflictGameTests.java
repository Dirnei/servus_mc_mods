package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.packing.WarehouseSupplier;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.terminal.LineWarehouse;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlock;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalWarehouses;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * A data line that reaches two warehouses is in conflict: the terminal shows
 * no stock and refuses, the line's Packing Stations send nothing, and each
 * warehouse keeps running its own racks.
 *
 * <pre>
 *   z 1: warehouse A (controller x 2, Packing Station x 3), duct x 4
 *   z 4: terminal at x 5 beside the duct
 *   z 8: warehouse B (controller x 2, Packing Station x 3), duct x 4
 * </pre>
 */
public final class LineConflictGameTests {

    private static final BlockPos CONTROLLER_A = new BlockPos(2, 1, 1);
    private static final BlockPos STATION_A = new BlockPos(3, 1, 1);
    private static final BlockPos CONTROLLER_B = new BlockPos(2, 1, 8);
    private static final BlockPos STATION_B = new BlockPos(3, 1, 8);
    private static final BlockPos TERMINAL = new BlockPos(5, 1, 4);
    /** The duct that joins the two halves of the line. */
    private static final BlockPos JOIN = new BlockPos(4, 1, 5);

    private LineConflictGameTests() {
    }

    /**
     * A controller at {@code controller} with rails at y 0 under it and the
     * block east of it, a Packing Station on that block, a rack west of the
     * first rail holding the items and a garage north of it with one shuttle.
     */
    private static HeavyRackBlockEntity warehouse(GameTestHelper helper, BlockPos controller, int cobblestone, boolean powered) {
        if (powered) {
            WarehouseFixtures.controller(helper, controller);
        } else {
            helper.setBlock(controller, ServusShuttlesBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState());
        }
        BlockPos rail = controller.below();
        WarehouseFixtures.rail(helper, rail);
        WarehouseFixtures.rail(helper, rail.east());
        WarehouseFixtures.rack(helper, rail.west(), Direction.EAST);
        WarehouseFixtures.stock(helper, rail.west(), new ItemStack(Items.COBBLESTONE, cobblestone));
        WarehouseFixtures.garage(helper, rail.north(), Direction.NORTH, 1);
        helper.setBlock(controller.east(), ServusShuttlesBlocks.PACKING_STATION.get().defaultBlockState());
        return helper.getBlockEntity(rail.west(), HeavyRackBlockEntity.class);
    }

    /** Both warehouses, the ducts from z 1 to 8 except the joining one, and the terminal. */
    private static StorageTerminalBlockEntity line(GameTestHelper helper, boolean secondPowered, boolean joined) {
        warehouse(helper, CONTROLLER_A, 30, true);
        warehouse(helper, CONTROLLER_B, 20, secondPowered);
        for (int z = 1; z <= 8; z++) {
            BlockPos duct = new BlockPos(4, 1, z);
            if (joined || !duct.equals(JOIN)) {
                TerminalWiring.dataDuct(helper, duct);
            }
        }
        helper.setBlock(TERMINAL, ServusShuttlesBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(TERMINAL, StorageTerminalBlockEntity.class);
    }

    private static StorageTerminalMenu menu(GameTestHelper helper, StorageTerminalBlockEntity terminal) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        return new StorageTerminalMenu(1, player.getInventory(), terminal);
    }

    private static boolean conflict(GameTestHelper helper) {
        return TerminalWarehouses.line(helper.getLevel(), helper.absolutePos(TERMINAL)).conflict();
    }

    private static boolean statusKeyIs(PackingStationBlockEntity station, String suffix) {
        return station.status().line().getContents() instanceof TranslatableContents contents && contents.getKey().endsWith(suffix);
    }

    /** Two warehouses on one line: no stock, a refused request, a dark lamp, a silent Packing Station and a supplier that answers 0. */
    @ServusGameTest(timeoutTicks = 200)
    public static void twoWarehousesPutTheLineInConflict(GameTestHelper helper) {
        StorageTerminalBlockEntity terminal = line(helper, true, true);
        StorageTerminalMenu menu = menu(helper, terminal);
        Waits.when(helper, () -> conflict(helper) && !WarehouseFixtures.lit(helper, TERMINAL), 80,
                () -> "the line should be in conflict and the terminal lamp dark", () -> {
            helper.assertTrue(!menu.stock().online() && menu.stock().reason().equals(LineWarehouse.CONFLICT_KEY),
                    "the terminal shows the conflict instead of stock, got " + menu.stock().reason());
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 5);
            helper.assertTrue(menu.stock().message().equals(LineWarehouse.CONFLICT_KEY),
                    "the request is refused for the conflict, got " + menu.stock().message());
            PackingStationBlockEntity station = helper.getBlockEntity(STATION_A, PackingStationBlockEntity.class);
            helper.assertTrue(statusKeyIs(station, LineWarehouse.CONFLICT_KEY) && station.status().alert(),
                    "the Packing Station names the conflict as an alert");
            int supplied = new WarehouseSupplier(station).supply(helper.absolutePos(TERMINAL), new ItemStack(Items.COBBLESTONE), 5).amount();
            helper.assertTrue(supplied == 0, "the supplier answers 0, got " + supplied);
            helper.succeed();
        });
    }

    /** A second warehouse whose controller has no power still puts the line in conflict. */
    @ServusGameTest(timeoutTicks = 100)
    public static void unpoweredSecondWarehouseStillConflicts(GameTestHelper helper) {
        line(helper, false, true);
        Waits.when(helper, () -> conflict(helper), 80, () -> "the line should be in conflict with an unpowered second warehouse",
                helper::succeed);
    }

    /** On a line in conflict the Input row keeps what the player hands in. */
    @ServusGameTest(timeoutTicks = 200)
    public static void inputRowWaitsThroughConflict(GameTestHelper helper) {
        StorageTerminalBlockEntity terminal = line(helper, true, true);
        Waits.when(helper, () -> conflict(helper), 80, () -> "the line should be in conflict", () -> {
            terminal.queueInput(new ItemStack(Items.OAK_LOG, 16));
            helper.runAfterDelay(60, () -> {
                int left = WarehouseFixtures.count(terminal.input(), Items.OAK_LOG);
                helper.assertTrue(left == 16, "the 16 logs stay in the Input row, " + left + " left");
                helper.assertTrue(LineWarehouse.CONFLICT_KEY.equals(terminal.inputRefusal()),
                        "the Input row says why, got " + terminal.inputRefusal());
                helper.succeed();
            });
        });
    }

    /** A warehouse on a line in conflict still puts away what reaches its Inbound Station. */
    @ServusGameTest(timeoutTicks = 400)
    public static void warehouseKeepsWorkingThroughConflict(GameTestHelper helper) {
        line(helper, true, true);
        HeavyRackBlockEntity rackA = helper.getBlockEntity(CONTROLLER_A.below().west(), HeavyRackBlockEntity.class);
        InboundStationBlockEntity inbound = WarehouseFixtures.inbound(helper, STATION_A.below().south(), Direction.SOUTH);
        Waits.when(helper, () -> conflict(helper), 80, () -> "the line should be in conflict", () -> {
            Transfer.insertItem(inbound.intake(), new ItemStack(Items.OAK_PLANKS, 16), false);
            Waits.when(helper, () -> WarehouseFixtures.count(rackA, Items.OAK_PLANKS) == 16, 300,
                    () -> "the planks should reach the rack, rack has " + WarehouseFixtures.count(rackA, Items.OAK_PLANKS), helper::succeed);
        });
    }

    /** A request made before the line joined a second warehouse waits through the conflict and finishes once the line is split again. */
    @ServusGameTest(timeoutTicks = 700)
    public static void requestResumesAfterTheLineIsSplit(GameTestHelper helper) {
        StorageTerminalBlockEntity terminal = line(helper, true, false);
        StorageTerminalMenu menu = menu(helper, terminal);
        Waits.when(helper, () -> TerminalWiring.stockOf(menu, Items.COBBLESTONE) == 30, 80,
                () -> "the terminal should list warehouse A's 30 cobblestone", () -> {
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 30);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request goes through, got " + menu.stock().message());
            TerminalWiring.dataDuct(helper, JOIN);
            helper.runAfterDelay(200, () -> {
                helper.assertTrue(conflict(helper), "the joined line is in conflict");
                helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0,
                        "nothing is sent while the line is in conflict, got " + terminal.outputCount(Items.COBBLESTONE));
                helper.assertTrue(!terminal.requests().isEmpty(), "the request waits");
                helper.setBlock(JOIN, Blocks.AIR.defaultBlockState());
                Waits.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 30, 400,
                        () -> "the 30 cobblestone should arrive once the line is split, got " + terminal.outputCount(Items.COBBLESTONE),
                        helper::succeed);
            });
        });
    }
}
