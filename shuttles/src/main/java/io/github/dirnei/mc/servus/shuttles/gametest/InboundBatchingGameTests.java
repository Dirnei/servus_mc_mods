package io.github.dirnei.mc.servus.shuttles.gametest;

import static io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps.duct;

import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.packet.Grant;
import io.github.dirnei.mc.servus.foundation.data.packet.PacketEndpoint;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctMode;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.lock.LockedSlots;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLock;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * Inbound batching: a slot whose put-away no shuttle has loaded keeps taking
 * its item, an Item Pipe announces what it pulls and gets one slot reserved
 * for it, and a stack goes into storage as one stack in one trip.
 */
public final class InboundBatchingGameTests {

    private static final BlockPos ORIGIN = new BlockPos(3, 1, 4);
    /** Where {@link MiniWarehouse#withInbound()} puts the Inbound Station, front to the south. */
    private static final BlockPos INBOUND = ORIGIN.east(2).south();
    private static final BlockPos DUCT = INBOUND.east();
    private static final BlockPos SOURCE = DUCT.east();
    private static final BlockPos LONE_STATION = new BlockPos(2, 1, 2);
    private static final ItemKey COBBLESTONE = ItemKey.of(new ItemStack(Items.COBBLESTONE));
    private static final int FOREIGN_JOB = 777;
    private static final int WAIT = 60;

    private InboundBatchingGameTests() {
    }

    private static int count(InboundStationBlockEntity station, Item item) {
        return WarehouseFixtures.count(station.buffer(), item);
    }

    /** How many buffer slots hold the item. */
    private static int slotsWith(LockedSlots buffer, Item item) {
        int used = 0;
        for (int slot = 0; slot < buffer.getSlots(); slot++) {
            if (buffer.getStackInSlot(slot).is(item)) {
                used++;
            }
        }
        return used;
    }

    /** The first slot holding an IN lock, -1 for none. */
    private static int inLocked(LockedSlots buffer) {
        for (LockedSlots.Locked locked : buffer.locked()) {
            if (locked.lock().kind() == SlotLock.Kind.IN) {
                return locked.slot();
            }
        }
        return -1;
    }

    /** Whether a slot holding fewer than {@code full} of the item is OUT locked, so a shuttle was sent for a part. */
    private static boolean partSent(LockedSlots buffer, Item item, int full) {
        for (LockedSlots.Locked locked : buffer.locked()) {
            ItemStack stack = buffer.getStackInSlot(locked.slot());
            if (locked.lock().kind() == SlotLock.Kind.OUT && stack.is(item) && locked.lock().amount() < full) {
                return true;
            }
        }
        return false;
    }

    private static PacketEndpoint endpoint(InboundStationBlockEntity station) {
        return station.endpointFor(Direction.EAST);
    }

