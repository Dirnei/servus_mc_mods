package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.data.packet.DataPackets;
import io.github.dirnei.mc.servus.foundation.data.packet.Delivery;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DataCableItem;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Bandwidth of data cable tiers: a Warehouse Controller at x 2 (a crate of
 * planks and a spare crate beside it), six data ducts at x 3 to 8 and a
 * Crafting Station at x 9, all at y 1, z 3. Neither end is a transfer gate,
 * so only the ducts limit the deliveries. Rates are measured over the 40
 * ticks after the first second, when the full budgets of fresh ducts are spent.
 */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DataBandwidthGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos CRATE = new BlockPos(1, 1, 3);
    private static final BlockPos SPARE_CRATE = new BlockPos(2, 1, 2);
    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 3);
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
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos);
        duct.addDataLine(cable.tierId());
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
        return duct;
    }

    /** Controller with 1024 planks, six ducts of the tier {@code tierAt} gives per x, a station with full outputs that never crafts. */
    private static CraftingStationBlockEntity setup(GameTestHelper helper, java.util.function.IntFunction<DataCableItem> tierAt) {
        helper.setBlock(CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity crate = helper.getBlockEntity(CRATE);
        crate.insert(new ItemStack(Items.OAK_PLANKS, 1024), false);
        helper.setBlock(SPARE_CRATE, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        helper.setBlock(CONTROLLER, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(STATION, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState()
                .setValue(CraftingStationBlock.FACING, Direction.NORTH));
        CraftingStationBlockEntity station = helper.getBlockEntity(STATION);
        StationJobs.sticksJob(station, 1000);
        for (int slot = 0; slot < station.outputs().getSlots(); slot++) {
            station.outputs().setStackInSlot(slot, new ItemStack(Items.STICK, 64));
        }
        for (int x = FIRST_DUCT; x <= LAST_DUCT; x++) {
            dataDuct(helper, new BlockPos(x, 1, 3), tierAt.apply(x));
        }
        return station;
    }

    private static Delivery move(GameTestHelper helper, BlockPos from, BlockPos to, Item item, int amount) {
        return DataPackets.move(helper.getLevel(), helper.absolutePos(from), helper.absolutePos(to), new ItemStack(item), amount)
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
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void slowestDuctDecides(GameTestHelper helper) {
        setup(helper, x -> x == 5 ? ServusLogisticsItems.DATA_CABLE.get() : ServusLogisticsItems.FAST_DATA_CABLE.get());
        helper.runAfterDelay(2, () -> {
            Delivery delivery = move(helper, CONTROLLER, STATION, Items.OAK_PLANKS, 500);
            measure(helper, new Delivery[] {delivery}, about(64), "about 64", null, null);
        });
    }

    /** All Fast Data Cable ducts: 128 items per second. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void fastDuctsCarryMore(GameTestHelper helper) {
        setup(helper, x -> ServusLogisticsItems.FAST_DATA_CABLE.get());
        helper.runAfterDelay(2, () -> {
            Delivery delivery = move(helper, CONTROLLER, STATION, Items.OAK_PLANKS, 560);
            measure(helper, new Delivery[] {delivery}, about(256), "about 256", null, null);
        });
    }

    /** Two deliveries in the same direction over duplex ducts of 32 share the budget: together about 32 items per second. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void sharedDuctSplits(GameTestHelper helper) {
        setup(helper, x -> ServusLogisticsItems.DUPLEX_DATA_CABLE.get());
        helper.runAfterDelay(2, () -> {
            Delivery first = move(helper, CONTROLLER, STATION, Items.OAK_PLANKS, 300);
            Delivery second = move(helper, CONTROLLER, STATION, Items.OAK_PLANKS, 300);
            measure(helper, new Delivery[] {first, second}, about(64), "about 64", null, null);
        });
    }

    /** Two deliveries in opposite directions over simplex Data Cable ducts share the bandwidth: together about 32 per second. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void simplexOppositeShares(GameTestHelper helper) {
        setup(helper, x -> ServusLogisticsItems.DATA_CABLE.get());
        helper.runAfterDelay(2, () -> {
            Delivery planks = move(helper, CONTROLLER, STATION, Items.OAK_PLANKS, 250);
            Delivery sticks = move(helper, STATION, CONTROLLER, Items.STICK, 250);
            measure(helper, new Delivery[] {planks, sticks}, about(64), "about 64", null, null);
        });
    }

    /** The same two deliveries over Duplex Data Cable ducts: each moves about 32 per second. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void duplexOppositeSeparate(GameTestHelper helper) {
        setup(helper, x -> ServusLogisticsItems.DUPLEX_DATA_CABLE.get());
        helper.runAfterDelay(2, () -> {
            Delivery planks = move(helper, CONTROLLER, STATION, Items.OAK_PLANKS, 250);
            Delivery sticks = move(helper, STATION, CONTROLLER, Items.STICK, 250);
            measure(helper, new Delivery[] {planks, sticks}, about(128), "about 128", about(64), "about 64");
        });
    }
}
