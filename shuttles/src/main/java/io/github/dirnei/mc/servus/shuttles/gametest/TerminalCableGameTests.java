package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlock;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/** The Storage Terminal on the data line: packets, reach, cancel and closed output. */
public final class TerminalCableGameTests {

    private TerminalCableGameTests() {
    }

    /**
     * A controller at {@code controller} with rails at y 0 under it and under
     * the block east of it (where the tests put a Packing Station), a rack west
     * of the first rail holding the items and a garage north of it with one
     * shuttle.
     */
    private static HeavyRackBlockEntity warehouse(GameTestHelper helper, BlockPos controller, Item item, int count) {
        WarehouseFixtures.controller(helper, controller);
        BlockPos rail = controller.below();
        WarehouseFixtures.rail(helper, rail);
        WarehouseFixtures.rail(helper, rail.east());
        WarehouseFixtures.rack(helper, rail.west(), Direction.EAST);
        WarehouseFixtures.stock(helper, rail.west(), new ItemStack(item, count));
        WarehouseFixtures.garage(helper, rail.north(), Direction.NORTH, 1);
        return helper.getBlockEntity(rail.west(), HeavyRackBlockEntity.class);
    }

    private static StorageTerminalBlockEntity terminal(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusShuttlesBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(pos, StorageTerminalBlockEntity.class);
    }

    /** A request rides a shuttle from a rack to the Packing Station and goes through three data ducts into the output slots. */
    @ServusGameTest(timeoutTicks = 400)
    public static void terminalRequestArrivesAsPackets(GameTestHelper helper) {
        HeavyRackBlockEntity rack = warehouse(helper, new BlockPos(2, 1, 3), Items.OAK_PLANKS, 20);
        helper.setBlock(new BlockPos(3, 1, 3), ServusShuttlesBlocks.PACKING_STATION.get().defaultBlockState());
        TerminalWiring.railUnder(helper, new BlockPos(3, 1, 3));
        for (int x = 4; x <= 6; x++) {
            TerminalWiring.dataDuct(helper, new BlockPos(x, 1, 3));
        }
        BlockPos terminalPos = new BlockPos(7, 1, 3);
        StorageTerminalBlockEntity terminal = terminal(helper, terminalPos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
        Waits.when(helper, () -> helper.getBlockState(terminalPos).getValue(StorageNodeBlocks.ONLINE), 60,
                () -> "the terminal lamp should light once a warehouse is reachable", () -> {
            helper.assertTrue(TerminalWiring.stockOf(menu, Items.OAK_PLANKS) == 20, "the terminal lists 20 planks");
            menu.request(ItemKey.of(new ItemStack(Items.OAK_PLANKS)), 20);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request goes through, got " + menu.stock().message());
            boolean[] sawSending = {false};
            Waits.when(helper, () -> {
                if (!menu.stock().requests().isEmpty() && menu.stock().requests().get(0).state().equals("SENDING")) {
                    sawSending[0] = true;
                }
                return terminal.outputCount(Items.OAK_PLANKS) == 20 && menu.stock().requests().isEmpty();
            }, 300, () -> "20 planks should arrive, had " + terminal.outputCount(Items.OAK_PLANKS) + ", rows " + menu.stock().requests(), () -> {
                helper.assertTrue(sawSending[0], "the request showed the state sending on its way");
                helper.assertTrue(rack.isEmpty(), "the rack was emptied");
                helper.succeed();
            });
        });
    }

    /** A terminal beside a controller but on no data line reaches nothing. */
    @ServusGameTest(timeoutTicks = 100)
    public static void terminalBesideControllerIsOffline(GameTestHelper helper) {
        warehouse(helper, new BlockPos(2, 1, 3), Items.COBBLESTONE, 10);
        BlockPos terminalPos = new BlockPos(3, 1, 3);
        StorageTerminalBlockEntity terminal = terminal(helper, terminalPos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(45, () -> {
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(!menu.stock().online() && menu.stock().reason().equals("no_warehouse"),
                    "the terminal should say no warehouse is reachable, got " + menu.stock().reason());
            helper.assertTrue(!helper.getBlockState(terminalPos).getValue(StorageNodeBlocks.ONLINE), "the lamp stays dark");
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 5);
            helper.assertTrue(menu.stock().message().equals("offline"), "requests are refused, got " + menu.stock().message());
            helper.succeed();
        });
    }

    /** Cancelling a request before its packets leave leaves or puts the items back into storage. */
    @ServusGameTest(timeoutTicks = 500)
    public static void terminalCancelPutsPicksBack(GameTestHelper helper) {
        HeavyRackBlockEntity rack = warehouse(helper, new BlockPos(2, 1, 3), Items.COBBLESTONE, 64);
        helper.setBlock(new BlockPos(3, 1, 3), ServusShuttlesBlocks.PACKING_STATION.get().defaultBlockState());
        TerminalWiring.railUnder(helper, new BlockPos(3, 1, 3));
        TerminalWiring.dataDuct(helper, new BlockPos(4, 1, 3));
        StorageTerminalBlockEntity terminal = terminal(helper, new BlockPos(5, 1, 3));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
        Waits.when(helper, () -> TerminalWiring.stockOf(menu, Items.COBBLESTONE) == 64, 60, () -> "the warehouse should be reachable", () -> {
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64);
            int id = menu.stock().requests().get(0).id();
            menu.cancel(id);
            helper.assertTrue(menu.stock().requests().isEmpty(), "the cancelled request leaves the list");
            Waits.when(helper, () -> WarehouseFixtures.count(rack, Items.COBBLESTONE) == 64, 400,
                    () -> "the cobblestone should end back in the rack, rack has " + WarehouseFixtures.count(rack, Items.COBBLESTONE)
                            + ", output " + terminal.outputCount(Items.COBBLESTONE), () -> {
                helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0, "nothing reached the terminal");
                helper.succeed();
            });
        });
    }

    /** A hopper below the terminal cannot take anything out of the output slots. */
    @ServusGameTest(timeoutTicks = 100)
    public static void hopperCannotTakeTerminalOutput(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.HOPPER.defaultBlockState());
        StorageTerminalBlockEntity terminal = terminal(helper, new BlockPos(3, 2, 3));
        terminal.output().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 10));
        helper.runAfterDelay(40, () -> {
            HopperBlockEntity hopper = helper.getBlockEntity(new BlockPos(3, 1, 3), HopperBlockEntity.class);
            helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 10, "the output keeps its 10 cobblestone");
            helper.assertTrue(hopper.isEmpty(), "the hopper took nothing");
            helper.succeed();
        });
    }

    /** A warehouse reached only through its controller cannot ship: there is no Packing Station on the line. */
    @ServusGameTest(timeoutTicks = 100)
    public static void terminalRequestWithoutPackingStationIsRefused(GameTestHelper helper) {
        warehouse(helper, new BlockPos(2, 1, 3), Items.COBBLESTONE, 10);
        TerminalWiring.dataDuct(helper, new BlockPos(3, 1, 3));
        StorageTerminalBlockEntity terminal = terminal(helper, new BlockPos(4, 1, 3));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
        Waits.when(helper, () -> TerminalWiring.stockOf(menu, Items.COBBLESTONE) == 10, 60,
                () -> "the controller on the line should make the stock visible", () -> {
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 5);
            helper.assertTrue(menu.stock().message().equals("no_packing_station"), "the request is refused, got " + menu.stock().message());
            helper.assertTrue(menu.stock().requests().isEmpty(), "no request is left open");
            helper.succeed();
        });
    }
}
