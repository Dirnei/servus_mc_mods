package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalRequests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Shipments out of storage send only as far as their target grants: a
 * request owns as many Output slots as it fills, a full slot holds up only
 * its own shipment and nothing is picked for it, and a target without an
 * endpoint that stays full stalls without blocking the station. A mini
 * warehouse with its terminal; data ducts run east of the terminal's duct
 * over a Crafting Station.
 */
public final class EgressGameTests {

    private static final BlockPos ORIGIN = new BlockPos(2, 1, 2);
    private static final BlockPos STATION = new BlockPos(4, 1, 3);
    private static final ItemKey COPPER = ItemKey.of(new ItemStack(Items.COPPER_INGOT));
    private static final ItemKey IRON = ItemKey.of(new ItemStack(Items.IRON_INGOT));

    private EgressGameTests() {
    }

    private static PackingStationBlockEntity point(GameTestHelper helper, MiniWarehouse warehouse) {
        return helper.getBlockEntity(warehouse.packingPos(), PackingStationBlockEntity.class);
    }

    private static int outgoing(PackingStationBlockEntity point, Item item) {
        return WarehouseFixtures.count(point.outgoing(), item);
    }

    private static void request(GameTestHelper helper, StorageTerminalBlockEntity terminal, ItemKey key, int amount) {
        String refusal = TerminalRequests.request(helper.getLevel(), terminal, key, amount);
        helper.assertTrue(refusal.isEmpty(), "the request for " + amount + " " + key + " should be made, refused " + refusal);
    }

    /** With free slots a request for 128 copper owns two slots and all 128 arrive without the player taking any. */
    @ServusGameTest(timeoutTicks = 800)
    public static void twoStacksInTwoSlots(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).withTerminal()
                .stock(new ItemStack(Items.COPPER_INGOT, 64), new ItemStack(Items.COPPER_INGOT, 64));
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = warehouse.terminal();
            request(helper, terminal, COPPER, 128);
            helper.assertTrue(terminal.requests().get(0).slots.size() == 2, "the request should own two slots, owns "
                    + terminal.requests().get(0).slots);
            Waits.when(helper, () -> terminal.outputCount(Items.COPPER_INGOT) == 128, 700,
                    () -> "128 copper should arrive, got " + terminal.outputCount(Items.COPPER_INGOT), helper::succeed);
        });
    }

    /**
     * A request for 128 copper with one free slot fills it with 64; the other
     * 64 stay in the rack and locked, nothing is picked for the full slot, a
     * request for iron behind it still arrives, items nobody wants leave the
     * To network buffer, and the copper follows once the slot is emptied.
     */
    @ServusGameTest(timeoutTicks = 1600)
    public static void fullSlotHoldsUpOnlyItsShipment(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).withTerminal()
                .stock(new ItemStack(Items.COPPER_INGOT, 64), new ItemStack(Items.COPPER_INGOT, 64), new ItemStack(Items.IRON_INGOT, 16));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = warehouse.terminal();
            for (int slot = 1; slot < terminal.output().getSlots(); slot++) {
                terminal.output().setStackInSlot(slot, new ItemStack(Items.DIRT));
            }
            request(helper, terminal, COPPER, 128);
            helper.assertTrue(terminal.requests().get(0).slots.size() == 1, "only one slot is free");
            PackingStationBlockEntity point = point(helper, warehouse);
            StorageTerminalMenu menu = new StorageTerminalMenu(1, player.getInventory(), terminal);
            Waits.when(helper, () -> terminal.outputCount(Items.COPPER_INGOT) == 64, 400,
                    () -> "the first 64 copper should arrive, got " + terminal.outputCount(Items.COPPER_INGOT), () ->
            helper.runAfterDelay(200, () -> {
                helper.assertTrue(outgoing(point, Items.COPPER_INGOT) == 0, "no copper should wait in the To network buffer, got "
                        + outgoing(point, Items.COPPER_INGOT));
                helper.assertTrue(warehouse.rackCount(Items.COPPER_INGOT) == 64, "the other 64 copper should stay in the rack, got "
                        + warehouse.rackCount(Items.COPPER_INGOT));
                helper.assertTrue(TerminalWiring.stockOf(menu, Items.COPPER_INGOT) == 0, "the waiting copper is not requestable");
                point.outgoing().setStackInSlot(8, new ItemStack(Items.SAND, 10));
                terminal.output().setStackInSlot(1, ItemStack.EMPTY);
                request(helper, terminal, IRON, 16);
                Waits.when(helper, () -> terminal.outputCount(Items.IRON_INGOT) == 16 && outgoing(point, Items.SAND) == 0, 400,
                        () -> "16 iron should arrive behind the full copper slot and the sand leave To network, iron "
                                + terminal.outputCount(Items.IRON_INGOT) + ", sand " + outgoing(point, Items.SAND), () -> {
                    terminal.output().setStackInSlot(0, ItemStack.EMPTY);
                    Waits.when(helper, () -> terminal.outputCount(Items.COPPER_INGOT) == 64 && warehouse.rackCount(Items.COPPER_INGOT) == 0, 500,
                            () -> "the other 64 copper should arrive after the slot was emptied, got " + terminal.outputCount(Items.COPPER_INGOT)
                                    + ", rack " + warehouse.rackCount(Items.COPPER_INGOT), helper::succeed);
                });
            }));
        });
    }

    /** A Crafting Station that stays full stalls its planks shipment, and a request for iron at the same station still arrives. */
    @ServusGameTest(timeoutTicks = 1200)
    public static void stalledTargetDoesNotBlock(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).withTerminal()
                .stock(new ItemStack(Items.OAK_PLANKS, 32), new ItemStack(Items.IRON_INGOT, 16));
        for (int x = 4; x <= 6; x++) {
            TerminalWiring.dataDuct(helper, new BlockPos(x, 2, 3));
        }
        helper.setBlock(STATION, ServusShuttlesBlocks.CRAFTING_STATION.get().defaultBlockState());
        CraftingStationBlockEntity station = helper.getBlockEntity(STATION, CraftingStationBlockEntity.class);
        StationJobs.sticksJob(station, 64);
        for (int slot = 0; slot < station.outputs().getSlots(); slot++) {
            station.outputs().setStackInSlot(slot, new ItemStack(Items.STICK, 64));
        }
        for (int slot = 0; slot < station.inputs().getSlots(); slot++) {
            station.inputs().setStackInSlot(slot, new ItemStack(Items.OAK_PLANKS, 64));
        }
        helper.runAfterDelay(10, () -> {
            WarehouseNetwork.Shipping shipping = warehouse.network().ship(helper.absolutePos(STATION), ItemKey.of(new ItemStack(Items.OAK_PLANKS)), 16);
            helper.assertTrue(shipping.shipment().isPresent(), "the planks shipment should be planned, refused " + shipping.refusal());
            PackingStationBlockEntity point = point(helper, warehouse);
            Waits.when(helper, () -> outgoing(point, Items.OAK_PLANKS) == 16, 400,
                    () -> "the planks should wait in the To network buffer, got " + outgoing(point, Items.OAK_PLANKS), () -> {
                StorageTerminalBlockEntity terminal = warehouse.terminal();
                request(helper, terminal, IRON, 16);
                Waits.when(helper, () -> terminal.outputCount(Items.IRON_INGOT) == 16, 400,
                        () -> "16 iron should arrive while the planks wait, got " + terminal.outputCount(Items.IRON_INGOT), helper::succeed);
            });
        });
    }
}
