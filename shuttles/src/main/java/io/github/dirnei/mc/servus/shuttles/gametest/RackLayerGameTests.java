package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlock;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlock;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/** A top click on a rack with racks in hand places a layer over the clicked shelf. */
public final class RackLayerGameTests {

    private static final int Z = 3;
    private static final BlockPos MIDDLE = new BlockPos(3, 1, Z);

    private RackLayerGameTests() {
    }

    /** Ten racks on the middle of a shelf of three: three racks on top, one shelf facing north, seven left. */
    @ServusGameTest
    public static void layerCoversAShelfOfThree(GameTestHelper helper) {
        RackPlacementGameTests.floor(helper);
        heavyRow(helper, 2, 4);
        ItemStack stack = heavyRacks(10);
        Placing.onTop(helper, stack, MIDDLE, Direction.SOUTH, false);
        helper.assertTrue(stack.getCount() == 7, "three racks are used, left " + stack.getCount());
        for (int x = 2; x <= 4; x++) {
            helper.assertBlockProperty(new BlockPos(x, 2, Z), HeavyRackBlock.FACING, Direction.NORTH);
        }
        List<BlockPos> shelf = HeavyRackBlock.shelf(helper.getLevel(), helper.absolutePos(MIDDLE.above()));
        helper.assertTrue(shelf.size() == 3, "the layer forms one shelf of three, got " + shelf.size());
        helper.succeed();
    }

    /** A sneak at the top edge of a bay's front lays a layer on top of the column, twice: rows two and three, nine racks used. */
    @ServusGameTest
    public static void sneakUpLaysALayerOnTopOfTheColumn(GameTestHelper helper) {
        RackPlacementGameTests.floor(helper);
        heavyRow(helper, 2, 4);
        ItemStack stack = heavyRacks(10);
        for (int row = 2; row <= 3; row++) {
            Player player = Placing.player(helper, Direction.SOUTH, true, stack);
            Placing.use(helper, player, MIDDLE, Direction.NORTH, new Vec3(0.5, 0.9, 0));
            for (int x = 2; x <= 4; x++) {
                helper.assertBlockProperty(new BlockPos(x, row, Z), HeavyRackBlock.FACING, Direction.NORTH);
            }
        }
        helper.assertTrue(stack.getCount() == 4, "six racks are used, left " + stack.getCount());
        List<BlockPos> shelf = HeavyRackBlock.shelf(helper.getLevel(), helper.absolutePos(new BlockPos(3, 3, Z)));
        helper.assertTrue(shelf.size() == 3, "the top layer forms one shelf of three, got " + shelf.size());
        helper.succeed();
    }

    /** Two racks for a shelf of three place one rack on the clicked bay. */
    @ServusGameTest
    public static void tooFewRacksPlaceOne(GameTestHelper helper) {
        RackPlacementGameTests.floor(helper);
        heavyRow(helper, 2, 4);
        ItemStack stack = heavyRacks(2);
        Placing.onTop(helper, stack, MIDDLE, Direction.SOUTH, false);
        assertOnlyMiddle(helper, stack);
        helper.succeed();
    }

    /** A block over one bay of the shelf: one rack on the clicked bay. */
    @ServusGameTest
    public static void takenSpotPlacesOne(GameTestHelper helper) {
        RackPlacementGameTests.floor(helper);
        heavyRow(helper, 2, 4);
        WarehouseFixtures.stone(helper, new BlockPos(4, 2, Z));
        ItemStack stack = heavyRacks(10);
        Placing.onTop(helper, stack, MIDDLE, Direction.SOUTH, false);
        helper.assertTrue(stack.getCount() == 9, "one rack is used, left " + stack.getCount());
        helper.assertBlockPresent(ServusShuttlesBlocks.HEAVY_RACK.get(), MIDDLE.above());
        helper.assertBlockNotPresent(ServusShuttlesBlocks.HEAVY_RACK.get(), new BlockPos(2, 2, Z));
        helper.succeed();
    }

