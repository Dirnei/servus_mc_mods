package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataSummary;
import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.data.packet.Delivery;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DataCableItem;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlock;
import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bandwidth of data cable tiers: a Storage Terminal at x 2 (moves from it
 * take planks from a test stock, moves into it land in its Input row), six
 * data ducts at x 3 to 8 and a
 * Crafting Station at x 9, all at y 1, z 3. Neither end is a transfer gate,
 * so only the ducts limit the deliveries. Rates are measured over the 40
 * ticks after the first second, when the full budgets of fresh ducts are spent.
 */
public final class DataBandwidthGameTests {

    private static final BlockPos SOURCE = new BlockPos(2, 1, 3);
    /** The planks each running test moves out of the terminal, keyed by its helper. */
    private static final java.util.Map<GameTestHelper, ItemSlots> STOCKS = new java.util.WeakHashMap<>();
    private static final BlockPos STATION = new BlockPos(9, 1, 3);
    private static final int FIRST_DUCT = 3;
    private static final int LAST_DUCT = 8;
    /** Ticks after the start when the measurement begins and how long it runs. */
    private static final int WARM_UP = 20;
    private static final int MEASURE = 40;
    /** Items the measurement may be off by: fractions at both ends. */
    private static final int SLACK = 4;

    private DataBandwidthGameTests() {
    }

    static UtilityDuctBlockEntity dataDuct(GameTestHelper helper, BlockPos pos, DataCableItem cable) {
        helper.setBlock(pos, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos, UtilityDuctBlockEntity.class);
        duct.addDataLine(cable.tierId());
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
        return duct;
    }

