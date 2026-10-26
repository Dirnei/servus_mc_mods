package io.github.dirnei.mc.servus.shuttles.gametest;

import static io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps.duct;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctMode;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.lock.LockedSlots;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLock;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * The Item Pipe announcing its pulls to a block that answers offers, here an
 * Inbound Station of a warehouse without a garage, so what arrives stays in
 * the buffer.
 */
public final class DuctGrantGameTests {

    private static final BlockPos ORIGIN = new BlockPos(3, 1, 4);
    /** Where {@link MiniWarehouse#withInbound()} puts the Inbound Station, front to the south. */
    private static final BlockPos INBOUND = ORIGIN.east(2).south();
    private static final BlockPos DUCT = INBOUND.east();
    private static final BlockPos SOURCE = DUCT.east();
    private static final BlockPos SIDE_CHEST = DUCT.south();
    private static final int WAIT = 60;

    private DuctGrantGameTests() {
    }

    private static int cobblestone(InboundStationBlockEntity station) {
        return WarehouseFixtures.count(station.buffer(), Items.COBBLESTONE);
    }

    /** The IN locks of the buffer, as slot and lock. */
    private static List<LockedSlots.Locked> inLocks(InboundStationBlockEntity station) {
        return station.buffer().locked().stream().filter(locked -> locked.lock().kind() == SlotLock.Kind.IN).toList();
    }