    /** Two shelves of three side by side: a click on the left one layers only that one. */
    @ServusGameTest
    public static void onlyTheClickedShelfGetsALayer(GameTestHelper helper) {
        RackPlacementGameTests.floor(helper);
        heavyRow(helper, 2, 7);
        helper.assertTrue(HeavyRackBlock.shelf(helper.getLevel(), helper.absolutePos(MIDDLE)).size() == 3, "the row splits into shelves of three");
        ItemStack stack = heavyRacks(10);
        Placing.onTop(helper, stack, MIDDLE, Direction.SOUTH, false);
        helper.assertTrue(stack.getCount() == 7, "three racks are used, left " + stack.getCount());
        for (int x = 5; x <= 7; x++) {
            helper.assertBlockNotPresent(ServusShuttlesBlocks.HEAVY_RACK.get(), new BlockPos(x, 2, Z));
        }
        helper.succeed();
    }

    /** A layer of three empty Fluid Racks over a shelf of three has its port above the port below. */
    @ServusGameTest
    public static void fluidLayerPortAbovePort(GameTestHelper helper) {
        RackPlacementGameTests.floor(helper);
        FluidRackFixtures.row(helper, Direction.NORTH, new BlockPos(2, 1, Z), MIDDLE, new BlockPos(4, 1, Z));
        helper.runAfterDelay(2, () -> {
            ItemStack stack = FluidRackFixtures.item();
            stack.setCount(3);
            Placing.onTop(helper, stack, MIDDLE, Direction.SOUTH, false);
            helper.assertTrue(stack.isEmpty(), "three Fluid Racks are used, left " + stack.getCount());
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(FluidRackFixtures.shelf(helper, MIDDLE.above()).size() == 3, "the layer forms one shelf of three");
                for (int x = 2; x <= 4; x++) {
                    BlockPos below = new BlockPos(x, 1, Z);
                    helper.assertTrue(FluidRackFixtures.port(helper, below) == FluidRackFixtures.port(helper, below.above()),
                            "the port above matches the port below at x " + x);
                }
                helper.succeed();
            });
        });
    }

    /** A Fluid Rack item carrying water places one rack, not a layer. */
    @ServusGameTest
    public static void fluidItemWithWaterPlacesOne(GameTestHelper helper) {
        RackPlacementGameTests.floor(helper);
        FluidRackFixtures.row(helper, Direction.NORTH, new BlockPos(2, 1, Z), MIDDLE, new BlockPos(4, 1, Z));
        helper.runAfterDelay(2, () -> {
            ItemStack stack = FluidRackFixtures.carrying(Fluids.WATER, 1000);
            stack.setCount(3);
            Placing.onTop(helper, stack, MIDDLE, Direction.SOUTH, false);
            helper.assertTrue(stack.getCount() == 2, "one Fluid Rack is used, left " + stack.getCount());
            helper.assertBlockPresent(ServusShuttlesBlocks.FLUID_RACK.get(), MIDDLE.above());
            helper.assertBlockNotPresent(ServusShuttlesBlocks.FLUID_RACK.get(), new BlockPos(2, 2, Z));
            helper.assertBlockProperty(MIDDLE.above(), FluidRackBlock.FACING, Direction.NORTH);
            helper.succeed();
        });
    }

    private static void assertOnlyMiddle(GameTestHelper helper, ItemStack stack) {
        helper.assertTrue(stack.getCount() == 1, "one rack is used, left " + stack.getCount());
        helper.assertBlockPresent(ServusShuttlesBlocks.HEAVY_RACK.get(), MIDDLE.above());
        helper.assertBlockNotPresent(ServusShuttlesBlocks.HEAVY_RACK.get(), new BlockPos(2, 2, Z));
        helper.assertBlockNotPresent(ServusShuttlesBlocks.HEAVY_RACK.get(), new BlockPos(4, 2, Z));
    }

    /** Heavy Duty Racks placed by hand from x {@code fromX} to {@code toX}, facing north, so their shelves form as in play. */
    private static void heavyRow(GameTestHelper helper, int fromX, int toX) {
        for (int x = fromX; x <= toX; x++) {
            Placing.onTop(helper, heavyRacks(1), new BlockPos(x, 0, Z), Direction.SOUTH, false);
        }
    }

    private static ItemStack heavyRacks(int count) {
        return new ItemStack(ServusShuttlesItems.HEAVY_RACK.get(), count);
    }
}
