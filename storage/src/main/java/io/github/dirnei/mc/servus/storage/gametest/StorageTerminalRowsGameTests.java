package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlock;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** The Storage Terminal's Input and Output rows, automation inserts, the crafting popup and its id. */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StorageTerminalRowsGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos FULL_CRATE = new BlockPos(3, 1, 3);
    private static final BlockPos CONTROLLER = new BlockPos(4, 1, 3);
    private static final BlockPos TERMINAL = new BlockPos(5, 1, 3);
    private static final BlockPos FREE_CRATE = new BlockPos(4, 1, 2);

    private StorageTerminalRowsGameTests() {
    }

    private record Fixture(StorageTerminalMenu menu, StorageTerminalBlockEntity terminal, Player player) {
    }

    /** A crate full of planks, optionally an empty crate, the controller and the terminal wiring. */
    private static void setup(GameTestHelper helper, boolean freeCrate, java.util.function.Consumer<Fixture> test) {
        helper.setBlock(FULL_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity planks = helper.getBlockEntity(FULL_CRATE);
        planks.insert(new ItemStack(Items.OAK_PLANKS, planks.capacity()), false);
        if (freeCrate) {
            helper.setBlock(FREE_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        }
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        TerminalWiring.place(helper, TERMINAL);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, TERMINAL);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "warehouse should be online");
            test.accept(new Fixture(menu, terminal, player));
        });
    }

    private static int cobble(GameTestHelper helper) {
        CrateBlockEntity crate = helper.getBlockEntity(FREE_CRATE);
        return crate.count(0);
    }

    /** Cobblestone put into an input slot is stored over a few seconds and the slot empties. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void inputStores(GameTestHelper helper) {
        setup(helper, true, fixture -> {
            fixture.terminal().input().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 20));
            TerminalWiring.when(helper, () -> cobble(helper) == 20 && fixture.terminal().input().getStackInSlot(0).isEmpty(), 150,
                    () -> "the cobblestone should be stored, crate holds " + cobble(helper), helper::succeed);
        });
    }

    /** Without a Packing Station on the data line the stack stays in the input slot and the message says why. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void inputWaitsWithoutPackingStation(GameTestHelper helper) {
        BlockPos lone = new BlockPos(9, 1, 9);
        helper.setBlock(lone, ServusStorageBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(5, () -> {
            StorageTerminalBlockEntity terminal = helper.getBlockEntity(lone);
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
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void gridPushGoesThroughInput(GameTestHelper helper) {
        setup(helper, true, fixture -> {
            fixture.menu().setCarried(new ItemStack(Items.COBBLESTONE, 20));
            fixture.menu().pushCarried();
            helper.assertTrue(fixture.menu().getCarried().isEmpty(), "the carried stack should go into the input");
            TerminalWiring.when(helper, () -> cobble(helper) == 20, 150,
                    () -> "the cobblestone should end in the crate, crate holds " + cobble(helper), helper::succeed);
        });
    }

    /** Shift clicking an inventory slot moves it into the input row, which stores it. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void shiftClickToInput(GameTestHelper helper) {
        setup(helper, true, fixture -> {
            fixture.player().getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 20));
            fixture.menu().quickMoveStack(fixture.player(), fixture.menu().inventoryStart() + 27);
            helper.assertTrue(fixture.player().getInventory().getItem(0).isEmpty(), "the inventory slot should be empty");
            TerminalWiring.when(helper, () -> cobble(helper) == 20, 40,
                    () -> "the cobblestone should end in the crate, crate holds " + cobble(helper), helper::succeed);
        });
    }

    /** Automation: an item no request waits for goes through the input; a waited for item lands in the output. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void automationRouting(GameTestHelper helper) {
        setup(helper, true, fixture -> {
            BlockPos pos = helper.absolutePos(TerminalWiring.terminalPos(TERMINAL));
            IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
            helper.assertTrue(handler != null, "the terminal should take items from automation");
            ItemStack rest = ItemHandlerHelper.insertItemStacked(handler, new ItemStack(Items.COBBLESTONE, 10), false);
            helper.assertTrue(rest.isEmpty(), "the cobblestone should be taken, left " + rest);
            fixture.terminal().newRequest(helper.getLevel(), ItemKey.of(new ItemStack(Items.STICK)), 4);
            ItemStack sticks = ItemHandlerHelper.insertItemStacked(handler, new ItemStack(Items.STICK, 4), false);
            helper.assertTrue(sticks.isEmpty(), "the sticks should be taken, left " + sticks);
            int inOutput = fixture.terminal().outputCount(Items.STICK);
            helper.assertTrue(inOutput == 4, "the requested sticks should land in the output, got " + inOutput);
            TerminalWiring.when(helper, () -> cobble(helper) == 10, 40,
                    () -> "the cobblestone should go through the input into the crate, crate holds " + cobble(helper), helper::succeed);
        });
    }

    /** The Output row has its slots in one row below the Input row. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
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

    /** The popup's slots only take part while it is open; crafting works through them. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void popupCrafts(GameTestHelper helper) {
        setup(helper, true, fixture -> {
            StorageTerminalMenu menu = fixture.menu();
            menu.setCraftOpen(false);
            helper.assertTrue(!menu.getSlot(menu.gridStart()).isActive() && !menu.getSlot(menu.resultIndex()).isActive(),
                    "closed popup slots are inactive");
            menu.setCraftOpen(true);
            helper.assertTrue(menu.getSlot(menu.gridStart()).isActive(), "open popup slots are active");
            menu.craftSlots().setItem(1, new ItemStack(Items.OAK_PLANKS));
            menu.craftSlots().setItem(4, new ItemStack(Items.OAK_PLANKS));
            ItemStack result = menu.getSlot(menu.resultIndex()).getItem();
            helper.assertTrue(result.is(Items.STICK) && result.getCount() == 4, "the popup grid should make four sticks, got " + result);
            menu.clearGrid();
            helper.succeed();
        });
    }

    /** The block and item are registered as storage_terminal. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void storageTerminalId(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.BLOCK.getKey(ServusStorageBlocks.STORAGE_TERMINAL.get()).getPath().equals("storage_terminal"),
                "block id should be storage_terminal");
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(ServusStorageItems.STORAGE_TERMINAL.get()).getPath().equals("storage_terminal"),
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
    @GameTest(template = EMPTY, timeoutTicks = 400)
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
            TerminalWiring.when(helper, () -> cobble(helper) == 64 && fixture.terminal().input().getStackInSlot(0).isEmpty(), 350,
                    () -> "all 64 cobblestone should be stored, crate holds " + cobble(helper), () -> {
                long took = helper.getTick() - start;
                helper.assertTrue(took >= 120, "64 cobblestone at 8 per second take about 7 seconds, took " + took + " ticks");
                helper.succeed();
            });
        });
    }

    /** A To storage buffer with room for 16 takes 16 of the Input row; the rest waits until there is room again. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void fullToStorageKeepsInput(GameTestHelper helper) {
        setup(helper, false, fixture -> {
            io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity point =
                    helper.getBlockEntity(TerminalWiring.packingStationPos(TERMINAL));
            net.neoforged.neoforge.items.ItemStackHandler buffer = point.buffer();
            // Nothing can be put away (no crate takes cobblestone or diamonds), so the buffer stays as it is.
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
                TerminalWiring.when(helper, () -> inputCount(fixture.terminal()) == handedIn - 80, 200,
                        () -> "64 more should leave once there is room, " + inputCount(fixture.terminal()) + " left", () -> {
                    helper.runAfterDelay(40, () -> {
                        helper.assertTrue(inputCount(fixture.terminal()) == handedIn - 80, "the rest keeps waiting in the Input row");
                        helper.succeed();
                    });
                });
            });
        });
    }
}
