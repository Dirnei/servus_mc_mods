package io.github.dirnei.mc.servus.storage.gametest;

import static io.github.dirnei.mc.servus.storage.gametest.CpuLine.ACCESS;
import static io.github.dirnei.mc.servus.storage.gametest.CpuLine.CONTROLLER;
import static io.github.dirnei.mc.servus.storage.gametest.CpuLine.CPU_A;
import static io.github.dirnei.mc.servus.storage.gametest.CpuLine.CRAFTING_TABLE;
import static io.github.dirnei.mc.servus.storage.gametest.CpuLine.PATTERNS;
import static io.github.dirnei.mc.servus.storage.gametest.CpuLine.PLANKS;
import static io.github.dirnei.mc.servus.storage.gametest.CpuLine.RACK_A;
import static io.github.dirnei.mc.servus.storage.gametest.CpuLine.STICK;
import static io.github.dirnei.mc.servus.storage.gametest.CpuLine.bufferedSetup;
import static io.github.dirnei.mc.servus.storage.gametest.CpuLine.cpu;
import static io.github.dirnei.mc.servus.storage.gametest.CpuLine.line;
import static io.github.dirnei.mc.servus.storage.gametest.CpuLine.lineReady;
import static io.github.dirnei.mc.servus.storage.gametest.CpuLine.setup;
import static io.github.dirnei.mc.servus.storage.gametest.CpuLine.stock;
import static io.github.dirnei.mc.servus.storage.gametest.CpuLine.warehouse;
import static io.github.dirnei.mc.servus.storage.gametest.CpuLine.whenBuffered;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlock;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.storage.order.OrderBook;
import io.github.dirnei.mc.servus.storage.order.WorkOrder;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.pattern.PatternStorageBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;

/** The CPU buffer on the line of {@link CpuLine}, and orders delivered straight to the terminal. */
public final class CpuBufferGameTests {

    private CpuBufferGameTests() {
    }

    /** Planks lying as items within {@code radius} of a test position. */
    static int droppedNear(GameTestHelper helper, BlockPos at, int radius, Item item) {
        BlockPos pos = helper.absolutePos(at);
        AABB box = new AABB(pos).inflate(radius);
        int count = 0;
        for (ItemEntity entity
                : helper.getLevel().getEntitiesOfClass(ItemEntity.class, box)) {
            if (entity.getItem().is(item)) {
                count += entity.getItem().getCount();
            }
        }
        return count;
    }

    /** Planks one order holds in the buffer are not stock for a second order, which plans with the warehouse only. */
    @ServusGameTest(timeoutTicks = 1400)
    public static void bufferedPlanksNotForOthers(GameTestHelper helper) {
        CpuBlockEntity cpu = bufferedSetup(helper, 6, 1);
        Waits.when(helper, () -> lineReady(helper), 60, () -> "the station and the CPU should join the line", () -> {
            OrderBook book = cpu.book();
            WorkOrder first = book.create(CRAFTING_TABLE, 1, 0).order();
            helper.assertTrue(first != null, "the crafting table order should be made");
            whenBuffered(helper, cpu, first, PLANKS, () -> {
                OrderBook.Preview preview = book.preview(STICK, 4);
                helper.assertTrue(preview.fromStock().getOrDefault(PLANKS, 0) == 0,
                        "the buffered planks are no stock for others: " + preview.fromStock());
                OrderBook.Created second = book.create(STICK, 4, 0);
                helper.assertTrue(second.made() && second.order().reserved().getOrDefault(PLANKS, 0) == 0
                        && second.order().state() == WorkOrder.State.BLOCKED, "the second order misses planks: " + second.order().state());
                int held = cpu.buffer().held(first.id(), PLANKS);
                helper.assertTrue(held == 4, "the first order keeps its 4 planks, has " + held);
                book.cancel(second.order().id());
                Waits.when(helper, () -> first.state() == WorkOrder.State.DONE, 1200, () -> "the crafting table should be made", helper::succeed);
            });
        });
    }

