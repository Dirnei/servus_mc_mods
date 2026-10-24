package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.data.packet.Delivery;
import io.github.dirnei.mc.servus.foundation.data.packet.DeliveryReason;
import io.github.dirnei.mc.servus.foundation.data.packet.Grant;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.shuttles.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * The delivery handshake with a Packing Station as the endpoint. Rails along
 * z 6 from x 2 to 9 with the controller at x 1, a Packing Station south of the
 * rail at x 3, a rack at x 9 facing the rail, data ducts along z 8 from x 3 to
 * 5 and a Crafting Station south of the ducts at x 5 as the sending member,
 * whose items come from a test stock. No garage, so nothing is put away.
 */
public final class PacketEndpointGameTests {

    private static final BlockPos CONTROLLER = new BlockPos(1, 1, 6);
    private static final BlockPos POINT = new BlockPos(3, 1, 7);
    private static final BlockPos RACK = new BlockPos(9, 1, 7);
    private static final BlockPos SENDER = new BlockPos(5, 1, 9);

    private PacketEndpointGameTests() {
    }

    private record Setup(PackingStationBlockEntity point, ItemSlots stock) {
    }

    private static Setup setup(GameTestHelper helper, boolean rack, int planks) {
        WarehouseFixtures.controller(helper, CONTROLLER);
        for (int x = 2; x <= 9; x++) {
            WarehouseFixtures.railOnStone(helper, new BlockPos(x, 1, 6));
        }
        if (rack) {
            WarehouseFixtures.rack(helper, RACK, Direction.NORTH);
        }
        helper.setBlock(POINT, ServusShuttlesBlocks.PACKING_STATION.get().defaultBlockState());
        helper.setBlock(SENDER, ServusShuttlesBlocks.CRAFTING_STATION.get().defaultBlockState()
                .setValue(CraftingStationBlock.FACING, Direction.SOUTH));
        for (int x = 3; x <= 5; x++) {
            TerminalWiring.dataDuct(helper, new BlockPos(x, 1, 8));
        }
        ItemSlots stock = new ItemSlots(4);
        int left = planks;
        for (int slot = 0; slot < stock.getSlots() && left > 0; slot++) {
            int count = Math.min(64, left);
            stock.setStackInSlot(slot, new ItemStack(Items.OAK_PLANKS, count));
            left -= count;
        }
        return new Setup(helper.getBlockEntity(POINT, PackingStationBlockEntity.class), stock);
    }