    /** A chest east of a duct east of the station, holding {@code count} of the item, pulled by the duct toward the station. */
    private static ChestBlockEntity pipeFrom(GameTestHelper helper, Item item, int count) {
        helper.setBlock(SOURCE, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(SOURCE, ChestBlockEntity.class);
        int left = count;
        for (int slot = 0; left > 0; slot++) {
            int stack = Math.min(left, item.getDefaultMaxStackSize());
            chest.setItem(slot, new ItemStack(item, stack));
            left -= stack;
        }
        duct(helper, DUCT, Line.ITEM).setMode(Line.ITEM, Direction.EAST, DuctMode.IN_PULL);
        return chest;
    }

    /** A free slot and a slot with 20 bread planned for a put-away: a Pull side's 30 bread go into the planned slot. */
    @ServusGameTest(timeoutTicks = 100)
    public static void pullFillsThePartialSlotFirst(GameTestHelper helper) {
        InboundStationBlockEntity station = WarehouseFixtures.inbound(helper, LONE_STATION, Direction.NORTH);
        helper.setBlock(LONE_STATION.south(), Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(LONE_STATION.south(), ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.BREAD, 30));
        station.buffer().insertItem(4, new ItemStack(Items.BREAD, 20), false);
        station.buffer().lock(4, new SlotLock(SlotLock.Kind.OUT, ItemKey.of(new ItemStack(Items.BREAD)), 20, FOREIGN_JOB));
        station.sideConfig().set(station.relativeSide(Direction.SOUTH), SideMode.PULL);
        Waits.when(helper, () -> chest.countItem(Items.BREAD) == 0, WAIT, () -> "the bread should leave the chest", () -> {
            helper.assertTrue(station.buffer().getStackInSlot(4).getCount() == 50, "slot 4 holds 50 bread, has " + station.buffer().getStackInSlot(4));
            helper.assertTrue(slotsWith(station.buffer(), Items.BREAD) == 1, "no other slot takes bread");
            helper.succeed();
        });
    }

    /** One cobblestone every two ticks, 64 in all: never more than one slot holds cobblestone, and all of it lands in the rack. */
    @ServusGameTest(timeoutTicks = 900)
    public static void trickleFillsOneSlot(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        AtomicInteger handed = new AtomicInteger();
        AtomicInteger ticks = new AtomicInteger();
        helper.onEachTick(() -> {
            if (ticks.incrementAndGet() > 10 && ticks.get() % 2 == 0 && handed.get() < 64) {
                ItemStack left = Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE), false);
                if (left.isEmpty()) {
                    handed.incrementAndGet();
                }
            }
            helper.assertTrue(slotsWith(station.buffer(), Items.COBBLESTONE) <= 1, "cobblestone spreads over "
                    + slotsWith(station.buffer(), Items.COBBLESTONE) + " slots");
        });
        Waits.when(helper, () -> warehouse.rackCount(Items.COBBLESTONE) == 64, 850, () -> "all 64 should reach the rack, has "
                + warehouse.rackCount(Items.COBBLESTONE), helper::succeed);
    }

    /** 8 cobblestone wait for the shuttle on its way and 24 more arrive: the slot holds 32 and the shuttle takes all 32 at once. */
    @ServusGameTest(timeoutTicks = 600)
    public static void toppedUpBeforeLoading(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        helper.onEachTick(() -> {
            int inRack = warehouse.rackCount(Items.COBBLESTONE);
            helper.assertTrue(inRack == 0 || inRack == 32, "the cobblestone lands in one trip, rack holds " + inRack);
        });
        helper.runAfterDelay(10, () -> {
            Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 8), false);
            Waits.when(helper, () -> station.isClaimed(0), 20, () -> "the 8 should be planned", () -> {
                int job = station.claimedBy(0);
                Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 24), false);
                helper.assertTrue(station.buffer().getStackInSlot(0).getCount() == 32, "the slot holds 32, has " + station.buffer().getStackInSlot(0));
                Waits.when(helper, () -> station.buffer().lockedAmount(job, SlotLock.Kind.OUT) == 32, 10, () -> "the job should grow to 32", () -> {
                    Waits.when(helper, () -> warehouse.rackCount(Items.COBBLESTONE) == 32, MiniWarehouse.TRIP_TICKS * 3,
                            () -> "the 32 should reach the rack", helper::succeed);
                });
            });
        });
    }

    /** After the shuttle took 32 cobblestone, 8 more become a new put-away. */
    @ServusGameTest(timeoutTicks = 800)
    public static void arrivingAfterLoading(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        helper.runAfterDelay(10, () -> {
            Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 32), false);
            Waits.when(helper, () -> station.isClaimed(0), 20, () -> "the 32 should be planned", () -> {
                int first = station.claimedBy(0);
                Waits.when(helper, () -> count(station, Items.COBBLESTONE) == 0, MiniWarehouse.TRIP_TICKS * 2, () -> "the shuttle should load", () -> {
                    Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 8), false);
                    Waits.when(helper, () -> station.isClaimed(0), 20, () -> "the 8 should be planned", () -> {
                        helper.assertTrue(station.claimedBy(0) != first, "the 8 are a new put-away");
                        Waits.when(helper, () -> warehouse.rackCount(Items.COBBLESTONE) == 40, MiniWarehouse.TRIP_TICKS * 4,
                                () -> "all 40 should reach the rack", helper::succeed);
                    });
                });
            });
        });
    }

    /** 8 cobblestone planned and 8 more arriving: the slot holds 16, its OUT lock is for 16 and the rack's IN lock grows by 8. */
    @ServusGameTest(timeoutTicks = 200)
    public static void inboundSlotGrows(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        helper.runAfterDelay(10, () -> {
            Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 8), false);
            Waits.when(helper, () -> station.isClaimed(0), 20, () -> "the 8 should be planned", () -> {
                int job = station.claimedBy(0);
                Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 8), false);
                Waits.when(helper, () -> station.buffer().lockedAmount(job, SlotLock.Kind.OUT) == 16, 10, () -> "the OUT lock should grow to 16", () -> {
                    helper.assertTrue(station.buffer().getStackInSlot(0).getCount() == 16 && station.buffer().locksOf(0).size() == 1,
                            "one slot with one lock for 16");
                    helper.assertTrue(warehouse.rack().items().lockedAmount(job, SlotLock.Kind.IN) == 16, "the rack's IN lock is for 16");
                    helper.succeed();
                });
            });
        });
    }

    /** A job of 64 is full: 10 more cobblestone go into another slot and become a put-away of their own. */
    @ServusGameTest(timeoutTicks = 200)
    public static void jobFull(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        helper.runAfterDelay(10, () -> {
            Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 64), false);
            Waits.when(helper, () -> station.isClaimed(0), 20, () -> "the 64 should be planned", () -> {
                int job = station.claimedBy(0);
                Transfer.insertItem(station.intake(), new ItemStack(Items.COBBLESTONE, 10), false);
                Waits.when(helper, () -> station.isClaimed(1), 10, () -> "the 10 should be planned", () -> {
                    helper.assertTrue(station.buffer().getStackInSlot(1).getCount() == 10, "the 10 are in slot 1");
                    helper.assertTrue(station.claimedBy(1) != job && station.buffer().lockedAmount(job, SlotLock.Kind.OUT) == 64,
                            "the 10 are a put-away of their own");
                    helper.succeed();
                });
            });
        });
    }

    /** Offered 200 cobblestone, the station grants 64 in one reserved slot; offered 20, it grants 20. */
    @ServusGameTest(timeoutTicks = 100)
    public static void grantsOneSlot(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN, false);
        InboundStationBlockEntity station = warehouse.withInbound();
        Waits.when(helper, () -> station.network().isPresent(), WAIT, () -> "the station should join the warehouse", () -> {
            Grant big = endpoint(station).offer(new ItemStack(Items.COBBLESTONE), 200);
            helper.assertTrue(big.granted() && big.amount() == 64, "200 offered grant 64, got " + big.amount());
            int slot = inLocked(station.buffer());
            helper.assertTrue(slot >= 0 && station.buffer().locksOf(slot).get(0).amount() == 64 && station.buffer().locked().size() == 1,
                    "one slot is reserved for 64");
            helper.assertTrue(station.holdsPipeGrant((int) big.ticket()), "the lock belongs to a pipe grant");
            Grant small = endpoint(station).offer(new ItemStack(Items.DIRT), 20);
            helper.assertTrue(small.granted() && small.amount() == 20 && station.buffer().locked().size() == 2, "20 offered grant 20 in one slot");
            big.close();
            small.close();
            helper.assertTrue(station.buffer().locked().isEmpty(), "closing frees the slots");
            helper.succeed();
        });
    }

    /** With every slot in use the station refuses; offline it refuses too. */
    @ServusGameTest(timeoutTicks = 100)
    public static void refusesWhenFullOrOffline(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN, false);
        InboundStationBlockEntity station = warehouse.withInbound();
        InboundStationBlockEntity lone = WarehouseFixtures.inbound(helper, LONE_STATION, Direction.NORTH);
        Waits.when(helper, () -> station.network().isPresent(), WAIT, () -> "the station should join the warehouse", () -> {
            for (int slot = 0; slot < station.buffer().getSlots(); slot++) {
                station.buffer().setStackInSlot(slot, new ItemStack(Items.STONE, 64));
            }
            helper.assertTrue(!endpoint(station).offer(new ItemStack(Items.COBBLESTONE), 64).granted(), "a full buffer refuses");
            helper.assertTrue(!lone.endpointFor(Direction.EAST).offer(new ItemStack(Items.COBBLESTONE), 64).granted(), "an offline station refuses");
            helper.assertTrue(station.endpointFor(Direction.SOUTH) == null, "the front answers no offer");
            helper.succeed();
        });
    }

    /** A reload while 30 of 64 granted cobblestone are in: the grant ends, its lock goes and the 30 are put away. */
    @ServusGameTest(timeoutTicks = 600)
    public static void restartWhileAPipeFills(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        Waits.when(helper, () -> station.network().isPresent(), WAIT, () -> "the station should join the warehouse", () -> {
            Grant grant = endpoint(station).offer(new ItemStack(Items.COBBLESTONE), 64);
            ItemStack rest = grant.deliver(new ItemStack(Items.COBBLESTONE, 30), helper.getLevel().getGameTime());
            helper.assertTrue(rest.isEmpty() && grant.left() == 34, "30 arrive in the grant");
            InboundStationBlockEntity loaded = PersistGameTests.reload(helper, INBOUND);
            helper.assertTrue(grant.closed() && grant.left() == 0, "the grant ends with the old block entity");
            helper.assertTrue(inLocked(loaded.buffer()) < 0 && count(loaded, Items.COBBLESTONE) == 30, "the loaded station keeps 30 without a reservation");
            Waits.when(helper, () -> warehouse.rackCount(Items.COBBLESTONE) == 30, MiniWarehouse.TRIP_TICKS * 3,
                    () -> "the 30 should be put away", helper::succeed);
        });
    }

    /** A data duct beside the station does not make the station a data member. */
    @ServusGameTest(timeoutTicks = 100)
    public static void dataLineIgnoresTheStation(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN, false);
        warehouse.withInbound();
        TerminalWiring.dataDuct(helper, DUCT);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(INBOUND)).isEmpty(),
                    "the station is no member of the data line");
            helper.succeed();
        });
    }

    /**
     * A pipe pulling 64 cobblestone into the station with one shuttle: one
     * slot fills, no shuttle is sent for a part, and one trip takes all 64.
     */
    @ServusGameTest(timeoutTicks = 900)
    public static void oneStackOneSlotOneTrip(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        pipeFrom(helper, Items.COBBLESTONE, 64);
        helper.onEachTick(() -> {
            helper.assertTrue(slotsWith(station.buffer(), Items.COBBLESTONE) <= 1, "the cobblestone stays in one slot");
            helper.assertTrue(!partSent(station.buffer(), Items.COBBLESTONE, 64), "no shuttle is sent for a part");
            int inRack = warehouse.rackCount(Items.COBBLESTONE);
            helper.assertTrue(inRack == 0 || inRack == 64, "one trip of 64, rack holds " + inRack);
        });
        Waits.when(helper, () -> warehouse.rackCount(Items.COBBLESTONE) == 64, 850, () -> "the 64 should reach the rack, has "
                + warehouse.rackCount(Items.COBBLESTONE), helper::succeed);
    }

    /** 192 cobblestone: at most two slots in use at a time and three trips of 64. */
    @ServusGameTest(timeoutTicks = 1800)
    public static void threeStacks(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        pipeFrom(helper, Items.COBBLESTONE, 192);
        helper.onEachTick(() -> {
            helper.assertTrue(slotsWith(station.buffer(), Items.COBBLESTONE) <= 2, "at most two slots hold cobblestone");
            helper.assertTrue(!partSent(station.buffer(), Items.COBBLESTONE, 64), "no shuttle is sent for a part");
            helper.assertTrue(warehouse.rackCount(Items.COBBLESTONE) % 64 == 0, "trips of 64, rack holds " + warehouse.rackCount(Items.COBBLESTONE));
        });
        Waits.when(helper, () -> warehouse.rackCount(Items.COBBLESTONE) == 192, 1750, () -> "all 192 should reach the rack, has "
                + warehouse.rackCount(Items.COBBLESTONE), helper::succeed);
    }

    /** A grant for 64 logs ends when the chest is emptied after 40 arrived, and the 40 go in one trip. */
    @ServusGameTest(timeoutTicks = 900)
    public static void chestRunsDry(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        ChestBlockEntity chest = pipeFrom(helper, Items.OAK_LOG, 64);
        AtomicInteger arrived = new AtomicInteger(-1);
        helper.onEachTick(() -> {
            int inRack = warehouse.rackCount(Items.OAK_LOG);
            helper.assertTrue(inRack == 0 || inRack == arrived.get(), "one trip of " + arrived.get() + ", rack holds " + inRack);
        });
        Waits.when(helper, () -> count(station, Items.OAK_LOG) >= 40, 300, () -> "40 logs should arrive", () -> {
            chest.clearContent();
            arrived.set(count(station, Items.OAK_LOG));
            Waits.when(helper, () -> inLocked(station.buffer()) < 0, 20, () -> "the grant should end within two pull intervals", () ->
                    Waits.when(helper, () -> warehouse.rackCount(Items.OAK_LOG) == arrived.get(), MiniWarehouse.TRIP_TICKS * 3,
                            () -> "the logs should be put away", helper::succeed));
        });
    }

    /** The duct broken while its grant has brought part of 64 cobblestone: the grant ends and the part is put away. */
    @ServusGameTest(timeoutTicks = 900)
    public static void pipeBroken(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        InboundStationBlockEntity station = warehouse.withInbound();
        pipeFrom(helper, Items.COBBLESTONE, 64);
        Waits.when(helper, () -> count(station, Items.COBBLESTONE) >= 24, 300, () -> "24 should arrive", () -> {
            helper.setBlock(DUCT, Blocks.AIR.defaultBlockState());
            int arrived = count(station, Items.COBBLESTONE);
            Waits.when(helper, () -> inLocked(station.buffer()) < 0, 5, () -> "the grant should end with the duct", () ->
                    Waits.when(helper, () -> warehouse.rackCount(Items.COBBLESTONE) == arrived, MiniWarehouse.TRIP_TICKS * 3,
                            () -> "the " + arrived + " should be put away", helper::succeed));
        });
    }
}
