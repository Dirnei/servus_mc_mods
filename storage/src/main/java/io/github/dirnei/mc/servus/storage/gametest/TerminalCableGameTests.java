package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlock;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
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

/** The Storage Terminal on the data line: packets, reach, several warehouses, cancel and closed output. */
public final class TerminalCableGameTests {

    private static final String EMPTY = "empty_12x6x12";

    private TerminalCableGameTests() {
    }

    private static CrateBlockEntity crate(GameTestHelper helper, BlockPos pos, Item item, int count) {
        helper.setBlock(pos, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity crate = helper.getBlockEntity(pos, CrateBlockEntity.class);
        if (count > 0) {
            crate.insert(new ItemStack(item, count), false);
        }
        return crate;
    }

    private static void controller(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
    }

    private static StorageTerminalBlockEntity terminal(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(pos);
    }

    private static long stockOf(StorageTerminalMenu menu, Item item) {
        for (StockEntry entry : menu.stock().entries()) {
            if (entry.stack().is(item)) {
                return entry.count();
            }
        }
        return 0;
    }

    /** A request travels from a crate through the Packing Station and three data ducts into the output slots. */
    @ServusGameTest(timeoutTicks = 400)
    public static void terminalRequestArrivesAsPackets(GameTestHelper helper) {
        CrateBlockEntity crate = crate(helper, new BlockPos(1, 1, 3), Items.OAK_PLANKS, 20);
        controller(helper, new BlockPos(2, 1, 3));
        helper.setBlock(new BlockPos(3, 1, 3), ServusStorageBlocks.PACKING_STATION.get().defaultBlockState());
        TerminalWiring.railUnder(helper, new BlockPos(3, 1, 3));
        for (int x = 4; x <= 6; x++) {
            TerminalWiring.dataDuct(helper, new BlockPos(x, 1, 3));
        }
        BlockPos terminalPos = new BlockPos(7, 1, 3);
        StorageTerminalBlockEntity terminal = terminal(helper, terminalPos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
        TerminalWiring.when(helper, () -> helper.getBlockState(terminalPos).getValue(StorageNodeBlocks.ONLINE), 60,
                () -> "the terminal lamp should light once a warehouse is reachable", () -> {
            helper.assertTrue(stockOf(menu, Items.OAK_PLANKS) == 20, "the terminal lists 20 planks");
            menu.request(ItemKey.of(new ItemStack(Items.OAK_PLANKS)), 20);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request goes through, got " + menu.stock().message());
            helper.assertTrue(crate.isEmpty(), "the crate is picked at once");
            boolean[] sawSending = {false};
            TerminalWiring.when(helper, () -> {
                if (!menu.stock().requests().isEmpty() && menu.stock().requests().get(0).state().equals("SENDING")) {
                    sawSending[0] = true;
                }
                return terminal.outputCount(Items.OAK_PLANKS) == 20 && menu.stock().requests().isEmpty();
            }, 300, () -> "20 planks should arrive, had " + terminal.outputCount(Items.OAK_PLANKS) + ", rows " + menu.stock().requests(), () -> {
                helper.assertTrue(sawSending[0], "the request showed the state sending on its way");
                helper.succeed();
            });
        });
    }

    /** A terminal beside a controller but on no data line reaches nothing. */
    @ServusGameTest(timeoutTicks = 100)
    public static void terminalBesideControllerIsOffline(GameTestHelper helper) {
        crate(helper, new BlockPos(1, 1, 3), Items.COBBLESTONE, 10);
        controller(helper, new BlockPos(2, 1, 3));
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

    /** Two warehouses on one line: their stock adds up and a request takes from the fuller one first. */
    @ServusGameTest(timeoutTicks = 400)
    public static void twoWarehousesMergeAndSplit(GameTestHelper helper) {
        CrateBlockEntity first = crate(helper, new BlockPos(1, 1, 1), Items.COBBLESTONE, 30);
        controller(helper, new BlockPos(2, 1, 1));
        helper.setBlock(new BlockPos(3, 1, 1), ServusStorageBlocks.PACKING_STATION.get().defaultBlockState());
        TerminalWiring.railUnder(helper, new BlockPos(3, 1, 1));
        CrateBlockEntity second = crate(helper, new BlockPos(1, 1, 8), Items.COBBLESTONE, 20);
        controller(helper, new BlockPos(2, 1, 8));
        helper.setBlock(new BlockPos(3, 1, 8), ServusStorageBlocks.PACKING_STATION.get().defaultBlockState());
        TerminalWiring.railUnder(helper, new BlockPos(3, 1, 8));
        for (int z = 1; z <= 8; z++) {
            TerminalWiring.dataDuct(helper, new BlockPos(4, 1, z));
        }
        StorageTerminalBlockEntity terminal = terminal(helper, new BlockPos(5, 1, 4));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
        TerminalWiring.when(helper, () -> stockOf(menu, Items.COBBLESTONE) == 50, 60,
                () -> "the terminal should list 50 cobblestone from both warehouses, had " + stockOf(menu, Items.COBBLESTONE), () -> {
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 40);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request goes through, got " + menu.stock().message());
            helper.assertTrue(first.isEmpty() && second.count() == 10,
                    "30 come from the fuller warehouse and 10 from the other, left " + first.count() + " and " + second.count());
            TerminalWiring.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 40 && menu.stock().requests().isEmpty(), 360,
                    () -> "40 cobblestone should arrive, had " + terminal.outputCount(Items.COBBLESTONE) + ", rows " + menu.stock().requests(),
                    helper::succeed);
        });
    }

    /** Cancelling a request before its packets leave sends the picked items back into storage. */
    @ServusGameTest(timeoutTicks = 300)
    public static void terminalCancelPutsPicksBack(GameTestHelper helper) {
        CrateBlockEntity crate = crate(helper, new BlockPos(1, 1, 3), Items.COBBLESTONE, 64);
        controller(helper, new BlockPos(2, 1, 3));
        helper.setBlock(new BlockPos(3, 1, 3), ServusStorageBlocks.PACKING_STATION.get().defaultBlockState());
        TerminalWiring.railUnder(helper, new BlockPos(3, 1, 3));
        TerminalWiring.dataDuct(helper, new BlockPos(4, 1, 3));
        StorageTerminalBlockEntity terminal = terminal(helper, new BlockPos(5, 1, 3));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
        TerminalWiring.when(helper, () -> stockOf(menu, Items.COBBLESTONE) == 64, 60, () -> "the warehouse should be reachable", () -> {
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64);
            helper.assertTrue(crate.isEmpty(), "the crate is picked at once");
            int id = menu.stock().requests().get(0).id();
            menu.cancel(id);
            helper.assertTrue(menu.stock().requests().isEmpty(), "the cancelled request leaves the list");
            TerminalWiring.when(helper, () -> crate.count() == 64, 200,
                    () -> "the picked cobblestone should go back into the crate, crate has " + crate.count()
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
        crate(helper, new BlockPos(1, 1, 3), Items.COBBLESTONE, 10);
        controller(helper, new BlockPos(2, 1, 3));
        TerminalWiring.dataDuct(helper, new BlockPos(3, 1, 3));
        StorageTerminalBlockEntity terminal = terminal(helper, new BlockPos(4, 1, 3));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
        TerminalWiring.when(helper, () -> stockOf(menu, Items.COBBLESTONE) == 10, 60,
                () -> "the controller on the line should make the stock visible", () -> {
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 5);
            helper.assertTrue(menu.stock().message().equals("no_packing_station"), "the request is refused, got " + menu.stock().message());
            helper.assertTrue(menu.stock().requests().isEmpty(), "no request is left open");
            helper.succeed();
        });
    }
}
