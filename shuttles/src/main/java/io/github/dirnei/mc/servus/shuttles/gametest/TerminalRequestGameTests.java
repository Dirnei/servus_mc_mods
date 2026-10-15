package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.terminal.RequestRow;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Storage Terminal requests and pushes delivered by shuttle. */
public final class TerminalRequestGameTests {

    private static final BlockPos TR_ORIGIN = new BlockPos(3, 1, 3);
    private static final BlockPos RQ_CONTROLLER = new BlockPos(1, 1, 1);
    private static final BlockPos RQ_TERMINAL = new BlockPos(2, 1, 2);
    private static final BlockPos RQ_GARAGE = new BlockPos(3, 1, 2);
    private static final BlockPos RQ_RACK = new BlockPos(6, 1, 2);

    private TerminalRequestGameTests() {
    }

    /** A request is brought by shuttle into the output slots, the player collects it, and a push stores a stack back. */
    @ServusGameTest(timeoutTicks = 600)
    public static void terminalRequestsAndPushes(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, TR_ORIGIN).withTerminal().stock(new ItemStack(Items.COBBLESTONE, 50));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = warehouse.terminal();
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online with one controller");
            helper.assertTrue(menu.stock().entries().size() == 1 && menu.stock().entries().get(0).count() == 50,
                    "stock should list 50 cobblestone");

            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64);
            Waits.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 50
                            && menu.stock().requests().size() == 1 && menu.stock().requests().get(0).state().equals("MISSING"), 300,
                    () -> "50 cobblestone should arrive as packets, had " + terminal.outputCount(Items.COBBLESTONE) + ", rows " + menu.stock().requests(), () -> {
            helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 0, "nothing goes into the inventory by itself");
            List<RequestRow> rows = menu.stock().requests();
            helper.assertTrue(rows.size() == 1 && rows.get(0).delivered() == 50 && rows.get(0).missing() == 14 && rows.get(0).state().equals("MISSING"),
                    "the request should show 50 delivered and 14 missing, got " + rows);
            menu.quickMoveStack(player, 0);
            helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 50, "shift click should collect the delivery");
            helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0, "the output slot should be empty");

            menu.cancel(rows.get(0).id());
            helper.assertTrue(menu.stock().requests().isEmpty(), "the cancelled request leaves the list");

            int slot = player.getInventory().findSlotMatchingItem(new ItemStack(Items.COBBLESTONE));
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 20));
            int menuIndex = menu.inventoryStart() + (slot < 9 ? 27 + slot : slot - 9);
            menu.quickMoveStack(player, menuIndex);
            helper.assertTrue(player.getInventory().countItem(Items.COBBLESTONE) == 0, "inventory should be empty of cobblestone");
            // The shift click goes into the Input row, which stores it on the next tick.
            Waits.when(helper, () -> warehouse.rackCount(Items.COBBLESTONE) == 20, MiniWarehouse.TRIP_TICKS * 2,
                    () -> "the rack should hold 20 cobblestone after the push, had " + warehouse.rackCount(Items.COBBLESTONE), () -> {
                menu.request(ItemKey.of(new ItemStack(Items.BREAD)), 5);
                helper.assertTrue(menu.stock().message().equals("out_of_stock"), "a request for nothing in stock is refused, got " + menu.stock().message());
                helper.succeed();
            });
            });
        });
    }

    private static StorageTerminalMenu requestSetup(GameTestHelper helper, Player player, int cobblestone) {
        WarehouseFixtures.controller(helper, RQ_CONTROLLER);
        WarehouseFixtures.railRow(helper, 2, 6, 1);
        TerminalWiring.place(helper, RQ_TERMINAL);
        WarehouseFixtures.garage(helper, RQ_GARAGE, Direction.SOUTH, 0);
        WarehouseFixtures.rack(helper, RQ_RACK, Direction.NORTH);
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(RQ_GARAGE, ShuttleGarageBlockEntity.class);
        garage.addShuttle();
        WarehouseFixtures.stock(helper, RQ_RACK, new ItemStack(Items.COBBLESTONE, cobblestone));
        StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, RQ_TERMINAL);
        return new StorageTerminalMenu(1, player.getInventory(), terminal);
    }

    /** A request over a stack splits into two loads and shows its progress until both arrived. */
    @ServusGameTest(timeoutTicks = 400)
    public static void requestSplitsAndDelivers(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = requestSetup(helper, player, 200);
        StorageTerminalBlockEntity terminal = menu.terminal();
        helper.runAfterDelay(10, () -> {
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 100);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request should go through");
            helper.runAfterDelay(6, () -> {
                List<RequestRow> rows = menu.stock().requests();
                helper.assertTrue(rows.size() == 1 && rows.get(0).requested() == 100 && rows.get(0).delivered() == 0, "one open request for 100, got " + rows);
                helper.assertTrue(rows.get(0).state().equals("PICKING") || rows.get(0).state().equals("SENDING"), "state should be picking or sending, was " + rows.get(0).state());
            });
            Waits.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 100 && menu.stock().requests().isEmpty(), 360,
                    () -> "100 cobblestone should arrive and the request settle, had " + terminal.outputCount(Items.COBBLESTONE)
                            + ", rows " + menu.stock().requests(), helper::succeed);
        });
    }

    /** When the output slots are full the shuttle waits beside the terminal and unloads once a slot frees up. */
    @ServusGameTest(timeoutTicks = 400)
    public static void outputFullShuttleWaits(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        StorageTerminalMenu menu = requestSetup(helper, player, 64);
        StorageTerminalBlockEntity terminal = menu.terminal();
        for (int i = 0; i < terminal.output().getSlots(); i++) {
            terminal.output().setStackInSlot(i, new ItemStack(Items.DIRT, 64));
        }
        helper.runAfterDelay(10, () -> {
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64);
            Waits.when(helper, () -> !menu.stock().requests().isEmpty() && menu.stock().requests().get(0).state().equals("WAITING_ROOM"), 300,
                    () -> "the packets should be waiting for room, got " + menu.stock().requests(), () -> {
                List<RequestRow> rows = menu.stock().requests();
                helper.assertTrue(rows.size() == 1 && rows.get(0).delivered() == 0, "nothing delivered yet, got " + rows);
                helper.assertTrue(terminal.outputCount(Items.COBBLESTONE) == 0, "nothing can land yet");
                terminal.output().setStackInSlot(0, ItemStack.EMPTY);
                Waits.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 64 && menu.stock().requests().isEmpty(), 300,
                        () -> "the waiting packets should land once a slot is free, had " + terminal.outputCount(Items.COBBLESTONE)
                                + ", rows " + menu.stock().requests(), helper::succeed);
            });
        });
    }
}
