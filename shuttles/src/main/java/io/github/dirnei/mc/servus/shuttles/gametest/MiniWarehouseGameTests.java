package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** The shared smallest warehouse serves a request and puts a stack away. */
public final class MiniWarehouseGameTests {

    private static final BlockPos ORIGIN = new BlockPos(3, 1, 4);

    private MiniWarehouseGameTests() {
    }

    /** A stack of cobblestone in the rack lands in the terminal's output slots by shuttle. */
    @ServusGameTest(timeoutTicks = 300)
    public static void miniWarehouseServesARequest(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).withTerminal().stock(new ItemStack(Items.COBBLESTONE, 64));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = warehouse.terminal();
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            helper.assertTrue(menu.stock().online(), "online");
            menu.request(ItemKey.of(new ItemStack(Items.COBBLESTONE)), 64);
            helper.assertTrue(menu.stock().message().isEmpty(), "the request should go through, got " + menu.stock().message());
            Waits.when(helper, () -> terminal.outputCount(Items.COBBLESTONE) == 64, MiniWarehouse.TRIP_TICKS * 2,
                    () -> "the shuttle should bring 64 cobblestone, output has " + terminal.outputCount(Items.COBBLESTONE), () -> {
                        helper.assertTrue(warehouse.rackCount(Items.COBBLESTONE) == 0, "the rack should be empty");
                        helper.succeed();
                    });
        });
    }

    /** Cobblestone in an Inbound Station on the rail ends in the rack. */
    @ServusGameTest(timeoutTicks = 300)
    public static void miniWarehousePutsAway(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        helper.runAfterDelay(10, () -> {
            ItemStack left = Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 40), false);
            helper.assertTrue(left.isEmpty(), "the intake should take the stack");
            Waits.when(helper, () -> warehouse.rackCount(Items.COBBLESTONE) == 40, MiniWarehouse.TRIP_TICKS * 2,
                    () -> "the shuttle should put 40 cobblestone into the rack, it has " + warehouse.rackCount(Items.COBBLESTONE), () -> {
                        helper.assertTrue(station.isEmpty(), "the station buffer should be empty");
                        helper.succeed();
                    });
        });
    }
}
