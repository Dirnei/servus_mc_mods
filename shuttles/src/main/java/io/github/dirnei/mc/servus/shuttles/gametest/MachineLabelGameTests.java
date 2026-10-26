package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.MachineNameContract;
import io.github.dirnei.mc.servus.foundation.gametest.PlayerClicks;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuOrders;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.CraftingStationMenu;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleGarageMenu;
import io.github.dirnei.mc.servus.shuttles.order.OrderBook;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Labels of named machines in messages, menus and Data Potato reports, and the naming rules of every Shuttles kind. */
public final class MachineLabelGameTests {

    private static final int WAIT = 120;

    private MachineLabelGameTests() {
    }

    private static Object[] labelArgs(GameTestHelper helper, Component label, String key) {
        helper.assertTrue(label.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key),
                "expected the label " + key + ", got " + label);
        return ((TranslatableContents) label.getContents()).getArgs();
    }

    /** Asserts "<name> (<kind> #n)". */
    private static void assertNamed(GameTestHelper helper, Component label, String name, String kindKey, int number) {
        Object[] args = labelArgs(helper, label, NamedMachine.NAMED_KEY);
        helper.assertTrue(args[0].equals(name), "the label names " + name + ", got " + args[0]);
        Object[] numbered = labelArgs(helper, (Component) args[1], kindKey);
        helper.assertTrue(numbered[0].equals(number), "the label numbers #" + number + ", got " + numbered[0]);
    }

    /** Every Shuttles machine kind follows the same naming rules. */
    @ServusGameTest(timeoutTicks = 40)
    public static void everyShuttlesKindFollowsTheRules(GameTestHelper helper) {
        MachineNameContract.checkAll(helper, ServusShuttlesIds.MOD_ID);
    }

    /** Crafting Station "Sticks" renamed in its GUI: the CPU's producer list names it "Sticks (Crafting Station #1)", the summary starts with CPU #1. */
    @ServusGameTest(timeoutTicks = 300)
    public static void producerAndCpuNamedByLabel(GameTestHelper helper) {
        CpuLine.setup(helper, true);
        CraftingStationBlockEntity station = helper.getBlockEntity(CpuLine.STATION, CraftingStationBlockEntity.class);
        CpuBlockEntity cpu = CpuLine.cpu(helper, CpuLine.CPU_A);
        Waits.when(helper, () -> station.number() > 0 && cpu.number() > 0, WAIT, () -> "the station and the CPU should get numbers", () -> {
            ServerPlayer player = PlayerClicks.player(helper, CpuLine.STATION, ItemStack.EMPTY);
            new CraftingStationMenu(1, player.getInventory(), station).renameMachine(player, "Sticks");
            List<OrderBook.Producer> producers = CpuOrders.producers(helper.getLevel(), DataLines.at(helper, CpuLine.STATION));
            OrderBook.Producer producer = producers.stream().filter(found -> found.pos().equals(station.getBlockPos())).findFirst().orElseThrow();
            assertNamed(helper, producer.name(), "Sticks", "machine.servus_shuttles.crafting_station", 1);
            Object[] summary = labelArgs(helper, cpu.summary(), "message.servus_shuttles.cpu.summary");
            Object[] cpuLabel = labelArgs(helper, (Component) summary[0], "machine.servus_shuttles.crafting_cpu");
            helper.assertTrue(cpuLabel[0].equals(1), "the summary starts with CPU #1, got " + cpuLabel[0]);
            helper.succeed();
        });
    }

    /** The Data Potato on a numbered garage and on a named Crafting Station prints their labels and opens no GUI. */
    @ServusGameTest(timeoutTicks = 300)
    public static void potatoNamesGarageAndCraftingStation(GameTestHelper helper) {
        CpuLine.setup(helper, false);
        CraftingStationBlockEntity station = helper.getBlockEntity(CpuLine.STATION, CraftingStationBlockEntity.class);
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(CpuLine.GARAGE, ShuttleGarageBlockEntity.class);
        Waits.when(helper, () -> station.number() > 0 && garage.number() > 0, WAIT, () -> "the station and the garage should get numbers", () -> {
            station.rename("Sticks");
            List<Component> chat = new ArrayList<>();
            ServerPlayer player = ElevatorGameTests.potatoPlayer(helper, CpuLine.GARAGE, chat);
            PlayerClicks.use(helper, player, CpuLine.GARAGE);
            helper.assertTrue(chat.size() == 1, "one line for the garage, got " + chat);
            labelArgs(helper, chat.get(0), "machine.servus_shuttles.shuttle_garage");
            helper.assertTrue(player.containerMenu == player.inventoryMenu, "the garage GUI does not open");
            chat.clear();
            ServerPlayer near = ElevatorGameTests.potatoPlayer(helper, CpuLine.STATION, chat);
            PlayerClicks.use(helper, near, CpuLine.STATION);
            helper.assertTrue(chat.size() == 1, "one line for the station, got " + chat);
            assertNamed(helper, chat.get(0), "Sticks", "machine.servus_shuttles.crafting_station", 1);
            helper.assertTrue(near.containerMenu == near.inventoryMenu, "the station GUI does not open");
            helper.succeed();
        });
    }

    /** The garage GUI renames the garage; its title reads the new label. */
    @ServusGameTest(timeoutTicks = 300)
    public static void garageMenuRenames(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, new BlockPos(2, 1, 4));
        ShuttleGarageBlockEntity garage = helper.getBlockEntity(warehouse.garagePos(), ShuttleGarageBlockEntity.class);
        Waits.when(helper, () -> garage.number() > 0, WAIT, () -> "the garage should get a number", () -> {
            ServerPlayer player = PlayerClicks.player(helper, warehouse.garagePos(), ItemStack.EMPTY);
            new ShuttleGarageMenu(1, player.getInventory(), garage).renameMachine(player, "Main Garage");
            assertNamed(helper, garage.getDisplayName(), "Main Garage", "machine.servus_shuttles.shuttle_garage", 1);
            helper.succeed();
        });
    }
}
