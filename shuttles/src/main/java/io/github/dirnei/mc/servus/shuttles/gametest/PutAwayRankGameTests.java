package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.outbound.OutboundStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.station.InboundStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The put-away rule and the travel cost: a mini warehouse whose rail runs on
 * three blocks west to a far rack, so the near rack is two rail blocks from
 * the Inbound Station and the far rack five.
 */
public final class PutAwayRankGameTests {

    private static final BlockPos ORIGIN = new BlockPos(5, 1, 4);
    private static final int FAR_RAILS = 3;

    private PutAwayRankGameTests() {
    }

    private record Layout(MiniWarehouse warehouse, HeavyRackBlockEntity near, HeavyRackBlockEntity far) {
        int near(Item item) {
            return WarehouseFixtures.count(near, item);
        }

        int far(Item item) {
            return WarehouseFixtures.count(far, item);
        }
    }

    private static Layout layout(GameTestHelper helper) {
        MiniWarehouse warehouse = MiniWarehouse.build(helper, ORIGIN);
        BlockPos rail = warehouse.railPos(0);
        for (int i = 0; i < FAR_RAILS; i++) {
            rail = rail.west();
            WarehouseFixtures.railOnStone(helper, rail);
        }
        HeavyRackBlockEntity far = WarehouseFixtures.rack(helper, rail.north(), Direction.SOUTH);
        return new Layout(warehouse, warehouse.rack(), far);
    }

    private static void putAway(GameTestHelper helper, Layout layout, ItemStack stack) {
        InboundStationBlockEntity station = layout.warehouse().withInbound();
        helper.runAfterDelay(10, () -> Transfer.insertItem(station.intake(), stack, false));
    }

    /** A rack with a partial stack of the item beats a nearer one holding only full stacks of it. */
    @ServusGameTest(timeoutTicks = 600)
    public static void partialStackComesFirst(GameTestHelper helper) {
        Layout layout = layout(helper);
        WarehouseFixtures.put(layout.near(), new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 64));
        WarehouseFixtures.put(layout.far(), new ItemStack(Items.COBBLESTONE, 20));
        putAway(helper, layout, new ItemStack(Items.COBBLESTONE, 30));
        Waits.when(helper, () -> layout.far(Items.COBBLESTONE) == 50, 580, () -> "the far rack's partial stack should take the 30, far has "
                + layout.far(Items.COBBLESTONE), () -> {
            helper.assertTrue(layout.near(Items.COBBLESTONE) == 128, "the near rack keeps its two stacks");
            helper.assertTrue(layout.far().items().getStackInSlot(0).getCount() == 50, "the 30 top up the partial stack");
            helper.succeed();
        });
    }

    /** With no rack holding the item, the rack with more free slots wins over a nearer, fuller one. */
    @ServusGameTest(timeoutTicks = 600)
    public static void emptierRackComesFirst(GameTestHelper helper) {
        Layout layout = layout(helper);
        WarehouseFixtures.fill(layout.near(), Items.STONE);
        layout.near().items().setStackInSlot(0, ItemStack.EMPTY);
        layout.near().items().setStackInSlot(1, ItemStack.EMPTY);
        putAway(helper, layout, new ItemStack(Items.BREAD, 10));
        Waits.when(helper, () -> layout.far(Items.BREAD) == 10, 580, () -> "the emptier far rack should take the bread", () -> {
            helper.assertTrue(layout.near(Items.BREAD) == 0, "the nearly full near rack gets none");
            helper.succeed();
        });
    }

    /** Between equally empty racks the cheaper one wins. */
    @ServusGameTest(timeoutTicks = 400)
    public static void cheaperRackAmongEquals(GameTestHelper helper) {
        Layout layout = layout(helper);
        putAway(helper, layout, new ItemStack(Items.BREAD, 10));
        Waits.when(helper, () -> layout.near(Items.BREAD) == 10, 380, () -> "the near rack should take the bread", () -> {
            helper.assertTrue(layout.far(Items.BREAD) == 0, "the far rack gets none");
            helper.succeed();
        });
    }

    /** A pick takes from the rack with the cheaper way to its destination. */
    @ServusGameTest(timeoutTicks = 100)
    public static void pickFromTheCheaperRack(GameTestHelper helper) {
        Layout layout = layout(helper);
        WarehouseFixtures.put(layout.near(), new ItemStack(Items.IRON_INGOT, 16));
        WarehouseFixtures.put(layout.far(), new ItemStack(Items.IRON_INGOT, 16));
        OutboundStationBlockEntity station = WarehouseFixtures.outbound(helper, layout.warehouse().railPos(2).south().east(), Direction.SOUTH);
        WarehouseFixtures.railOnStone(helper, layout.warehouse().railPos(2).east());
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(layout.warehouse().network().feedStation(station, ItemKey.of(new ItemStack(Items.IRON_INGOT)), 16).isEmpty(),
                    "the feed is planned");
            helper.assertTrue(!layout.near().items().locked().isEmpty() && layout.far().items().locked().isEmpty(),
                    "the near rack gives the iron");
            helper.succeed();
        });
    }
}