    /** A warehouse without a garage, its station, a chest with {@code count} cobblestone and a duct pulling it toward the station. */
    private static ChestBlockEntity setup(GameTestHelper helper, int count) {
        MiniWarehouse.build(helper, ORIGIN, false).withInbound();
        helper.setBlock(SOURCE, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(SOURCE, ChestBlockEntity.class);
        for (int slot = 0; count - slot * 64 > 0; slot++) {
            chest.setItem(slot, new ItemStack(Items.COBBLESTONE, Math.min(64, count - slot * 64)));
        }
        return chest;
    }

    private static InboundStationBlockEntity station(GameTestHelper helper) {
        return helper.getBlockEntity(INBOUND, InboundStationBlockEntity.class);
    }

    private static UtilityDuctBlockEntity pullingDuct(GameTestHelper helper) {
        UtilityDuctBlockEntity duct = duct(helper, DUCT, Line.ITEM);
        duct.setMode(Line.ITEM, Direction.EAST, DuctMode.IN_PULL);
        return duct;
    }

    /** Runs {@code then} once the station is online in its warehouse. */
    private static void whenOnline(GameTestHelper helper, Runnable then) {
        Waits.when(helper, () -> station(helper).network().isPresent(), WAIT, () -> "the station should join the warehouse", then);
    }

    /** The first pull of 64 cobblestone offers all 64 before the first item moves, and the pipe still moves 8 per pull. */
    @ServusGameTest(timeoutTicks = 200)
    public static void stationToldAboutAStack(GameTestHelper helper) {
        setup(helper, 64);
        whenOnline(helper, () -> {
            pullingDuct(helper);
            InboundStationBlockEntity station = station(helper);
            Waits.when(helper, () -> cobblestone(station) > 0, WAIT, () -> "the first pull should arrive", () -> {
                helper.assertTrue(cobblestone(station) == 8, "one pull moves 8, got " + cobblestone(station));
                helper.assertTrue(inLocks(station).size() == 1 && inLocks(station).get(0).lock().amount() == 64,
                        "one slot is reserved for 64, locks " + station.buffer().locked());
                helper.assertTrue(station.buffer().getStackInSlot(inLocks(station).get(0).slot()).getCount() == 8, "the 8 are in the reserved slot");
                helper.succeed();
            });
        });
    }

    /** With "cobblestone, keep 16" on the pulling side and 50 in the chest, the offer is for 34. */
    @ServusGameTest(timeoutTicks = 200)
    public static void keepAmountRespected(GameTestHelper helper) {
        setup(helper, 50);
        whenOnline(helper, () -> {
            UtilityDuctBlockEntity duct = pullingDuct(helper);
            duct.filter(Direction.EAST).setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
            duct.setItemAmount(Direction.EAST, 0, 16);
            InboundStationBlockEntity station = station(helper);
            Waits.when(helper, () -> !inLocks(station).isEmpty(), WAIT, () -> "the offer should be granted", () -> {
                helper.assertTrue(inLocks(station).get(0).lock().amount() == 34, "the grant is for 34, got " + inLocks(station).get(0).lock());
                helper.succeed();
            });
        });
    }

    /** With no free slot the station refuses, and the pipe fills what room the slots still have. */
    @ServusGameTest(timeoutTicks = 300)
    public static void refusedOffer(GameTestHelper helper) {
        ChestBlockEntity chest = setup(helper, 64);
        whenOnline(helper, () -> {
            InboundStationBlockEntity station = station(helper);
            station.buffer().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 20));
            for (int slot = 1; slot < station.buffer().getSlots(); slot++) {
                station.buffer().setStackInSlot(slot, new ItemStack(Items.STONE, 64));
            }
            pullingDuct(helper);
            Waits.when(helper, () -> cobblestone(station) == 64, 200, () -> "the free room should fill, has " + cobblestone(station), () -> {
                helper.assertTrue(inLocks(station).isEmpty(), "no slot was reserved");
                helper.assertTrue(chest.countItem(Items.COBBLESTONE) == 20, "20 stay in the chest, has " + chest.countItem(Items.COBBLESTONE));
                helper.succeed();
            });
        });
    }

    /** 8 cobblestone pushed into the duct go into the station without an offer. */
    @ServusGameTest(timeoutTicks = 200)
    public static void pushedItemsAreNotAnnounced(GameTestHelper helper) {
        setup(helper, 0);
        whenOnline(helper, () -> {
            duct(helper, DUCT, Line.ITEM);
            InboundStationBlockEntity station = station(helper);
            helper.runAfterDelay(2, () -> {
                ItemStack left = Transfer.insertItem(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(DUCT), Direction.EAST),
                        new ItemStack(Items.COBBLESTONE, 8), false);
                helper.assertTrue(left.isEmpty() && cobblestone(station) == 8, "the 8 go in, left " + left);
                helper.assertTrue(inLocks(station).isEmpty(), "no offer was made");
                helper.succeed();
            });
        });
    }

    /** A chest on the same channel and priority gets its share while the station's share fills its grant. */
    @ServusGameTest(timeoutTicks = 400)
    public static void plainChestOnTheSameChannel(GameTestHelper helper) {
        setup(helper, 64);
        helper.setBlock(SIDE_CHEST, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity side = helper.getBlockEntity(SIDE_CHEST, ChestBlockEntity.class);
        whenOnline(helper, () -> {
            pullingDuct(helper);
            InboundStationBlockEntity station = station(helper);
            Waits.when(helper, () -> cobblestone(station) > 0 && side.countItem(Items.COBBLESTONE) > 0, 100,
                    () -> "both should get cobblestone", () -> {
                        helper.assertTrue(inLocks(station).size() == 1 && station.buffer().getStackInSlot(inLocks(station).get(0).slot()).is(Items.COBBLESTONE),
                                "the station's share is in the grant slot");
                        Waits.when(helper, () -> cobblestone(station) + side.countItem(Items.COBBLESTONE) == 64, 300,
                                () -> "all 64 should move", helper::succeed);
                    });
        });
    }

    /** 100 cobblestone: once the first grant of 64 is filled the pipe offers the other 36. */
    @ServusGameTest(timeoutTicks = 400)
    public static void nextStack(GameTestHelper helper) {
        setup(helper, 100);
        whenOnline(helper, () -> {
            pullingDuct(helper);
            InboundStationBlockEntity station = station(helper);
            Waits.when(helper, () -> inLocks(station).stream().anyMatch(locked -> locked.lock().amount() == 36), 300,
                    () -> "a grant of 36 should follow, locks " + station.buffer().locked(), () -> {
                        helper.assertTrue(inLocks(station).size() == 1, "the first grant ended");
                        Waits.when(helper, () -> cobblestone(station) == 100, 100, () -> "all 100 should arrive", () -> {
                            Waits.when(helper, () -> inLocks(station).isEmpty(), 20, () -> "the second grant should end", helper::succeed);
                        });
                    });
        });
    }

    /** Emptying the chest while the grant still expects items ends the grant within two pull intervals. */
    @ServusGameTest(timeoutTicks = 300)
    public static void sourceDry(GameTestHelper helper) {
        ChestBlockEntity chest = setup(helper, 64);
        whenOnline(helper, () -> {
            pullingDuct(helper);
            InboundStationBlockEntity station = station(helper);
            Waits.when(helper, () -> cobblestone(station) >= 40, 200, () -> "40 should arrive", () -> {
                chest.clearContent();
                Waits.when(helper, () -> inLocks(station).isEmpty(), 16, () -> "the grant should end within two pull intervals", helper::succeed);
            });
        });
    }
}