    /** Terminal with a stock of 1024 planks, six ducts of the tier {@code tierAt} gives per x, a station with full outputs that never crafts. */
    private static CraftingStationBlockEntity setup(GameTestHelper helper, java.util.function.IntFunction<DataCableItem> tierAt) {
        ItemSlots stock = new ItemSlots(16);
        for (int slot = 0; slot < stock.getSlots(); slot++) {
            stock.setStackInSlot(slot, new ItemStack(Items.OAK_PLANKS, 64));
        }
        STOCKS.put(helper, stock);
        helper.setBlock(SOURCE, ServusStorageBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        helper.setBlock(STATION, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState()
                .setValue(CraftingStationBlock.FACING, Direction.NORTH));
        CraftingStationBlockEntity station = helper.getBlockEntity(STATION, CraftingStationBlockEntity.class);
        StationJobs.sticksJob(station, 1000);
        for (int slot = 0; slot < station.outputs().getSlots(); slot++) {
            station.outputs().setStackInSlot(slot, new ItemStack(Items.STICK, 64));
        }
        for (int x = FIRST_DUCT; x <= LAST_DUCT; x++) {
            dataDuct(helper, new BlockPos(x, 1, 3), tierAt.apply(x));
        }
        return station;
    }

    /** Moves from the terminal take the test stock as their source; other moves take from the member. */
    private static Delivery move(GameTestHelper helper, BlockPos from, BlockPos to, Item item, int amount) {
        ItemView source = from.equals(SOURCE) ? new ItemView(STOCKS.get(helper)) : null;
        return DataPackets.move(helper.getLevel(), helper.absolutePos(from), helper.absolutePos(to), new ItemStack(item), amount, source)
                .orElseThrow(() -> new AssertionError("the move should start"));
    }

    /**
     * Starts the deliveries, then checks what each moved during the measurement
     * window against the expectation for each and for all together.
     */
    private static void measure(GameTestHelper helper, Delivery[] deliveries, IntPredicate together, String expected,
                                IntPredicate each, String expectedEach) {
        int[] before = new int[deliveries.length];
        helper.runAfterDelay(WARM_UP, () -> {
            for (int i = 0; i < deliveries.length; i++) {
                before[i] = deliveries[i].delivered();
            }
        });
        helper.runAfterDelay(WARM_UP + MEASURE, () -> {
            int sum = 0;
            StringBuilder parts = new StringBuilder();
            for (int i = 0; i < deliveries.length; i++) {
                int moved = deliveries[i].delivered() - before[i];
                sum += moved;
                parts.append(' ').append(moved);
                helper.assertTrue(!deliveries[i].ended(), "delivery " + i + " should still run, " + deliveries[i]);
                if (each != null) {
                    helper.assertTrue(each.test(moved), "each should move " + expectedEach + " in two seconds, got" + parts);
                }
            }
            int total = sum;
            helper.assertTrue(together.test(total), "together they should move " + expected + " in two seconds, got" + parts);
            helper.succeed();
        });
    }

    private static IntPredicate about(int items) {
        return moved -> Math.abs(moved - items) <= SLACK;
    }

    /** Five Fast Data Cable ducts and one Data Cable duct: the slow duct decides, about 32 items per second. */
    @ServusGameTest(timeoutTicks = 200)
    public static void slowestDuctDecides(GameTestHelper helper) {
        setup(helper, x -> x == 5 ? ServusLogisticsItems.DATA_CABLE.get() : ServusLogisticsItems.FAST_DATA_CABLE.get());
        helper.runAfterDelay(2, () -> {
            Delivery delivery = move(helper, SOURCE, STATION, Items.OAK_PLANKS, 500);
            measure(helper, new Delivery[] {delivery}, about(64), "about 64", null, null);
        });
    }

    /** All Fast Data Cable ducts: 128 items per second. */
    @ServusGameTest(timeoutTicks = 200)
    public static void fastDuctsCarryMore(GameTestHelper helper) {
        setup(helper, x -> ServusLogisticsItems.FAST_DATA_CABLE.get());
        helper.runAfterDelay(2, () -> {
            Delivery delivery = move(helper, SOURCE, STATION, Items.OAK_PLANKS, 560);
            measure(helper, new Delivery[] {delivery}, about(256), "about 256", null, null);
        });
    }

    /** Two deliveries in the same direction over duplex ducts of 32 share the budget: together about 32 items per second. */
    @ServusGameTest(timeoutTicks = 200)
    public static void sharedDuctSplits(GameTestHelper helper) {
        setup(helper, x -> ServusLogisticsItems.DUPLEX_DATA_CABLE.get());
        helper.runAfterDelay(2, () -> {
            Delivery first = move(helper, SOURCE, STATION, Items.OAK_PLANKS, 300);
            Delivery second = move(helper, SOURCE, STATION, Items.OAK_PLANKS, 300);
            measure(helper, new Delivery[] {first, second}, about(64), "about 64", null, null);
        });
    }

    /** Two deliveries in opposite directions over simplex Data Cable ducts share the bandwidth: together about 32 per second. */
    @ServusGameTest(timeoutTicks = 200)
    public static void simplexOppositeShares(GameTestHelper helper) {
        setup(helper, x -> ServusLogisticsItems.DATA_CABLE.get());
        helper.runAfterDelay(2, () -> {
            Delivery planks = move(helper, SOURCE, STATION, Items.OAK_PLANKS, 250);
            Delivery sticks = move(helper, STATION, SOURCE, Items.STICK, 250);
            measure(helper, new Delivery[] {planks, sticks}, about(64), "about 64", null, null);
        });
    }

    /** The same two deliveries over Duplex Data Cable ducts: each moves about 32 per second. */
    @ServusGameTest(timeoutTicks = 200)
    public static void duplexOppositeSeparate(GameTestHelper helper) {
        setup(helper, x -> ServusLogisticsItems.DUPLEX_DATA_CABLE.get());
        helper.runAfterDelay(2, () -> {
            Delivery planks = move(helper, SOURCE, STATION, Items.OAK_PLANKS, 250);
            Delivery sticks = move(helper, STATION, SOURCE, Items.STICK, 250);
            measure(helper, new Delivery[] {planks, sticks}, about(128), "about 128", about(64), "about 64");
        });
    }

    /** A busy Data Cable duct's summary says how many items per second pass it out of its bandwidth. */
    @ServusGameTest(timeoutTicks = 200)
    public static void summaryShowsLoad(GameTestHelper helper) {
        setup(helper, x -> ServusLogisticsItems.DATA_CABLE.get());
        BlockPos middle = new BlockPos(5, 1, 3);
        helper.runAfterDelay(2, () -> move(helper, SOURCE, STATION, Items.OAK_PLANKS, 500));
        helper.runAfterDelay(2 + WARM_UP + MEASURE, () -> {
            UtilityDuctBlockEntity duct = helper.getBlockEntity(middle, UtilityDuctBlockEntity.class);
            DataNetwork network = duct.dataNetwork().orElseThrow(() -> new AssertionError("the duct has a data network"));
            Component summary = DataSummary.of(network, helper.absolutePos(middle), duct.dataTier());
            helper.assertTrue(summary.getContents() instanceof TranslatableContents contents
                            && contents.getKey().equals(DataSummary.LOAD_KEY)
                            && contents.getArgs().length == 5
                            && contents.getArgs()[3] instanceof Integer load && Math.abs(load - 32) <= SLACK
                            && Integer.valueOf(32).equals(contents.getArgs()[4]),
                    "the summary should say about 32 of 32 items/s, got " + summary.getString());
            UtilityDuctBlockEntity idle = dataDuct(helper, new BlockPos(5, 1, 5), ServusLogisticsItems.DATA_CABLE.get());
            helper.assertTrue(network.load(io.github.dirnei.mc.servus.foundation.network.Conversions.toNodePos(idle.getBlockPos())) == 0,
                    "a duct off the path is idle");
            helper.succeed();
        });
    }

    /** The Production Terminal view names the slow Data Cable duct as the busiest with its load out of 32. */
    @ServusGameTest(timeoutTicks = 200)
    public static void busiestDuctNamed(GameTestHelper helper) {
        setup(helper, x -> x == 5 ? ServusLogisticsItems.DATA_CABLE.get() : ServusLogisticsItems.FAST_DATA_CABLE.get());
        BlockPos slow = new BlockPos(5, 1, 3);
        helper.runAfterDelay(2, () -> move(helper, SOURCE, STATION, Items.OAK_PLANKS, 500));
        helper.runAfterDelay(2 + WARM_UP + MEASURE, () -> {
            UtilityDuctBlockEntity duct = helper.getBlockEntity(slow, UtilityDuctBlockEntity.class);
            DataNetwork network = duct.dataNetwork().orElseThrow(() -> new AssertionError("the duct has a data network"));
            var rows = io.github.dirnei.mc.servus.storage.production.ProductionView.busiest(helper.getLevel(), network);
            helper.assertTrue(rows.size() == 1, "one busiest duct, got " + rows);
            var row = rows.get(0);
            helper.assertTrue(row.pos().equals(helper.absolutePos(slow)), "the Data Cable duct is the busiest, got " + row);
            helper.assertTrue(row.name().equals("Data Cable") && row.bandwidth() == 32 && Math.abs(row.load() - 32) <= SLACK,
                    "named Data Cable with about 32 of 32 items/s, got " + row);
            helper.succeed();
        });
    }
}
