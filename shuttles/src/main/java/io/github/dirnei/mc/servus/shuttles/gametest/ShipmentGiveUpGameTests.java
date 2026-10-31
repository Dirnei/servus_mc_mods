package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.production.ItemSupplier;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.packing.WarehouseSupplier;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalRequests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Supplier tickets and the Packing Station giving up shipments it cannot
 * deliver. A mini warehouse with its terminal; data ducts run east of the
 * terminal's duct to a Crafting Station under the last one, so breaking the
 * middle duct cuts the station from the line while it still takes items.
 */
public final class ShipmentGiveUpGameTests {

    private static final BlockPos ORIGIN = new BlockPos(2, 1, 2);
    private static final BlockPos STATION = new BlockPos(6, 1, 3);
    private static final BlockPos CUT_DUCT = new BlockPos(5, 2, 3);
    private static final ItemKey PLANKS = ItemKey.of(new ItemStack(Items.OAK_PLANKS));
    /** Tests that shorten the refusal timeout run in a batch of their own. */
    private static final String SHORT_BATCH = "shipment_give_up";
    private static final int SHORT_TIMEOUT = 100;
    /** A running delivery notices a removed target at its next retry, the station at its next push after that. */
    private static final int REMOVED_WITHIN = 60;
    /** Tests of the short batch still running; the last one to end puts the default timeout back. */
    private static int shortRunning;

    private ShipmentGiveUpGameTests() {
    }

    private static void shortTimeout() {
        shortRunning++;
        ShuttlesStatsManager.INSTANCE.setRefusedShipmentTimeout(SHORT_TIMEOUT);
    }

    private static void succeedShort(GameTestHelper helper) {
        if (--shortRunning <= 0) {
            shortRunning = 0;
            ShuttlesStatsManager.INSTANCE.setRefusedShipmentTimeout(ShuttlesStatsManager.DEFAULT_REFUSED_SHIPMENT_TIMEOUT);
        }
        helper.succeed();
    }

