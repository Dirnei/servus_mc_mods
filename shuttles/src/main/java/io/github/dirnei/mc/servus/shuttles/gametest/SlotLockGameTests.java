package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.lock.LockedSlots;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLock;
import io.github.dirnei.mc.servus.shuttles.menu.HeavyRackMenu;
import io.github.dirnei.mc.servus.shuttles.menu.InboundStationMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/** Slot locks: what they close, how jobs place and release them, and how they survive reloads and broken blocks. */
public final class SlotLockGameTests {

    private static final BlockPos ORIGIN = new BlockPos(3, 1, 4);
    private static final BlockPos LONE_RACK = new BlockPos(2, 1, 2);
    private static final BlockPos LONE_STATION = new BlockPos(6, 1, 6);
    /** A job id no fleet knows. */
    private static final int FOREIGN_JOB = Integer.MAX_VALUE;
    private static final ItemKey COBBLE = ItemKey.of(new ItemStack(Items.COBBLESTONE));
    private static final ItemKey IRON = ItemKey.of(new ItemStack(Items.IRON_INGOT));

    private SlotLockGameTests() {
    }

    /** An Outbound Station south of the mini warehouse's first rail, facing south. */
    private static OutboundStationBlockEntity outbound(GameTestHelper helper, MiniWarehouse warehouse) {
        return WarehouseFixtures.outbound(helper, warehouse.railPos(0).south(), Direction.SOUTH);
    }

    private static int locks(LockedSlots slots) {
        return slots.locked().size();
    }