    /** Cancelling an order with planks in the buffer sends them to the Packing Station; a shuttle puts them into a rack. */
    @ServusGameTest(timeoutTicks = 1400)
    public static void cancelSendsBufferToStorage(GameTestHelper helper) {
        CpuBlockEntity cpu = bufferedSetup(helper, 6, 1);
        Waits.when(helper, () -> lineReady(helper), 60, () -> "the station and the CPU should join the line", () -> {
            WorkOrder order = cpu.book().create(CRAFTING_TABLE, 1, 0).order();
            whenBuffered(helper, cpu, order, PLANKS, () -> {
                helper.assertTrue(cpu.book().cancel(order.id()), "the order can be cancelled");
                Waits.when(helper, () -> warehouse(helper).available(PLANKS) == 4 && cpu.bufferUsed() == 0, 600,
                        () -> "the 4 planks should be put away, warehouse has " + warehouse(helper).available(PLANKS)
                                + ", buffer " + cpu.buffer(), helper::succeed);
            });
        });
    }

    /** Breaking a Buffer block while planks wait sends them to the Packing Station and a shuttle puts them into a rack. */
    @ServusGameTest(timeoutTicks = 1400)
    public static void unformPutsBufferAway(GameTestHelper helper) {
        CpuBlockEntity cpu = bufferedSetup(helper, 6, 1);
        Waits.when(helper, () -> lineReady(helper), 60, () -> "the station and the CPU should join the line", () -> {
            WorkOrder order = cpu.book().create(CRAFTING_TABLE, 1, 0).order();
            whenBuffered(helper, cpu, order, PLANKS, () -> {
                BlockPos buffer = CPU_A.offset(1, 1, 0);
                helper.assertTrue(helper.getBlockState(buffer).is(ServusStorageBlocks.CPU_BUFFER.get()), "a Buffer block is broken");
                helper.setBlock(buffer, Blocks.AIR.defaultBlockState());
                helper.assertTrue(!helper.getBlockState(CPU_A).getValue(CpuBlock.FORMED), "the CPU unforms");
                helper.assertTrue(order.state() == WorkOrder.State.CANCELLED, "the order is cancelled");
                Waits.when(helper, () -> warehouse(helper).available(PLANKS) == 4, 600,
                        () -> "the 4 planks should be put away, warehouse has " + warehouse(helper).available(PLANKS), () -> {
                            helper.assertTrue(droppedNear(helper, CPU_A, 3, Items.OAK_PLANKS) == 0, "no planks dropped at the CPU");
                            helper.succeed();
                        });
            });
        });
    }

    /** Breaking a block while the only Packing Station's To storage buffer is full drops the planks beside the station. */
    @ServusGameTest(timeoutTicks = 1400)
    public static void unformFullPackingStationDrops(GameTestHelper helper) {
        CpuBlockEntity cpu = bufferedSetup(helper, 6, 1);
        Waits.when(helper, () -> lineReady(helper), 60, () -> "the station and the CPU should join the line", () -> {
            WorkOrder order = cpu.book().create(CRAFTING_TABLE, 1, 0).order();
            whenBuffered(helper, cpu, order, PLANKS, () -> {
                PackingStationBlockEntity point =
                        helper.getBlockEntity(ACCESS, PackingStationBlockEntity.class);
                for (int i = 0; i < point.buffer().getSlots(); i++) {
                    point.buffer().setStackInSlot(i, new ItemStack(Items.COBBLESTONE, 64));
                }
                helper.setBlock(CPU_A.offset(1, 1, 1), Blocks.AIR.defaultBlockState());
                Waits.when(helper, () -> droppedNear(helper, ACCESS, 2, Items.OAK_PLANKS) == 4, 100,
                        () -> "the 4 planks should drop beside the Packing Station, found " + droppedNear(helper, ACCESS, 2, Items.OAK_PLANKS)
                                + " there and " + droppedNear(helper, CPU_A, 3, Items.OAK_PLANKS) + " at the CPU", helper::succeed);
            });
        });
    }

