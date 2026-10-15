package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.data.packet.Delivery;
import io.github.dirnei.mc.servus.foundation.data.packet.DeliveryReason;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlock;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Deliveries over the data line between a Storage Terminal and a Crafting
 * Station: terminal at x 2, six data ducts at x 3 to 8, station at x 9, all
 * at y 1, z 3. Moves from the terminal take their planks from a test stock
 * handed in as the delivery source; moves into it land in its Input row.
 */
public final class DataPacketGameTests {

    private static final BlockPos SOURCE = new BlockPos(2, 1, 3);
    private static final BlockPos STATION = new BlockPos(9, 1, 3);
    private static final int FIRST_DUCT = 3;
    private static final int LAST_DUCT = 8;

    private DataPacketGameTests() {
    }

    private record Setup(ItemSlots stock, StorageTerminalBlockEntity terminal, CraftingStationBlockEntity station) {
    }

    /** Terminal with a stock of 32 planks, six data ducts, a sticks station whose outputs are full so it never crafts. */
    private static Setup setup(GameTestHelper helper, boolean ducts) {
        ItemSlots stock = new ItemSlots(1);
        stock.setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 32));
        helper.setBlock(SOURCE, ServusStorageBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        StorageTerminalBlockEntity terminal = helper.getBlockEntity(SOURCE, StorageTerminalBlockEntity.class);
        helper.setBlock(STATION, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState()
                .setValue(CraftingStationBlock.FACING, Direction.NORTH));
        CraftingStationBlockEntity station = helper.getBlockEntity(STATION, CraftingStationBlockEntity.class);
        StationJobs.sticksJob(station, 64);
        for (int slot = 0; slot < station.outputs().getSlots(); slot++) {
            station.outputs().setStackInSlot(slot, new ItemStack(Items.STICK, 64));
        }
        if (ducts) {
            for (int x = FIRST_DUCT; x <= LAST_DUCT; x++) {
                TerminalWiring.dataDuct(helper, new BlockPos(x, 1, 3));
            }
        }
        return new Setup(stock, terminal, station);
    }

    private static int dropped(GameTestHelper helper, Item item) {
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item))
                .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    private static Delivery move(GameTestHelper helper, BlockPos from, BlockPos to, Item item, int amount) {
        return DataPackets.move(helper.getLevel(), helper.absolutePos(from), helper.absolutePos(to), new ItemStack(item), amount)
                .orElseThrow(() -> new AssertionError("the move should start"));
    }

    /** Moves planks from the terminal to the station, taken out of the test stock. */
    private static Delivery movePlanks(GameTestHelper helper, Setup setup, int amount) {
        return DataPackets.move(helper.getLevel(), helper.absolutePos(SOURCE), helper.absolutePos(STATION), new ItemStack(Items.OAK_PLANKS), amount,
                new ItemView(setup.stock())).orElseThrow(() -> new AssertionError("the move should start"));
    }

    /** Sixteen planks move from the terminal's stock into the station. */
    @ServusGameTest(timeoutTicks = 200)
    public static void packetMoveControllerToStation(GameTestHelper helper) {
        Setup setup = setup(helper, true);
        helper.runAfterDelay(2, () -> {
            Delivery delivery = movePlanks(helper, setup, 16);
            helper.succeedWhen(() -> {
                helper.assertTrue(delivery.state() == Delivery.State.DONE, "delivery should be done, is " + delivery);
                helper.assertTrue(WarehouseFixtures.count(setup.stock(), Items.OAK_PLANKS) == 16, "16 planks should remain, got " + WarehouseFixtures.count(setup.stock(), Items.OAK_PLANKS));
                helper.assertTrue(WarehouseFixtures.count(setup.station().inputs(), Items.OAK_PLANKS) == 16, "16 planks should be in the station");
                helper.assertTrue(delivery.delivered() == 16, "delivered 16, got " + delivery.delivered());
            });
        });
    }

    /** The data line is instant: eight planks arrive within the first ticks, distance does not matter. */
    @ServusGameTest(timeoutTicks = 100)
    public static void packetMoveIsInstant(GameTestHelper helper) {
        Setup setup = setup(helper, true);
        helper.runAfterDelay(2, () -> {
            Delivery delivery = movePlanks(helper, setup, 8);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(WarehouseFixtures.count(setup.station().inputs(), Items.OAK_PLANKS) == 8, "8 planks should have arrived after 2 ticks");
                helper.assertTrue(delivery.state() == Delivery.State.DONE, "delivery done, " + delivery);
                helper.succeed();
            });
        });
    }

    /** A sent stack of 20 arrives at once between members that are no gates. */
    @ServusGameTest(timeoutTicks = 100)
    public static void packetSendArrivesAtOnce(GameTestHelper helper) {
        Setup setup = setup(helper, true);
        helper.runAfterDelay(2, () -> {
            Delivery delivery = DataPackets.send(helper.getLevel(), helper.absolutePos(SOURCE), helper.absolutePos(STATION),
                    new ItemStack(Items.OAK_PLANKS, 20)).orElseThrow();
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(delivery.state() == Delivery.State.DONE && delivery.delivered() == 20, "all 20 delivered, " + delivery);
                helper.assertTrue(WarehouseFixtures.count(setup.station().inputs(), Items.OAK_PLANKS) == 20, "20 planks in the station");
                helper.assertTrue(WarehouseFixtures.count(setup.stock(), Items.OAK_PLANKS) == 32, "a send takes nothing from the stock");
                helper.succeed();
            });
        });
    }

    /** A target with room for 3 takes 3; the other 5 wait and go in once room frees up. */
    @ServusGameTest(timeoutTicks = 200)
    public static void packetTargetFullRetries(GameTestHelper helper) {
        Setup setup = setup(helper, true);
        ItemSlots inputs = setup.station().inputs();
        for (int slot = 0; slot < inputs.getSlots(); slot++) {
            inputs.setStackInSlot(slot, new ItemStack(Items.OAK_PLANKS, slot == 0 ? 61 : 64));
        }
        helper.runAfterDelay(2, () -> {
            Delivery delivery = movePlanks(helper, setup, 8);
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(delivery.delivered() == 3 && !delivery.ended(), "3 delivered and 5 waiting, " + delivery);
                helper.assertTrue(WarehouseFixtures.count(setup.stock(), Items.OAK_PLANKS) == 29, "the 5 waiting planks stay in the stock, got " + WarehouseFixtures.count(setup.stock(), Items.OAK_PLANKS));
                inputs.setStackInSlot(1, ItemStack.EMPTY);
                helper.runAfterDelay(25, () -> {
                    helper.assertTrue(delivery.state() == Delivery.State.DONE && delivery.delivered() == 8, "all 8 delivered after the retry, " + delivery);
                    helper.succeed();
                });
            });
        });
    }

    /** A move of 16 sticks from a station holding 4 ends done with 4. */
    @ServusGameTest(timeoutTicks = 200)
    public static void packetSourceShortEndsDone(GameTestHelper helper) {
        Setup setup = setup(helper, true);
        ItemSlots outputs = setup.station().outputs();
        for (int slot = 0; slot < outputs.getSlots(); slot++) {
            outputs.setStackInSlot(slot, slot == 0 ? new ItemStack(Items.STICK, 4) : ItemStack.EMPTY);
        }
        helper.runAfterDelay(2, () -> {
            Delivery delivery = move(helper, STATION, SOURCE, Items.STICK, 16);
            helper.succeedWhen(() -> {
                helper.assertTrue(delivery.state() == Delivery.State.DONE && delivery.delivered() == 4, "done with 4, " + delivery);
                helper.assertTrue(WarehouseFixtures.count(setup.terminal().input(), Items.STICK) == 4, "the 4 sticks should be in the terminal's Input row, got "
                        + WarehouseFixtures.count(setup.terminal().input(), Items.STICK));
            });
        });
    }

    /** Without a route the move fails at once and the stock keeps its planks. */
    @ServusGameTest(timeoutTicks = 40)
    public static void packetNoRouteKeepsItems(GameTestHelper helper) {
        Setup setup = setup(helper, true);
        helper.setBlock(new BlockPos(5, 1, 3), Blocks.AIR.defaultBlockState());
        helper.runAfterDelay(3, () -> {
            Delivery delivery = movePlanks(helper, setup, 8);
            helper.assertTrue(delivery.state() == Delivery.State.FAILED && delivery.reason() == DeliveryReason.NO_ROUTE,
                    "the move should fail with no route, " + delivery);
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(WarehouseFixtures.count(setup.stock(), Items.OAK_PLANKS) == 32, "the stock keeps its planks");
                helper.succeed();
            });
        });
    }

    /** The op command starts a move and the sticks arrive. */
    @ServusGameTest(timeoutTicks = 200)
    public static void packetCommandStartsMove(GameTestHelper helper) {
        Setup setup = setup(helper, true);
        helper.runAfterDelay(2, () -> {
            BlockPos from = helper.absolutePos(STATION);
            BlockPos to = helper.absolutePos(SOURCE);
            CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack()
                    .withLevel(helper.getLevel()).withPermission(LevelBasedPermissionSet.OWNER).withSuppressedOutput();
            String command = "servus_foundation data move %d %d %d %d %d %d minecraft:stick 8"
                    .formatted(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ());
            helper.getLevel().getServer().getCommands().performPrefixedCommand(source, command);
            helper.succeedWhen(() -> helper.assertTrue(WarehouseFixtures.count(setup.terminal().input(), Items.STICK) == 8,
                    "the command should move 8 sticks, got " + WarehouseFixtures.count(setup.terminal().input(), Items.STICK)));
        });
    }

    /** Breaking a duct while a move waits for room lets the rest go round a detour. */
    @ServusGameTest(timeoutTicks = 200)
    public static void packetDetourArrives(GameTestHelper helper) {
        Setup setup = setup(helper, true);
        TerminalWiring.dataDuct(helper, new BlockPos(4, 1, 4));
        TerminalWiring.dataDuct(helper, new BlockPos(5, 1, 4));
        TerminalWiring.dataDuct(helper, new BlockPos(6, 1, 4));
        ItemSlots inputs = fillInputs(setup, 3);
        helper.runAfterDelay(2, () -> {
            Delivery delivery = movePlanks(helper, setup, 8);
            helper.runAfterDelay(6, () -> helper.setBlock(new BlockPos(5, 1, 3), Blocks.AIR.defaultBlockState()));
            helper.runAfterDelay(10, () -> inputs.setStackInSlot(1, ItemStack.EMPTY));
            helper.succeedWhen(() -> {
                helper.assertTrue(delivery.state() == Delivery.State.DONE && delivery.delivered() == 8, "delivered by the detour, " + delivery);
                helper.assertTrue(WarehouseFixtures.count(setup.stock(), Items.OAK_PLANKS) == 24, "8 planks left the stock, " + WarehouseFixtures.count(setup.stock(), Items.OAK_PLANKS));
            });
        });
    }

    /** Breaking the only route fails the move; what it has not moved stays in the stock. */
    @ServusGameTest(timeoutTicks = 100)
    public static void packetBrokenRouteKeepsItems(GameTestHelper helper) {
        Setup setup = setup(helper, true);
        fillInputs(setup, 3);
        helper.runAfterDelay(2, () -> {
            Delivery delivery = movePlanks(helper, setup, 8);
            helper.runAfterDelay(6, () -> {
                helper.assertTrue(delivery.delivered() == 3 && !delivery.ended(), "3 moved before the break, " + delivery);
                helper.setBlock(new BlockPos(5, 1, 3), Blocks.AIR.defaultBlockState());
            });
            helper.runAfterDelay(15, () -> {
                helper.assertTrue(delivery.state() == Delivery.State.FAILED && delivery.reason() == DeliveryReason.ROUTE_BROKEN,
                        "the delivery should fail with route broken, " + delivery);
                helper.assertTrue(WarehouseFixtures.count(setup.stock(), Items.OAK_PLANKS) == 29, "29 planks stay in the stock, got " + WarehouseFixtures.count(setup.stock(), Items.OAK_PLANKS));
                helper.assertTrue(dropped(helper, Items.OAK_PLANKS) == 0, "nothing is dropped");
                helper.succeed();
            });
        });
    }

    /** Fills the station's inputs with planks, leaving room for {@code room} in the first slot. */
    private static ItemSlots fillInputs(Setup setup, int room) {
        ItemSlots inputs = setup.station().inputs();
        for (int slot = 0; slot < inputs.getSlots(); slot++) {
            inputs.setStackInSlot(slot, new ItemStack(Items.OAK_PLANKS, slot == 0 ? 64 - room : 64));
        }
        return inputs;
    }
}
