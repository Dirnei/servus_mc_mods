package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.data.packet.PacketInlets;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlock;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Storage;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** The Storage Terminal's Input and Output rows, automation inserts, the crafting popup and its id. */
public final class StorageTerminalRowsGameTests {

    /** The mini warehouse's rails start here, so its Packing Station stands at {@link #TERMINAL}. */
    private static final BlockPos ORIGIN = new BlockPos(4, 1, 2);
    private static final BlockPos TERMINAL = new BlockPos(5, 1, 3);
    private static final BlockPos RACK = new BlockPos(4, 1, 1);

    private StorageTerminalRowsGameTests() {
    }

    private record Fixture(StorageTerminalMenu menu, StorageTerminalBlockEntity terminal, Player player) {
    }

    /** A mini warehouse whose rack holds planks, full unless {@code room}, and the terminal on its Packing Station. */
    private static void setup(GameTestHelper helper, boolean room, Consumer<Fixture> test) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).withTerminal();
        HeavyRackBlockEntity rack = warehouse.rack();
        int slots = room ? 1 : rack.items().getSlots();
        for (int slot = 0; slot < slots; slot++) {
            rack.items().setStackInSlot(slot, new ItemStack(Items.OAK_PLANKS, 64));
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online");
            test.accept(new Fixture(menu, terminal, player));
        });
    }

    private static int cobble(GameTestHelper helper) {
        return WarehouseFixtures.count(helper.getBlockEntity(RACK, HeavyRackBlockEntity.class), Items.COBBLESTONE);
    }

    /** Cobblestone put into an input slot is stored over a few seconds and the slot empties. */
    @ServusGameTest(timeoutTicks = 400)
    public static void inputStores(GameTestHelper helper) {
        setup(helper, true, fixture -> {
            fixture.terminal().input().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 20));
            Waits.when(helper, () -> cobble(helper) == 20 && fixture.terminal().input().getStackInSlot(0).isEmpty(), 350,
                    () -> "the cobblestone should be stored, rack holds " + cobble(helper), helper::succeed);
        });
    }

    /** Without a Packing Station on the data line the stack stays in the input slot and the message says why. */
    @ServusGameTest(timeoutTicks = 100)
    public static void inputWaitsWithoutPackingStation(GameTestHelper helper) {
        BlockPos lone = new BlockPos(9, 1, 9);
        helper.setBlock(lone, ServusShuttlesBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(5, () -> {
            StorageTerminalBlockEntity terminal = helper.getBlockEntity(lone, StorageTerminalBlockEntity.class);
            terminal.input().setStackInSlot(0, new ItemStack(Items.BREAD, 5));
            helper.runAfterDelay(30, () -> {
                ItemStack left = terminal.input().getStackInSlot(0);
                helper.assertTrue(left.is(Items.BREAD) && left.getCount() == 5, "the bread should stay in the input, got " + left);
                helper.assertTrue("input_no_packing_station".equals(terminal.inputRefusal()),
                        "the input should wait for a Packing Station, got " + terminal.inputRefusal());
                helper.succeed();
            });
        });
    }

    /** Clicking the grid with a carried stack moves it into the input row, which stores it. */
    @ServusGameTest(timeoutTicks = 400)
    public static void gridPushGoesThroughInput(GameTestHelper helper) {
        setup(helper, true, fixture -> {
            fixture.menu().setCarried(new ItemStack(Items.COBBLESTONE, 20));
            fixture.menu().pushCarried();
            helper.assertTrue(fixture.menu().getCarried().isEmpty(), "the carried stack should go into the input");
            Waits.when(helper, () -> cobble(helper) == 20, 350,
                    () -> "the cobblestone should end in the rack, rack holds " + cobble(helper), helper::succeed);
        });
    }

    /** Shift clicking an inventory slot moves it into the input row, which stores it. */
    @ServusGameTest(timeoutTicks = 300)
    public static void shiftClickToInput(GameTestHelper helper) {
        setup(helper, true, fixture -> {
            fixture.player().getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 20));
            fixture.menu().quickMoveStack(fixture.player(), fixture.menu().inventoryStart() + 27);
            helper.assertTrue(fixture.player().getInventory().getItem(0).isEmpty(), "the inventory slot should be empty");
            Waits.when(helper, () -> cobble(helper) == 20, MiniWarehouse.TRIP_TICKS * 2,
                    () -> "the cobblestone should end in the rack, rack holds " + cobble(helper), helper::succeed);
        });
    }

    /** Packets: an item no request waits for goes through the input; a waited for item lands in the output. */
    @ServusGameTest(timeoutTicks = 300)
    public static void packetRouting(GameTestHelper helper) {
        setup(helper, true, fixture -> {
            BlockPos pos = helper.absolutePos(TerminalWiring.terminalPos(TERMINAL));
            ItemView handler = PacketInlets.handler(helper.getLevel(), pos, Direction.UP);
            helper.assertTrue(handler != null, "the terminal should take packets through its inlet");
            ItemStack rest = handler.insertItemStacked(new ItemStack(Items.COBBLESTONE, 10), false);
            helper.assertTrue(rest.isEmpty(), "the cobblestone should be taken, left " + rest);
            fixture.terminal().newRequest(helper.getLevel(), ItemKey.of(new ItemStack(Items.STICK)), 4);
            ItemStack sticks = handler.insertItemStacked(new ItemStack(Items.STICK, 4), false);
            helper.assertTrue(sticks.isEmpty(), "the sticks should be taken, left " + sticks);
            int inOutput = fixture.terminal().outputCount(Items.STICK);
            helper.assertTrue(inOutput == 4, "the requested sticks should land in the output, got " + inOutput);
            Waits.when(helper, () -> cobble(helper) == 10, MiniWarehouse.TRIP_TICKS * 2,
                    () -> "the cobblestone should go through the input into the rack, rack holds " + cobble(helper), helper::succeed);
        });
    }

    /** The Output row has its slots in one row below the Input row. */
    @ServusGameTest(timeoutTicks = 60)
    public static void outputRowDelivers(GameTestHelper helper) {
        setup(helper, true, fixture -> {
            StorageTerminalMenu menu = fixture.menu();
            helper.assertTrue(menu.outputSlots() == Storage.ROW_SLOTS, "nine output slots, got " + menu.outputSlots());
            for (int i = 0; i < menu.outputSlots(); i++) {
                helper.assertTrue(menu.getSlot(i).y == Storage.OUTPUT.y(), "output slot " + i + " should sit in the output row");
                helper.assertTrue(menu.getSlot(menu.inputStart() + i).y == Storage.INPUT.y(), "input slot " + i + " should sit in the input row");
            }
            helper.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.COBBLESTONE)), "the player cannot place into the output");
            helper.succeed();
        });
    }

    /** The order grid's cells only take part while it is open; a click sets a ghost copy and the cursor keeps its items. */
    @ServusGameTest(timeoutTicks = 60)
    public static void orderCellsAreGhosts(GameTestHelper helper) {
        setup(helper, true, fixture -> {
            StorageTerminalMenu menu = fixture.menu();
            menu.setOrderOpen(false);
            helper.assertTrue(!menu.getSlot(menu.gridStart()).isActive(), "closed order grid cells are inactive");
            menu.setOrderOpen(true);
            helper.assertTrue(menu.getSlot(menu.gridStart()).isActive(), "open order grid cells are active");
            menu.setCarried(new ItemStack(Items.OAK_PLANKS, 10));
            menu.clicked(menu.gridStart() + 1, 0, ContainerInput.PICKUP, fixture.player());
            menu.clicked(menu.gridStart() + 4, 0, ContainerInput.PICKUP, fixture.player());
            helper.assertTrue(menu.gridCells().get(1).is(Items.OAK_PLANKS) && menu.gridCells().get(1).getCount() == 1,
                    "a click sets one ghost plank, got " + menu.gridCells().get(1));
            helper.assertTrue(menu.getCarried().getCount() == 10, "the cursor keeps its planks, has " + menu.getCarried());
            menu.setCarried(ItemStack.EMPTY);
            menu.clicked(menu.gridStart() + 4, 0, ContainerInput.PICKUP, fixture.player());
            helper.assertTrue(menu.gridCells().get(4).isEmpty(), "an empty click clears the cell");
            helper.assertTrue(menu.quickMoveStack(fixture.player(), menu.gridStart() + 1).isEmpty(), "a ghost cell hands nothing out");
            helper.succeed();
        });
    }

    /** The block and item are registered as storage_terminal. */
    @ServusGameTest(timeoutTicks = 20)
    public static void storageTerminalId(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.BLOCK.getKey(ServusShuttlesBlocks.STORAGE_TERMINAL.get()).getPath().equals("storage_terminal"),
                "block id should be storage_terminal");
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(ServusShuttlesItems.STORAGE_TERMINAL.get()).getPath().equals("storage_terminal"),
                "item id should be storage_terminal");
        helper.succeed();
    }

    private static int inputCount(StorageTerminalBlockEntity terminal) {
        int total = 0;
        for (int slot = 0; slot < terminal.input().getSlots(); slot++) {
            total += terminal.input().getStackInSlot(slot).getCount();
        }
        return total;
    }

    /** A stack in the Input row leaves at the Packing Station's 8 per second, not all at once. */
    @ServusGameTest(timeoutTicks = 600)
    public static void inputDrainsGradually(GameTestHelper helper) {
        setup(helper, true, fixture -> {
            long start = helper.getTick();
            fixture.terminal().input().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 64));
            helper.runAfterDelay(40, () -> {
                int left = inputCount(fixture.terminal());
                // At most one second of saved up budget plus two seconds at 8 per second, at least about 1.5 seconds' worth.
                helper.assertTrue(left >= 64 - 25 && left <= 64 - 10,
                        "about 16 to 24 cobblestone should have left after 2 seconds, " + left + " left");
            });
            Waits.when(helper, () -> cobble(helper) == 64 && fixture.terminal().input().getStackInSlot(0).isEmpty(), 550,
                    () -> "all 64 cobblestone should be stored, rack holds " + cobble(helper), () -> {
                long took = helper.getTick() - start;
                helper.assertTrue(took >= 120, "64 cobblestone at 8 per second take about 7 seconds, took " + took + " ticks");
                helper.succeed();
            });
        });
    }

    /** A To storage buffer with room for 16 takes 16 of the Input row; the rest waits until there is room again. */
    @ServusGameTest(timeoutTicks = 400)
    public static void fullToStorageKeepsInput(GameTestHelper helper) {
        setup(helper, false, fixture -> {
            PackingStationBlockEntity point =
                    helper.getBlockEntity(TERMINAL, PackingStationBlockEntity.class);
            ItemSlots buffer = point.buffer();
            // Nothing can be put away (the only rack is full), so the buffer stays as it is.
            for (int slot = 0; slot < buffer.getSlots(); slot++) {
                buffer.setStackInSlot(slot, slot == 0 ? new ItemStack(Items.COBBLESTONE, 48) : new ItemStack(Items.DIAMOND, 64));
            }
            for (int slot = 0; slot < fixture.terminal().input().getSlots(); slot++) {
                fixture.terminal().input().setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 16));
            }
            int handedIn = 16 * fixture.terminal().input().getSlots();
            helper.runAfterDelay(100, () -> {
                helper.assertTrue(inputCount(fixture.terminal()) == handedIn - 16,
                        "only the 16 that fit should leave the Input row, " + inputCount(fixture.terminal()) + " left");
                helper.assertTrue(buffer.getStackInSlot(0).getCount() == 64, "To storage should be full");
                buffer.setStackInSlot(1, ItemStack.EMPTY);
                Waits.when(helper, () -> inputCount(fixture.terminal()) == handedIn - 80, 200,
                        () -> "64 more should leave once there is room, " + inputCount(fixture.terminal()) + " left", () -> {
                    helper.runAfterDelay(40, () -> {
                        helper.assertTrue(inputCount(fixture.terminal()) == handedIn - 80, "the rest keeps waiting in the Input row");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** Logs handed in with no room anywhere do not wait silently: the Packing Station shows the blocked lamp and says why. */
    @ServusGameTest(timeoutTicks = 200)
    public static void inputWithoutRoomShowsBlocked(GameTestHelper helper) {
        setup(helper, false, fixture -> {
            BlockPos pos = TERMINAL;
            PackingStationBlockEntity point =
                    helper.getBlockEntity(pos, PackingStationBlockEntity.class);
            fixture.terminal().input().setStackInSlot(0, new ItemStack(Items.OAK_LOG, 64));
            Waits.when(helper, () -> helper.getBlockState(pos).getValue(StorageNodeBlocks.ACTIVITY)
                    == StorageNodeBlocks.Activity.BLOCKED, 150,
                    () -> "the Packing Station shows the blocked lamp", () -> {
                helper.assertTrue(point.status().alert() && !point.status().line().getString().isEmpty(),
                        "its status line says why, was " + point.status().line().getString());
                helper.succeed();
            });
        });
    }
}
