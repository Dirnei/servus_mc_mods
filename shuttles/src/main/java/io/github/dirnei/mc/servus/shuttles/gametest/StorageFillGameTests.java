package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryMode;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLock;
import io.github.dirnei.mc.servus.shuttles.menu.WarehouseControllerMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.StorageRow;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.storage.StorageFill;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneDefinition;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** How full a warehouse is: rack slots in use with locks, zones, Storage gantries, Fluid Racks, and the controller GUI's row. */
public final class StorageFillGameTests {

    private static final BlockPos ORIGIN = new BlockPos(3, 1, 4);
    private static final BlockPos CONTROLLER = new BlockPos(4, 1, 1);
    private static final BlockPos RAIL = new BlockPos(4, 1, 4);
    private static final BlockPos SECOND = new BlockPos(4, 1, 7);
    private static final int FOREIGN_JOB = Integer.MAX_VALUE;

    private StorageFillGameTests() {
    }

    /**
     * Three stacks and one IN locked empty slot make four slots in use; the
     * station buffer does not count, and the zoned rack has its own row,
     * also in the controller GUI's storage row.
     */
    @ServusGameTest(timeoutTicks = 100)
    public static void rackSlotsInUseCountLocksNotStations(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN, false).stock(new ItemStack(Items.COBBLESTONE, 64),
                new ItemStack(Items.DIRT, 10), new ItemStack(Items.BREAD, 5));
        HeavyRackBlockEntity rack = warehouse.rack();
        int slots = rack.items().getSlots();
        rack.items().lock(slots - 1, new SlotLock(SlotLock.Kind.IN, ItemKey.of(new ItemStack(Items.STONE)), 64, FOREIGN_JOB));
        rack.setZone(new ZoneDefinition("Ores", ZoneFilter.EMPTY));
        InboundStationBlockEntity station = warehouse.withInbound();
        helper.assertTrue(Transfer.insertItem(station.intake(), new ItemStack(Items.GRAVEL, 64), false).isEmpty(), "the station takes gravel");
        Waits.when(helper, () -> fill(warehouse.network()).items().used() == 4, 60,
                () -> "four slots should be in use, got " + fill(warehouse.network()).items(), () -> {
            StorageFill.Report report = fill(warehouse.network());
            helper.assertTrue(report.items().total() == slots && report.bays() == 1, "only the rack's slots count, got " + report.items());
            helper.assertTrue(report.zones().size() == 1 && report.zones().get(0).name().equals("Ores") && report.zones().get(0).used() == 4,
                    "one Ores row with 4 slots, got " + report.zones());
            StorageRow row = WarehouseControllerMenu.status(warehouse.controller()).storage();
            helper.assertTrue(row.used() == 4 && row.total() == slots && row.bays() == 1, "the controller GUI shows 4 of " + slots + ", got " + row);
            helper.succeed();
        });
    }

    /** A full rack is a full zone row and a full warehouse. */
    @ServusGameTest(timeoutTicks = 100)
    public static void fullRackIsFull(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN, false);
        WarehouseFixtures.fill(warehouse.rack(), Items.COBBLESTONE);
        Waits.when(helper, () -> fill(warehouse.network()).items().full(), 60,
                () -> "the rack should be full, got " + fill(warehouse.network()).items(), () -> {
            helper.assertTrue(fill(warehouse.network()).zones().get(0).full() && fill(warehouse.network()).zones().get(0).unzoned(),
                    "the unzoned row is full, got " + fill(warehouse.network()).zones());
            helper.succeed();
        });
    }

    /** A Storage gantry with a Capacity Upgrade counts its tanks; an Import gantry's water does not count. */
    @ServusGameTest(timeoutTicks = 100)
    public static void storageGantriesCountImportDoesNot(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = WarehouseFixtures.controller(helper, CONTROLLER);
        GantryFixtures.railAlongZ(helper, RAIL.getX(), 2, 9);
        FluidGantryBlockEntity storage = GantryFixtures.place(helper, RAIL);
        storage.upgrades().setStackInSlot(0, new ItemStack(ServusFoundationItems.CAPACITY_UPGRADE.get()));
        storage.tank(0).fill(new FluidStack(Fluids.WATER, 3000), false);
        FluidGantryBlockEntity imports = GantryFixtures.place(helper, SECOND);
        imports.setMode(GantryMode.IMPORT);
        imports.tank(0).fill(new FluidStack(Fluids.WATER, 5000), false);
        long capacity = (long) storage.tank(0).getCapacity() + storage.tank(1).getCapacity();
        Waits.when(helper, () -> {
            StorageFill.Report report = fill(controller.network().orElseThrow());
            return report.fluid() == 3000 && report.fluidCapacity() == capacity;
        }, 60, () -> "3000 of " + capacity + " mB should count, got " + fill(controller.network().orElseThrow()), () -> {
            WarehouseNetwork network = controller.network().orElseThrow();
            helper.assertTrue(network.storageGantries().size() == 1, "only the Storage gantry counts, got " + network.storageGantries());
            helper.assertTrue(capacity > 2L * imports.tank(0).getCapacity(), "the Capacity Upgrade raises the Storage gantry's tanks");
            helper.assertTrue(WarehouseControllerMenu.status(controller).storage().gantries() == 1, "the controller GUI counts one gantry");
            helper.succeed();
        });
    }

    /** A docked three bay Fluid Rack section counts its water against three bays' capacity, and the controller GUI counts it. */
    @ServusGameTest(timeoutTicks = 100)
    public static void fluidRackSectionCounts(GameTestHelper helper) {
        WarehouseControllerBlockEntity controller = WarehouseFixtures.controller(helper, CONTROLLER);
        GantryFixtures.railAlongZ(helper, RAIL.getX(), 2, 9);
        BlockPos[] bays = {new BlockPos(3, 1, 4), new BlockPos(3, 1, 5), new BlockPos(3, 1, 6)};
        FluidRackFixtures.row(helper, Direction.EAST, bays);
        helper.runAfterDelay(2, () -> {
            FluidRackFixtures.fill(helper, bays[0], Fluids.WATER, 12000);
            long capacity = 3L * ShuttlesStatsManager.INSTANCE.fluidRackCapacityPerBay();
            Waits.when(helper, () -> {
                StorageFill.Report report = fill(controller.network().orElseThrow());
                return report.fluid() == 12000 && report.fluidCapacity() == capacity;
            }, 60, () -> "12000 of " + capacity + " mB should count, got " + fill(controller.network().orElseThrow()), () -> {
                StorageRow row = WarehouseControllerMenu.status(controller).storage();
                helper.assertTrue(row.fluidRacks() == 1 && row.gantries() == 0, "the controller GUI counts one section and no gantry, got " + row);
                helper.succeed();
            });
        });
    }

    private static StorageFill.Report fill(WarehouseNetwork network) {
        return network.storageFill();
    }
}