    /** Without a Packing Station on the line the buffered planks drop at the broken block. */
    @ServusGameTest(timeoutTicks = 100)
    public static void unformWithoutPackingStationDrops(GameTestHelper helper) {
        TerminalWiring.dataDuct(helper, new BlockPos(5, 2, 2));
        CpuBlockEntity cpu = CpuBuild.formSlotted(helper, CPU_A);
        cpu.buffer().expect(1, 7, PLANKS, 4, 64);
        cpu.buffer().land(PLANKS, 4);
        BlockPos broken = CPU_A.offset(1, 1, 1);
        helper.runAfterDelay(5, () -> {
            helper.setBlock(broken, Blocks.AIR.defaultBlockState());
            helper.assertTrue(droppedNear(helper, broken, 1, Items.OAK_PLANKS) == 4,
                    "the 4 planks drop at the broken block, found " + droppedNear(helper, broken, 1, Items.OAK_PLANKS));
            helper.succeed();
        });
    }

    /** A hopper above a formed CPU cannot put items in: the CPU has no item capability, even while its buffer expects planks. */
    @ServusGameTest(timeoutTicks = 100)
    public static void inletRefusesHopper(GameTestHelper helper) {
        CpuBlockEntity cpu = CpuBuild.formSlotted(helper, CPU_A);
        cpu.buffer().expect(1, 7, PLANKS, 4, 64);
        BlockPos hopperPos = CPU_A.offset(0, 2, 0);
        helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopper =
                helper.getBlockEntity(hopperPos, HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.Item.BLOCK,
                helper.absolutePos(CPU_A.offset(0, 1, 0)), Direction.UP) == null, "a CPU block has no item capability");
        helper.assertTrue(helper.getLevel().getCapability(ServusFoundationCapabilities.PACKET_INLET,
                helper.absolutePos(CPU_A.offset(0, 1, 0)), Direction.UP) != null, "a formed CPU block has a packet inlet");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(hopper.getItem(0).getCount() == 4, "the hopper keeps its planks");
            helper.assertTrue(cpu.buffer().held(7, PLANKS) == 0, "nothing reached the buffer");
            helper.succeed();
        });
    }

    /** The summary counts buffer stacks in use against the room. */
    @ServusGameTest(timeoutTicks = 100)
    public static void summaryShowsBuffer(GameTestHelper helper) {
        setup(helper, false);
        CpuBlockEntity cpu = CpuBuild.formSlotted(helper, CPU_A);
        helper.runAfterDelay(10, () -> {
            for (int i = 0; i < 3; i++) {
                helper.assertTrue(cpu.book().create(STICK, 8, 0).made(), "order " + i + " should be made");
            }
            cpu.buffer().expect(1, 1, PLANKS, 4, 64);
            cpu.buffer().expect(2, 2, PLANKS, 4, 64);
            cpu.buffer().expect(3, 3, PLANKS, 4, 64);
            cpu.buffer().land(PLANKS, 12);
            Component summary = cpu.summary();
            helper.assertTrue(summary.getContents() instanceof TranslatableContents contents
                            && Arrays.equals(contents.getArgs(), new Object[] {2, 2, 1, 3, 6}),
                    "summary should say 2 of 2 in use, 1 waiting and 3 of 6 buffer stacks, got " + summary.getString());
            helper.succeed();
        });
    }

    /** Sticks crafted for a request travel straight from the station to the terminal and never enter the warehouse. */
    @ServusGameTest(timeoutTicks = 1000)
    public static void sticksGoStraightToTerminal(GameTestHelper helper) {
        setup(helper, true);
        stock(helper, RACK_A, Items.OAK_PLANKS, 10);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int[] stockedSticks = {0};
        helper.onEachTick(() -> {
            if (helper.getBlockEntity(CONTROLLER, BlockEntity.class) instanceof WarehouseControllerBlockEntity controller) {
                controller.network().ifPresent(network -> stockedSticks[0] = Math.max(stockedSticks[0], (int) network.available(STICK)));
            }
        });
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            menu.request(STICK, 8);
            CpuBlockEntity cpu = cpu(helper, CPU_A);
            WorkOrder order = cpu.book().orders().get(0);
            helper.assertTrue(helper.absolutePos(TerminalWiring.terminalPos(ACCESS)).equals(order.deliverTo()),
                    "the order should remember the terminal, got " + order.deliverTo());
            Waits.when(helper, () -> terminal.outputCount(Items.STICK) == 8
                            && cpu.book().orders().stream().noneMatch(WorkOrder::open), 900,
                    () -> "eight sticks should reach the terminal, had " + terminal.outputCount(Items.STICK), () -> {
                        helper.assertTrue(stockedSticks[0] == 0, "no stick should ever be in the warehouse, saw " + stockedSticks[0]);
                        helper.assertTrue(order.directSent() == 8, "all eight sent directly, got " + order.directSent());
                        helper.assertTrue(menu.stock().requests().isEmpty(), "the request should be done: " + menu.stock().requests());
                        helper.succeed();
                    });
        });
    }

    /** Two sticks for a request: one craft gives four, two go to the terminal and two are put away. */
    @ServusGameTest(timeoutTicks = 1000)
    public static void surplusIsPutAway(GameTestHelper helper) {
        setup(helper, true);
        stock(helper, RACK_A, Items.OAK_PLANKS, 10);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            menu.request(STICK, 2);
            CpuBlockEntity cpu = cpu(helper, CPU_A);
            Waits.when(helper, () -> terminal.outputCount(Items.STICK) == 2 && warehouse(helper).available(STICK) == 2
                            && cpu.book().orders().stream().noneMatch(WorkOrder::open), 900,
                    () -> "two sticks at the terminal and two in storage, had " + terminal.outputCount(Items.STICK)
                            + " and " + warehouse(helper).available(STICK), () -> {
                        helper.assertTrue(terminal.outputCount(Items.STICK) == 2, "the terminal gets only what it asked for");
                        helper.succeed();
                    });
        });
    }

    /** Sticks from planks from logs: the planks go to storage, only the sticks go straight to the terminal. */
    @ServusGameTest(timeoutTicks = 1400)
    public static void chainDeliversOnlyFinal(GameTestHelper helper) {
        setup(helper, true);
        PatternStorageBlockEntity patterns = helper.getBlockEntity(PATTERNS, PatternStorageBlockEntity.class);
        patterns.set(1, StationJobs.planks());
        stock(helper, RACK_A, Items.OAK_LOG, 2);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int[] seen = {0, 0};
        helper.onEachTick(() -> {
            if (helper.getBlockEntity(CONTROLLER, BlockEntity.class) instanceof WarehouseControllerBlockEntity controller) {
                controller.network().ifPresent(network -> {
                    seen[0] = Math.max(seen[0], (int) network.available(PLANKS));
                    seen[1] = Math.max(seen[1], (int) network.available(STICK));
                });
            }
        });
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            menu.request(STICK, 8);
            CpuBlockEntity cpu = cpu(helper, CPU_A);
            Waits.when(helper, () -> terminal.outputCount(Items.STICK) == 8, 1300,
                    () -> "eight sticks should reach the terminal, had " + terminal.outputCount(Items.STICK), () -> {
                        helper.assertTrue(seen[0] > 0, "the planks should pass through storage");
                        helper.assertTrue(seen[1] == 0, "no stick should be in the warehouse, saw " + seen[1]);
                        helper.assertTrue(terminal.outputCount(Items.OAK_PLANKS) == 0, "no planks at the terminal");
                        helper.succeed();
                    });
        });
    }

    /** The terminal link and the direct count are saved with the order. */
    @ServusGameTest(timeoutTicks = 200)
    public static void deliverToSaved(GameTestHelper helper) {
        setup(helper, true);
        stock(helper, RACK_A, Items.OAK_PLANKS, 10);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = TerminalWiring.at(helper, ACCESS);
            new StorageTerminalMenu(1, player.getInventory(), terminal).request(STICK, 8);
            CpuBlockEntity cpu = cpu(helper, CPU_A);
            int id = cpu.book().orders().get(0).id();
            CpuBlockEntity loaded = PersistGameTests.reload(helper, cpu.getBlockPos().subtract(helper.absolutePos(BlockPos.ZERO)));
            WorkOrder order = loaded.book().order(id);
            helper.assertTrue(order != null && helper.absolutePos(TerminalWiring.terminalPos(ACCESS)).equals(order.deliverTo()),
                    "the order should still name the terminal after a reload");
            helper.succeed();
        });
    }
}