    private static void fillSlots(PackingStationBlockEntity point, int slots) {
        for (int slot = 0; slot < slots; slot++) {
            point.buffer().setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
    }

    private static Delivery movePlanks(GameTestHelper helper, Setup setup, int amount) {
        return DataPackets.move(helper.getLevel(), helper.absolutePos(SENDER), helper.absolutePos(POINT), new ItemStack(Items.OAK_PLANKS),
                amount, new ItemView(setup.stock())).orElseThrow(() -> new AssertionError("the move should be made"));
    }

    private static int planks(ItemSlots slots) {
        return WarehouseFixtures.count(slots, Items.OAK_PLANKS);
    }

    /** With two free slots a move of 200 planks is granted 128: those arrive, 72 stay at the source. */
    @ServusGameTest(timeoutTicks = 400)
    public static void endpointGrantsPart(GameTestHelper helper) {
        Setup setup = setup(helper, true, 200);
        fillSlots(setup.point(), 7);
        helper.runAfterDelay(10, () -> {
            Delivery delivery = movePlanks(helper, setup, 200);
            helper.assertTrue(delivery.requested() == 128, "the delivery should shrink to the grant of 128, is " + delivery);
            helper.succeedWhen(() -> {
                helper.assertTrue(delivery.state() == Delivery.State.DONE, "the delivery should be done, is " + delivery);
                helper.assertTrue(planks(setup.point().buffer()) == 128, "128 planks should be in To storage, got " + planks(setup.point().buffer()));
                helper.assertTrue(planks(setup.stock()) == 72, "72 planks should stay at the source, got " + planks(setup.stock()));
            });
        });
    }

    /** A full To storage buffer refuses: the move fails at once with the reason and takes nothing. */
    @ServusGameTest(timeoutTicks = 100)
    public static void endpointRefusesWhenFull(GameTestHelper helper) {
        Setup setup = setup(helper, true, 64);
        fillSlots(setup.point(), 9);
        helper.runAfterDelay(10, () -> {
            Delivery delivery = movePlanks(helper, setup, 64);
            helper.assertTrue(delivery.state() == Delivery.State.FAILED && delivery.reason() == DeliveryReason.REFUSED,
                    "the move should be refused, is " + delivery);
            helper.assertTrue(delivery.refusal().equals(PackingStationBlockEntity.REFUSE_FULL), "the reason should be To storage full, is "
                    + delivery.refusal());
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(planks(setup.stock()) == 64, "the source should keep its 64 planks");
                helper.succeed();
            });
        });
    }

    /** Without storage that takes the item the station refuses with no storage. */
    @ServusGameTest(timeoutTicks = 100)
    public static void endpointRefusesWithoutStorage(GameTestHelper helper) {
        Setup setup = setup(helper, false, 64);
        helper.runAfterDelay(10, () -> {
            Delivery delivery = movePlanks(helper, setup, 64);
            helper.assertTrue(delivery.reason() == DeliveryReason.REFUSED && delivery.refusal().equals(PackingStationBlockEntity.REFUSE_NO_STORAGE),
                    "the move should be refused for no storage, is " + delivery + " " + delivery.refusal());
            helper.succeed();
        });
    }

    /** Room a grant reserved is its own: a second move finds no free slot and the reserved slot stays empty. */
    @ServusGameTest(timeoutTicks = 100)
    public static void reservedRoomIsTheGrants(GameTestHelper helper) {
        Setup setup = setup(helper, true, 64);
        fillSlots(setup.point(), 8);
        helper.runAfterDelay(10, () -> {
            Grant held = setup.point().offer(new ItemStack(Items.OAK_PLANKS), 64);
            helper.assertTrue(held.granted() && held.amount() == 64, "the last slot should be granted for 64");
            Delivery delivery = movePlanks(helper, setup, 32);
            helper.assertTrue(delivery.reason() == DeliveryReason.REFUSED, "a second move should be refused, is " + delivery);
            helper.assertTrue(setup.point().buffer().getStackInSlot(8).isEmpty(), "the reserved slot should stay empty");
            held.close();
            helper.assertTrue(!setup.point().buffer().isLocked(8), "closing the grant frees the slot");
            helper.succeed();
        });
    }

    /** A delivery whose source runs dry ends short; its grant closes with what arrived and frees the slot. */
    @ServusGameTest(timeoutTicks = 300)
    public static void shortDeliveryClosesGrant(GameTestHelper helper) {
        Setup setup = setup(helper, true, 64);
        helper.runAfterDelay(10, () -> {
            Delivery delivery = movePlanks(helper, setup, 64);
            helper.runAfterDelay(30, () -> {
                setup.stock().setStackInSlot(0, ItemStack.EMPTY);
                helper.succeedWhen(() -> {
                    helper.assertTrue(delivery.state() == Delivery.State.DONE, "the delivery should end, is " + delivery);
                    int arrived = planks(setup.point().buffer());
                    helper.assertTrue(arrived > 0 && arrived < 64 && arrived == delivery.delivered(),
                            "only what arrived should be in To storage, got " + arrived + " of " + delivery);
                    for (int slot = 0; slot < setup.point().buffer().getSlots(); slot++) {
                        helper.assertTrue(!setup.point().buffer().isLocked(slot), "no slot should stay reserved");
                    }
                });
            });
        });
    }

    /** Grants are not saved: a reloaded station holds no reservation and keeps what already arrived. */
    @ServusGameTest(timeoutTicks = 100)
    public static void reloadDropsGrants(GameTestHelper helper) {
        Setup setup = setup(helper, true, 0);
        helper.runAfterDelay(10, () -> {
            PackingStationBlockEntity point = setup.point();
            Grant grant = point.offer(new ItemStack(Items.OAK_PLANKS), 64);
            grant.deliver(new ItemStack(Items.OAK_PLANKS, 30), helper.getLevel().getGameTime());
            helper.assertTrue(point.buffer().locked().size() == 1, "the grant reserves one slot");
            CompoundTag tag = point.saveWithFullMetadata(helper.getLevel().registryAccess());
            PackingStationBlockEntity copy = new PackingStationBlockEntity(point.getBlockPos(), point.getBlockState());
            copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
            helper.assertTrue(copy.buffer().locked().isEmpty(), "the reloaded station holds no reservation");
            helper.assertTrue(planks(copy.buffer()) == 30, "the 30 planks that arrived are kept, got " + planks(copy.buffer()));
            grant.close();
            helper.succeed();
        });
    }

    /** A target without endpoint takes packets as before. */
    @ServusGameTest(timeoutTicks = 100)
    public static void targetWithoutEndpoint(GameTestHelper helper) {
        Setup setup = setup(helper, true, 0);
        CraftingStationBlockEntity station = helper.getBlockEntity(SENDER, CraftingStationBlockEntity.class);
        StationJobs.sticksJob(station, 64);
        for (int slot = 0; slot < station.outputs().getSlots(); slot++) {
            station.outputs().setStackInSlot(slot, new ItemStack(Items.STICK, 64));
        }
        ItemSlots stock = new ItemSlots(1);
        stock.setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 8));
        helper.runAfterDelay(10, () -> {
            Delivery delivery = DataPackets.move(helper.getLevel(), helper.absolutePos(POINT), helper.absolutePos(SENDER),
                    new ItemStack(Items.OAK_PLANKS), 8, new ItemView(stock)).orElseThrow();
            helper.succeedWhen(() -> {
                helper.assertTrue(delivery.state() == Delivery.State.DONE, "the delivery should be done, is " + delivery);
                helper.assertTrue(WarehouseFixtures.count(station.inputs(), Items.OAK_PLANKS) == 8, "8 planks should be in the station");
            });
        });

    }
}