    /** A locked rack slot refuses a hand click, shift clicks into it and a hopper; free slots still work. */
    @ServusGameTest(timeoutTicks = 100)
    public static void lockedSlotIsClosedToHandsAndHoppers(GameTestHelper helper) {
        HeavyRackBlockEntity rack = WarehouseFixtures.rack(helper, LONE_RACK, Direction.SOUTH);
        rack.items().insertItem(0, new ItemStack(Items.BREAD, 20), false);
        rack.items().lock(0, new SlotLock(SlotLock.Kind.OUT, ItemKey.of(new ItemStack(Items.BREAD)), 20, FOREIGN_JOB));
        rack.items().lock(1, new SlotLock(SlotLock.Kind.IN, COBBLE, 64, FOREIGN_JOB));
        helper.setBlock(LONE_RACK.above(), Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopper = helper.getBlockEntity(LONE_RACK.above(), HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        HeavyRackMenu menu = HeavyRackMenu.forSection(1, player.getInventory(), rack);
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty() && rack.items().getStackInSlot(0).getCount() == 20,
                "a click on the OUT locked slot takes nothing");
        player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 10));
        menu.quickMoveStack(player, menu.rackSlots() + 27);
        helper.assertTrue(rack.items().getStackInSlot(1).isEmpty() && rack.items().getStackInSlot(2).is(Items.COBBLESTONE),
                "shift click passes the IN locked slot");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(rack.items().getStackInSlot(1).isEmpty(), "the hopper does not fill the locked slot");
            helper.assertTrue(WarehouseFixtures.count(rack, Items.COBBLESTONE) == 15, "the hopper fills free slots, rack has "
                    + WarehouseFixtures.count(rack, Items.COBBLESTONE));
            helper.assertTrue(Transfer.extractItem(rack.items(), 0, 64, false).isEmpty(), "nothing takes from the locked slot");
            helper.succeed();
        });
    }

    /** A Pull side does not top up a slot a pipe grant holds and loses nothing when the buffer takes less than offered. */
    @ServusGameTest(timeoutTicks = 100)
    public static void pullSideRespectsLocks(GameTestHelper helper) {
        InboundStationBlockEntity station = WarehouseFixtures.inbound(helper, LONE_STATION, Direction.NORTH);
        helper.setBlock(LONE_STATION.south(), Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(LONE_STATION.south(), ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.BREAD, 64));
        station.sideConfig().set(station.relativeSide(Direction.SOUTH), SideMode.PULL);
        station.buffer().insertItem(0, new ItemStack(Items.BREAD, 20), false);
        station.buffer().lock(0, new SlotLock(SlotLock.Kind.IN, ItemKey.of(new ItemStack(Items.BREAD)), 64, FOREIGN_JOB));
        for (int slot = 2; slot < station.buffer().getSlots(); slot++) {
            station.buffer().insertItem(slot, new ItemStack(Items.STONE, 64), false);
        }
        station.buffer().insertItem(1, new ItemStack(Items.BREAD, 54), false);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(station.buffer().getStackInSlot(0).getCount() == 20, "the locked slot keeps 20 bread");
            helper.assertTrue(station.buffer().getStackInSlot(1).getCount() == 64, "the free slot is topped up to a stack");
            helper.assertTrue(chest.countItem(Items.BREAD) == 54, "the 54 bread the buffer had no room for stay in the chest, has "
                    + chest.countItem(Items.BREAD));
            helper.succeed();
        });
    }

    /** A Pull side tops up an Inbound slot planned for a put-away up to a stack; only the rest goes into a free slot. */
    @ServusGameTest(timeoutTicks = 100)
    public static void pullSideTopsUpAPlannedSlot(GameTestHelper helper) {
        InboundStationBlockEntity station = WarehouseFixtures.inbound(helper, LONE_STATION, Direction.NORTH);
        helper.setBlock(LONE_STATION.south(), Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(LONE_STATION.south(), ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.BREAD, 64));
        station.buffer().insertItem(0, new ItemStack(Items.BREAD, 20), false);
        station.buffer().lock(0, new SlotLock(SlotLock.Kind.OUT, ItemKey.of(new ItemStack(Items.BREAD)), 20, FOREIGN_JOB));
        station.sideConfig().set(station.relativeSide(Direction.SOUTH), SideMode.PULL);
        Waits.when(helper, () -> chest.countItem(Items.BREAD) == 0, 60, () -> "the bread should leave the chest", () -> {
            helper.assertTrue(station.buffer().getStackInSlot(0).getCount() == 64, "the planned slot fills to 64, has "
                    + station.buffer().getStackInSlot(0));
            helper.assertTrue(station.buffer().getStackInSlot(1).getCount() == 20, "the other 20 go into a free slot, has "
                    + station.buffer().getStackInSlot(1));
            helper.succeed();
        });
    }

    /** A planned Inbound slot with bread takes no carrots and gives nothing to the intake, the buffer or the GUI. */
    @ServusGameTest(timeoutTicks = 40)
    public static void plannedSlotStaysClosedOtherwise(GameTestHelper helper) {
        InboundStationBlockEntity station = WarehouseFixtures.inbound(helper, LONE_STATION, Direction.NORTH);
        station.buffer().insertItem(0, new ItemStack(Items.BREAD, 20), false);
        station.buffer().lock(0, new SlotLock(SlotLock.Kind.OUT, ItemKey.of(new ItemStack(Items.BREAD)), 20, FOREIGN_JOB));
        Transfer.insertItem(station.intake(), new ItemStack(Items.CARROT, 10), false);
        helper.assertTrue(station.buffer().getStackInSlot(0).is(Items.BREAD) && station.buffer().getStackInSlot(1).is(Items.CARROT),
                "the carrots go into another slot");
        helper.assertTrue(Transfer.extractItem(station.buffer(), 0, 64, false).isEmpty(), "the buffer gives nothing from the planned slot");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        InboundStationMenu menu = new InboundStationMenu(1, player.getInventory(), station);
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty() && station.buffer().getStackInSlot(0).getCount() == 20, "a click takes nothing");
        helper.succeed();
    }

    /** A put-away locks its station slot and the room in the rack, both for the same job, and releases both on delivery. */
    @ServusGameTest(timeoutTicks = 400)
    public static void putAwayLocksAndReleases(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        helper.runAfterDelay(10, () -> {
            Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 40), false);
            Waits.when(helper, () -> station.isClaimed(0), 20, () -> "the station slot should be locked", () -> {
                int job = station.claimedBy(0);
                LockedSlots rack = warehouse.rack().items();
                helper.assertTrue(rack.lockedAmount(job, SlotLock.Kind.IN) == 40 && station.buffer().lockedAmount(job, SlotLock.Kind.OUT) == 40,
                        "the rack holds an IN lock and the station an OUT lock for 40 of the same job");
                Waits.when(helper, () -> warehouse.rackCount(Items.COBBLESTONE) == 40, MiniWarehouse.TRIP_TICKS * 3,
                        () -> "the cobblestone should reach the rack", () -> {
                            helper.assertTrue(locks(rack) == 0 && locks(station.buffer()) == 0, "every lock is released after delivery");
                            helper.succeed();
                        });
            });
        });
    }

    /** A pick locks the items in the rack and a slot of its destination, and releases both once delivered. */
    @ServusGameTest(timeoutTicks = 500)
    public static void pickLocksAndReleases(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).stock(new ItemStack(Items.IRON_INGOT, 64));
        OutboundStationBlockEntity station = outbound(helper, warehouse);
        helper.runAfterDelay(10, () -> {
            String refusal = warehouse.network().feedStation(station, IRON, 16);
            helper.assertTrue(refusal.isEmpty(), "the feed is planned, refused " + refusal);
            LockedSlots rack = warehouse.rack().items();
            helper.assertTrue(rack.locked().size() == 1 && rack.locked().getFirst().lock().amount() == 16, "16 iron are locked in the rack");
            int job = rack.locked().getFirst().lock().jobId();
            helper.assertTrue(station.buffer().lockedAmount(job, SlotLock.Kind.IN) == 16, "a buffer slot is locked for the same pick");
            helper.assertTrue(warehouse.network().available(IRON) == 48, "only unlocked iron counts as available");
            Waits.when(helper, () -> station.bufferCount(IRON) == 16, MiniWarehouse.TRIP_TICKS * 3, () -> "the iron should arrive", () -> {
                helper.assertTrue(locks(rack) == 0 && locks(station.buffer()) == 0, "every lock is released after delivery");
                helper.assertTrue(warehouse.rackCount(Items.IRON_INGOT) == 48, "48 iron stay in the rack");
                helper.succeed();
            });
        });
    }

    /** Two picks never plan the same items: the second one finds nothing left. */
    @ServusGameTest(timeoutTicks = 100)
    public static void picksDoNotShareItems(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).stock(new ItemStack(Items.COBBLESTONE, 64));
        OutboundStationBlockEntity station = outbound(helper, warehouse);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(warehouse.network().feedStation(station, COBBLE, 40).isEmpty(), "the first pick is planned");
            helper.assertTrue(warehouse.network().feedStation(station, COBBLE, 24).isEmpty(), "the second pick takes the rest of the slot");
            String refusal = warehouse.network().feedStation(station, COBBLE, 1);
            helper.assertTrue(refusal.equals("out_of_stock"), "nothing is left for a third, refused " + refusal);
            helper.assertTrue(warehouse.rack().items().locksOf(0).size() == 2, "both picks share the slot");
            helper.succeed();
        });
    }

    /** A lock whose job nobody knows is released after a few seconds. */
    @ServusGameTest(timeoutTicks = 400)
    public static void orphanLockIsReleased(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).stock(new ItemStack(Items.COBBLESTONE, 30));
        warehouse.rack().items().lock(0, new SlotLock(SlotLock.Kind.OUT, COBBLE, 30, FOREIGN_JOB));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(warehouse.rack().items().isLocked(0) && warehouse.network().available(COBBLE) == 0, "the orphan lock holds for now");
            Waits.when(helper, () -> !warehouse.rack().items().isLocked(0), 350, () -> "the orphan lock should be released",
                    () -> {
                        helper.assertTrue(warehouse.network().available(COBBLE) == 30, "the cobblestone is available again");
                        helper.succeed();
                    });
        });
    }

    /** Locks are saved with the rack and come back when it loads. */
    @ServusGameTest(timeoutTicks = 100)
    public static void locksSurviveAReload(GameTestHelper helper) {
        HeavyRackBlockEntity rack = WarehouseFixtures.rack(helper, LONE_RACK, Direction.SOUTH);
        rack.items().insertItem(3, new ItemStack(Items.COBBLESTONE, 30), false);
        rack.items().lock(3, new SlotLock(SlotLock.Kind.OUT, COBBLE, 12, 7));
        rack.items().lock(3, new SlotLock(SlotLock.Kind.OUT, COBBLE, 10, 8));
        rack.items().lock(5, new SlotLock(SlotLock.Kind.IN, COBBLE, 64, 9));
        HeavyRackBlockEntity loaded = PersistGameTests.reload(helper, LONE_RACK);
        helper.assertTrue(loaded.items().locksOf(3).size() == 2 && loaded.items().lockedAmount(8, SlotLock.Kind.OUT) == 10,
                "both OUT locks of slot 3 come back");
        helper.assertTrue(loaded.items().lockOf(5) != null && loaded.items().lockOf(5).kind() == SlotLock.Kind.IN && loaded.items().lockOf(5).jobId() == 9,
                "the IN lock of slot 5 comes back");
        helper.succeed();
    }

    /** When the rack a queued pick takes from is broken, the pick is planned again from another rack. */
    @ServusGameTest(timeoutTicks = 600)
    public static void brokenSourceReplansThePick(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN).stock(new ItemStack(Items.IRON_INGOT, 16));
        BlockPos farRail = warehouse.railPos(2).east();
        WarehouseFixtures.railOnStone(helper, farRail);
        HeavyRackBlockEntity farRack = WarehouseFixtures.rack(helper, farRail.north(), Direction.SOUTH);
        WarehouseFixtures.put(farRack, new ItemStack(Items.IRON_INGOT, 16));
        OutboundStationBlockEntity station = outbound(helper, warehouse);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(warehouse.network().feedStation(station, IRON, 16).isEmpty(), "the pick is planned");
            helper.assertTrue(warehouse.rack().items().locked().size() == 1, "the near rack is the cheapest source");
            helper.setBlock(warehouse.rackPos(), Blocks.AIR.defaultBlockState());
            Waits.when(helper, () -> station.bufferCount(IRON) == 16, MiniWarehouse.TRIP_TICKS * 4,
                    () -> "the pick should be planned again from the far rack, buffer has " + station.bufferCount(IRON), () -> {
                        helper.assertTrue(WarehouseFixtures.count(farRack, Items.IRON_INGOT) == 0, "the far rack gave its iron");
                        helper.succeed();
                    });
        });
    }
}