    /** The mini warehouse with 32 planks, the ducts and a Crafting Station that crafts sticks into full outputs. */
    private static MiniWarehouse build(GameTestHelper helper, boolean stationFull) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).withTerminal().stock(new ItemStack(Items.OAK_PLANKS, 32));
        for (int x = 4; x <= 6; x++) {
            TerminalWiring.dataDuct(helper, new BlockPos(x, 2, 3));
        }
        helper.setBlock(STATION, ServusShuttlesBlocks.CRAFTING_STATION.get().defaultBlockState()
                .setValue(CraftingStationBlock.FACING, Direction.SOUTH));
        CraftingStationBlockEntity station = station(helper);
        StationJobs.sticksJob(station, 64);
        for (int slot = 0; slot < station.outputs().getSlots(); slot++) {
            station.outputs().setStackInSlot(slot, new ItemStack(Items.STICK, 64));
        }
        if (stationFull) {
            for (int slot = 0; slot < station.inputs().getSlots(); slot++) {
                station.inputs().setStackInSlot(slot, new ItemStack(Items.OAK_PLANKS, 64));
            }
        }
        return warehouse;
    }

    private static CraftingStationBlockEntity station(GameTestHelper helper) {
        return helper.getBlockEntity(STATION, CraftingStationBlockEntity.class);
    }

    private static int stationPlanks(GameTestHelper helper) {
        return WarehouseFixtures.count(station(helper).inputs(), Items.OAK_PLANKS);
    }

    private static PackingStationBlockEntity point(GameTestHelper helper, MiniWarehouse warehouse) {
        return helper.getBlockEntity(warehouse.packingPos(), PackingStationBlockEntity.class);
    }

    private static int outgoing(GameTestHelper helper, MiniWarehouse warehouse) {
        return WarehouseFixtures.count(point(helper, warehouse).outgoing(), Items.OAK_PLANKS);
    }

    private static PackingStationBlockEntity.Shipment ship(GameTestHelper helper, MiniWarehouse warehouse, int amount) {
        WarehouseNetwork.Shipping shipping = warehouse.network().ship(helper.absolutePos(STATION), PLANKS, amount);
        helper.assertTrue(shipping.shipment().isPresent(), "the planks shipment should be planned, refused " + shipping.refusal());
        return shipping.shipment().get();
    }

    /** A supplier ticket is open while its shipment runs and closes once every item arrived; an unknown ticket is not open. */
    @ServusGameTest(timeoutTicks = 800)
    public static void supplierTicketClosesWhenDelivered(GameTestHelper helper) {
        MiniWarehouse warehouse = build(helper, false);
        helper.runAfterDelay(10, () -> {
            ItemSupplier supplier = new WarehouseSupplier(warehouse.controller());
            ItemSupplier.Supply supply = supplier.supply(helper.absolutePos(STATION), new ItemStack(Items.OAK_PLANKS), 16);
            helper.assertTrue(supply.amount() == 16, "16 planks are planned, got " + supply.amount());
            helper.assertTrue(supplier.open(supply.ticket()), "the ticket is open while the shipment runs");
            helper.assertFalse(supplier.open(-5L), "an unknown ticket is not open");
            Waits.when(helper, () -> stationPlanks(helper) == 16 && !supplier.open(supply.ticket()), 700,
                    () -> "16 planks should arrive and the ticket close, got " + stationPlanks(helper)
                            + ", open " + supplier.open(supply.ticket()), helper::succeed);
        });
    }

    /** Cancelling the ticket of a refused shipment ends it and puts its picked planks back into storage. */
    @ServusGameTest(timeoutTicks = 1000)
    public static void cancelledRefusedShipmentReturnsItems(GameTestHelper helper) {
        MiniWarehouse warehouse = build(helper, true);
        helper.runAfterDelay(10, () -> {
            ItemSupplier supplier = new WarehouseSupplier(warehouse.controller());
            ItemSupplier.Supply supply = supplier.supply(helper.absolutePos(STATION), new ItemStack(Items.OAK_PLANKS), 16);
            helper.assertTrue(supply.amount() == 16, "16 planks are planned, got " + supply.amount());
            Waits.when(helper, () -> outgoing(helper, warehouse) == 16, 400,
                    () -> "the planks should wait in the To network buffer, got " + outgoing(helper, warehouse), () -> {
                supplier.cancel(supply.ticket());
                helper.assertFalse(supplier.open(supply.ticket()), "a cancelled ticket is not open");
                Waits.when(helper, () -> outgoing(helper, warehouse) == 0 && warehouse.rackCount(Items.OAK_PLANKS) == 32, 500,
                        () -> "the planks should go back into the rack, To network " + outgoing(helper, warehouse)
                                + ", rack " + warehouse.rackCount(Items.OAK_PLANKS), helper::succeed);
            });
        });
    }

    /** A machine that refuses its planks past the timeout ends the shipment short, and the planks go back into storage. */
    @ServusGameTest(timeoutTicks = 2400, batch = SHORT_BATCH)
    public static void refusedShipmentEndsAfterTimeoutAndReturnsItems(GameTestHelper helper) {
        shortTimeout();
        MiniWarehouse warehouse = build(helper, true);
        helper.runAfterDelay(10, () -> {
            PackingStationBlockEntity.Shipment shipment = ship(helper, warehouse, 16);
            Waits.when(helper, () -> shipment.ended() && outgoing(helper, warehouse) == 0 && warehouse.rackCount(Items.OAK_PLANKS) == 32,
                    2300, () -> "the refused shipment should end and its planks return, ended " + shipment.ended() + ", To network "
                            + outgoing(helper, warehouse) + ", rack " + warehouse.rackCount(Items.OAK_PLANKS), () -> {
                helper.assertTrue(shipment.delivered() == 0, "nothing was delivered, got " + shipment.delivered());
                helper.assertTrue(stationPlanks(helper) == station(helper).inputs().getSlots() * 64, "the station kept only its own planks");
                succeedShort(helper);
            });
        });
    }

    /** A terminal request whose Output slot stays full past the timeout still waits, picks locked, and arrives once there is room. */
    @ServusGameTest(timeoutTicks = 1600, batch = SHORT_BATCH)
    public static void terminalFullSlotWaitsPastTimeout(GameTestHelper helper) {
        shortTimeout();
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).withTerminal()
                .stock(new ItemStack(Items.COPPER_INGOT, 64), new ItemStack(Items.COPPER_INGOT, 64));
        helper.runAfterDelay(10, () -> {
            StorageTerminalBlockEntity terminal = warehouse.terminal();
            for (int slot = 1; slot < terminal.output().getSlots(); slot++) {
                terminal.output().setStackInSlot(slot, new ItemStack(Items.DIRT));
            }
            String refusal = TerminalRequests.request(helper.getLevel(), terminal, ItemKey.of(new ItemStack(Items.COPPER_INGOT)), 128);
            helper.assertTrue(refusal.isEmpty(), "the request should be made, refused " + refusal);
            Waits.when(helper, () -> terminal.outputCount(Items.COPPER_INGOT) == 64, 400,
                    () -> "the first 64 copper should arrive, got " + terminal.outputCount(Items.COPPER_INGOT), () ->
            helper.runAfterDelay(SHORT_TIMEOUT * 4, () -> {
                helper.assertFalse(terminal.requests().isEmpty(), "the request is still open past the timeout");
                helper.assertTrue(warehouse.rackCount(Items.COPPER_INGOT) == 64, "the other 64 copper wait in the rack, got "
                        + warehouse.rackCount(Items.COPPER_INGOT));
                helper.assertTrue(warehouse.network().available(ItemKey.of(new ItemStack(Items.COPPER_INGOT))) == 0,
                        "the waiting copper stays locked");
                terminal.output().setStackInSlot(0, ItemStack.EMPTY);
                Waits.when(helper, () -> terminal.outputCount(Items.COPPER_INGOT) == 64 && warehouse.rackCount(Items.COPPER_INGOT) == 0, 600,
                        () -> "the other 64 copper should arrive after the slot was emptied, got " + terminal.outputCount(Items.COPPER_INGOT)
                                + ", rack " + warehouse.rackCount(Items.COPPER_INGOT), () -> succeedShort(helper));
            }));
        });
    }

    /** A broken target ends its shipment at the next push, and the planks waiting for it go back into storage. */
    @ServusGameTest(timeoutTicks = 1000)
    public static void removedTargetEndsAtOnce(GameTestHelper helper) {
        MiniWarehouse warehouse = build(helper, true);
        helper.runAfterDelay(10, () -> {
            PackingStationBlockEntity.Shipment shipment = ship(helper, warehouse, 16);
            Waits.when(helper, () -> outgoing(helper, warehouse) == 16, 400,
                    () -> "the planks should wait in the To network buffer, got " + outgoing(helper, warehouse), () -> {
                helper.setBlock(STATION, Blocks.AIR.defaultBlockState());
                Waits.when(helper, shipment::ended, REMOVED_WITHIN,
                        () -> "the shipment should end at the next push", () ->
                Waits.when(helper, () -> outgoing(helper, warehouse) == 0 && warehouse.rackCount(Items.OAK_PLANKS) == 32, 500,
                        () -> "the planks should go back into the rack, To network " + outgoing(helper, warehouse)
                                + ", rack " + warehouse.rackCount(Items.OAK_PLANKS), helper::succeed));
            });
        });
    }

    /** A duct on the route broken and placed again within the timeout keeps the shipment, and its planks arrive. */
    @ServusGameTest(timeoutTicks = 1000, batch = SHORT_BATCH)
    public static void ductRebuiltWithinTimeoutDelivers(GameTestHelper helper) {
        shortTimeout();
        MiniWarehouse warehouse = build(helper, false);
        helper.runAfterDelay(10, () -> {
            PackingStationBlockEntity.Shipment shipment = ship(helper, warehouse, 16);
            helper.destroyBlock(CUT_DUCT);
            helper.runAfterDelay(SHORT_TIMEOUT / 2, () -> {
                helper.assertFalse(shipment.ended(), "the cut shipment waits");
                helper.assertTrue(shipment.refusedSince() >= 0, "the refusal clock runs while cut");
                TerminalWiring.dataDuct(helper, CUT_DUCT);
                Waits.when(helper, () -> stationPlanks(helper) == 16 && shipment.ended() && shipment.delivered() == 16, 800,
                        () -> "16 planks should arrive after the duct is back, got " + stationPlanks(helper), () -> succeedShort(helper));
            });
        });
    }

    /** A target cut from the line for longer than the timeout ends its shipment short. */
    @ServusGameTest(timeoutTicks = 1000, batch = SHORT_BATCH)
    public static void cutPastTimeoutEndsShort(GameTestHelper helper) {
        shortTimeout();
        MiniWarehouse warehouse = build(helper, false);
        helper.runAfterDelay(10, () -> {
            PackingStationBlockEntity.Shipment shipment = ship(helper, warehouse, 16);
            helper.destroyBlock(CUT_DUCT);
            Waits.when(helper, () -> shipment.ended() && outgoing(helper, warehouse) == 0 && warehouse.rackCount(Items.OAK_PLANKS) == 32, 900,
                    () -> "the cut shipment should end and its planks return, ended " + shipment.ended() + ", To network "
                            + outgoing(helper, warehouse) + ", rack " + warehouse.rackCount(Items.OAK_PLANKS), () -> {
                helper.assertTrue(stationPlanks(helper) == 0, "nothing reached the cut station");
                succeedShort(helper);
            });
        });
    }

    /** Room in between restarts the refusal clock: the shipment ends only after a new refusal lasted past the timeout. */
    @ServusGameTest(timeoutTicks = 3200, batch = SHORT_BATCH)
    public static void roomResetsRefusalClock(GameTestHelper helper) {
        shortTimeout();
        MiniWarehouse warehouse = build(helper, true);
        helper.runAfterDelay(10, () -> {
            PackingStationBlockEntity.Shipment shipment = ship(helper, warehouse, 16);
            Waits.when(helper, () -> shipment.refusedSince() >= 0, 1000, () -> "the station should refuse the planks", () -> {
                long first = shipment.refusedSince();
                CraftingStationBlockEntity station = station(helper);
                station.inputs().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 60));
                Waits.when(helper, () -> shipment.delivered() == 4, 1000,
                        () -> "the station should take 4 planks, delivered " + shipment.delivered(), () -> {
                    helper.assertFalse(shipment.ended(), "room keeps the shipment open");
                    helper.assertTrue(shipment.refusedSince() == -1 || shipment.refusedSince() > first,
                            "the clock restarted, first " + first + ", now " + shipment.refusedSince());
                    Waits.when(helper, () -> shipment.ended() && outgoing(helper, warehouse) == 0, 1100,
                            () -> "the shipment should end after the new refusal, ended " + shipment.ended(), () -> {
                        helper.assertTrue(shipment.delivered() == 4, "4 planks were delivered, got " + shipment.delivered());
                        succeedShort(helper);
                    });
                });
            });
        });
    }
}
