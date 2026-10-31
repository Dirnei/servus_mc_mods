package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.material.Material;
import io.github.dirnei.mc.servus.foundation.material.MaterialContent;
import io.github.dirnei.mc.servus.foundation.material.MaterialHolder;
import io.github.dirnei.mc.servus.shuttles.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.FieldCorrosion;
import io.github.dirnei.mc.servus.shuttles.field.FieldHeadBlockEntity;
import io.github.dirnei.mc.servus.shuttles.field.FieldStatus;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.rail.BufferStopBlock;
import io.github.dirnei.mc.servus.shuttles.rail.BufferStopBlockEntity;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlock;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Buffer Stop and the Farm Buffer Stop: facing, the rail running into it, carrot, standing, recipe,
 * and that the network, fields and field aging ignore it.
 */
public final class BufferStopGameTests {

    private static final BlockPos SPOT = new BlockPos(3, 1, 3);
    private static final Identifier OAK = BuiltInRegistries.BLOCK.getKey(Blocks.OAK_PLANKS);
    private static final Identifier BIRCH = BuiltInRegistries.BLOCK.getKey(Blocks.BIRCH_PLANKS);
    private static final Identifier SPRUCE = BuiltInRegistries.BLOCK.getKey(Blocks.SPRUCE_PLANKS);
    private static final int FIELD_Z = 5;
    private static final BlockPos FIELD_STOP = new BlockPos(8, 1, FIELD_Z);

    private BufferStopGameTests() {
    }

    /** A player looking north places a stop with the last rail of a run on its south side: it faces south. */
    @ServusGameTest(timeoutTicks = 20)
    public static void placedStopFacesTheLastRail(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, SPOT.south());
        WarehouseFixtures.rail(helper, SPOT.south(2));
        Placing.onTop(helper, item(), SPOT.below(), Direction.NORTH, false);
        helper.assertBlockProperty(SPOT, BufferStopBlock.FACING, Direction.SOUTH);
        helper.succeed();
    }

    /** Sneaking ignores the rail on the west side: the front faces the player. */
    @ServusGameTest(timeoutTicks = 20)
    public static void sneakPlacedStopFacesThePlayer(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, SPOT.west());
        Placing.onTop(helper, item(), SPOT.below(), Direction.NORTH, true);
        helper.assertBlockProperty(SPOT, BufferStopBlock.FACING, Direction.SOUTH);
        helper.succeed();
    }

    /** A player looking east places a stop with a Farm Rail on its north side: it faces north. */
    @ServusGameTest(timeoutTicks = 20)
    public static void stopTurnsToAFarmRail(GameTestHelper helper) {
        floor(helper);
        FieldFixtures.farmRail(helper, SPOT.north());
        Placing.onTop(helper, item(), SPOT.below(), Direction.EAST, false);
        helper.assertBlockProperty(SPOT, BufferStopBlock.FACING, Direction.NORTH);
        helper.succeed();
    }

    /** No stop appears at an open rail end on its own. */
    @ServusGameTest(timeoutTicks = 20)
    public static void openRailEndStaysOpen(GameTestHelper helper) {
        RailFixtures.railAlongZ(helper, 3, 2, 4);
        helper.runAfterDelay(5, () -> {
            helper.assertBlockNotPresent(ServusShuttlesBlocks.BUFFER_STOP.get(), new BlockPos(3, 1, 1));
            helper.assertBlockNotPresent(ServusShuttlesBlocks.BUFFER_STOP.get(), new BlockPos(3, 1, 5));
            helper.assertBlockProperty(new BlockPos(3, 1, 2), ConnectableBlock.NORTH, false);
            helper.succeed();
        });
    }

    /**
     * The last rail of a north south run shows a straight piece towards a stop facing it, without a dock;
     * turning the stop away with the carrot or breaking it brings the end back.
     */
    @ServusGameTest(timeoutTicks = 20)
    public static void railRunsIntoTheStop(GameTestHelper helper) {
        RailFixtures.railAlongZ(helper, 3, 3, 5);
        BlockPos last = new BlockPos(3, 1, 3);
        BlockPos stop = last.north();
        WarehouseFixtures.stone(helper, stop.below());
        helper.setBlock(stop, stop(Direction.SOUTH));
        helper.assertBlockProperty(last, ConnectableBlock.NORTH, true);
        helper.assertBlockProperty(last, RailBlock.DOCK_NORTH, false);
        GameTestSteps.useCarrot(helper, helper.makeMockPlayer(GameType.SURVIVAL), stop, Direction.UP);
        helper.assertBlockProperty(stop, BufferStopBlock.FACING, Direction.WEST);
        helper.assertBlockProperty(last, ConnectableBlock.NORTH, false);
        helper.setBlock(stop, stop(Direction.SOUTH));
        helper.assertBlockProperty(last, ConnectableBlock.NORTH, true);
        helper.getLevel().destroyBlock(helper.absolutePos(stop), true);
        helper.assertBlockProperty(last, ConnectableBlock.NORTH, false);
        helper.succeed();
    }

    /** A stop facing away from the rail beside it leaves the rail's end. */
    @ServusGameTest(timeoutTicks = 20)
    public static void stopFacingAwayLeavesTheEnd(GameTestHelper helper) {
        RailFixtures.railAlongZ(helper, 3, 3, 5);
        BlockPos stop = new BlockPos(3, 1, 2);
        WarehouseFixtures.stone(helper, stop.below());
        helper.setBlock(stop, stop(Direction.NORTH));
        helper.assertBlockProperty(stop.south(), ConnectableBlock.NORTH, false);
        helper.succeed();
    }

    /** The carrot turns the stop a quarter clockwise, four times round. */
    @ServusGameTest(timeoutTicks = 20)
    public static void carrotTurnsTheStopClockwise(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(SPOT, stop(Direction.SOUTH));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (Direction expected : List.of(Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH)) {
            GameTestSteps.useCarrot(helper, player, SPOT, Direction.UP);
            helper.assertBlockProperty(SPOT, BufferStopBlock.FACING, expected);
        }
        helper.succeed();
    }

    /** Sneaking with the carrot dismantles a spruce stop into a spruce item. */
    @ServusGameTest(timeoutTicks = 20)
    public static void sneakCarrotDismantlesWithItsWood(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(SPOT, stop(Direction.NORTH));
        wood(helper, SPOT, SPRUCE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        GameTestSteps.useCarrot(helper, player, SPOT, Direction.UP);
        helper.assertBlockNotPresent(ServusShuttlesBlocks.BUFFER_STOP.get(), SPOT);
        assertDroppedWith(helper, SPRUCE);
        helper.succeed();
    }

    /** Breaking the floor under a birch stop drops it as a birch item. */
    @ServusGameTest(timeoutTicks = 20)
    public static void stopDropsWhenTheFloorGoes(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(SPOT, stop(Direction.NORTH));
        wood(helper, SPOT, BIRCH);
        helper.setBlock(SPOT.below(), Blocks.AIR.defaultBlockState());
        helper.assertBlockNotPresent(ServusShuttlesBlocks.BUFFER_STOP.get(), SPOT);
        assertDroppedWith(helper, BIRCH);
        helper.succeed();
    }

    /** A stop needs a floor: placed over air it is refused. */
    @ServusGameTest(timeoutTicks = 20)
    public static void stopNeedsAFloor(GameTestHelper helper) {
        WarehouseFixtures.stone(helper, SPOT.west());
        Placing.use(helper, item(), SPOT.west(), Direction.EAST, Direction.NORTH, false);
        helper.assertBlockNotPresent(ServusShuttlesBlocks.BUFFER_STOP.get(), SPOT);
        helper.succeed();
    }

    /** A rail in the air that touches only a stop is not held: it cannot stand there and is not placed. */
    @ServusGameTest(timeoutTicks = 20)
    public static void stopHoldsNoRailUp(GameTestHelper helper) {
        WarehouseFixtures.stone(helper, SPOT.below());
        helper.setBlock(SPOT, stop(Direction.EAST));
        BlockPos hung = SPOT.east();
        helper.assertFalse(ServusShuttlesBlocks.RAIL.get().defaultBlockState().canSurvive(helper.getLevel(), helper.absolutePos(hung)),
                "a stop must not hold a rail up");
        Placing.use(helper, new ItemStack(ServusShuttlesItems.RAIL.get()), SPOT, Direction.EAST, Direction.WEST, false);
        helper.assertBlockNotPresent(ServusShuttlesBlocks.RAIL.get(), hung);
        helper.succeed();
    }

    /** Controller, three rails and a stop facing the last one: the network holds the controller and the three rails, nothing at the stop. */
    @ServusGameTest(timeoutTicks = 40)
    public static void networkIgnoresTheStop(GameTestHelper helper) {
        RailFixtures.railAlongZ(helper, 3, 3, 5);
        BlockPos controllerPos = new BlockPos(3, 1, 2);
        BlockPos stop = new BlockPos(3, 1, 6);
        WarehouseFixtures.controller(helper, controllerPos);
        WarehouseFixtures.stone(helper, stop.below());
        helper.setBlock(stop, stop(Direction.NORTH));
        helper.assertBlockProperty(stop.north(), ConnectableBlock.SOUTH, true);
        helper.runAfterDelay(5, () -> {
            WarehouseNetwork network = helper.getBlockEntity(controllerPos, WarehouseControllerBlockEntity.class).network().orElseThrow();
            Set<NodePos> members = network.members();
            for (int z = 3; z <= 5; z++) {
                helper.assertTrue(members.contains(node(helper, new BlockPos(3, 1, z))), "the rail at z " + z + " is in the network");
            }
            helper.assertFalse(members.contains(node(helper, stop)), "the stop is not in the network");
            helper.assertTrue(network.size() == 4, "the network holds the controller and three rails, got " + network.size());
            helper.succeed();
        });
    }

    /** A field of five Farm Rails ending at a stop still has five rails and is served. */
    @ServusGameTest(timeoutTicks = 40)
    public static void fieldIgnoresTheStop(GameTestHelper helper) {
        FieldHeadBlockEntity head = fieldEndingAt(helper, ServusShuttlesBlocks.BUFFER_STOP.get());
        helper.runAfterDelay(3, () -> {
            assertServesFiveRails(helper, head);
            helper.succeed();
        });
    }

    /** A Farm Buffer Stop placed beside a Farm Rail faces it, and the rail runs into it. */
    @ServusGameTest(timeoutTicks = 20)
    public static void farmStopFacesAndConnectsAFarmRail(GameTestHelper helper) {
        floor(helper);
        FieldFixtures.farmRail(helper, SPOT.north());
        Placing.onTop(helper, farmItem(), SPOT.below(), Direction.EAST, false);
        helper.assertBlockProperty(SPOT, BufferStopBlock.FACING, Direction.NORTH);
        helper.assertBlockProperty(SPOT.north(), ConnectableBlock.SOUTH, true);
        helper.succeed();
    }

    /** A Farm Buffer Stop placed beside a Warehouse Rail faces it, and the rail runs into it. */
    @ServusGameTest(timeoutTicks = 20)
    public static void farmStopFacesAndConnectsAWarehouseRail(GameTestHelper helper) {
        floor(helper);
        WarehouseFixtures.rail(helper, SPOT.south());
        Placing.onTop(helper, farmItem(), SPOT.below(), Direction.NORTH, false);
        helper.assertBlockProperty(SPOT, BufferStopBlock.FACING, Direction.SOUTH);
        helper.assertBlockProperty(SPOT.south(), ConnectableBlock.NORTH, true);
        helper.assertBlockProperty(SPOT.south(), RailBlock.DOCK_NORTH, false);
        helper.succeed();
    }

    /** Aging a field that ends at a Farm Buffer Stop leaves the stop as it was and the field's rails the same. */
    @ServusGameTest(timeoutTicks = 40)
    public static void agingLeavesTheFarmStop(GameTestHelper helper) {
        FieldHeadBlockEntity head = fieldEndingAt(helper, ServusShuttlesBlocks.FARM_BUFFER_STOP.get());
        BlockState before = helper.getBlockState(FIELD_STOP);
        helper.runAfterDelay(3, () -> {
            head.age(ShuttlesStatsManager.INSTANCE.fieldCorrosionWater());
            helper.assertTrue(head.corrosion() == FieldCorrosion.EXPOSED, "the field ages, got " + head.corrosion());
            helper.assertTrue(helper.getBlockState(FIELD_STOP) == before, "the stop is unchanged, got " + helper.getBlockState(FIELD_STOP));
            helper.assertTrue(helper.getBlockEntity(FIELD_STOP, BufferStopBlockEntity.class) != null, "the stop keeps its block entity");
            assertServesFiveRails(helper, head);
            helper.succeed();
        });
    }

    /** Three iron ingots over an oak Warehouse Rail give one oak Buffer Stop. */
    @ServusGameTest(timeoutTicks = 20)
    public static void recipeKeepsTheRailsWood(GameTestHelper helper) {
        ItemStack rail = new ItemStack(ServusShuttlesItems.RAIL.get());
        rail.set(MaterialContent.MATERIAL.get(), OAK);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack result = BlockStyleGameTests.craft(helper, "buffer_stop", 3, 2,
                List.of(iron, iron.copy(), iron.copy(), ItemStack.EMPTY, rail, ItemStack.EMPTY));
        helper.assertTrue(result.is(ServusShuttlesItems.BUFFER_STOP.get()) && result.getCount() == 1, "one Buffer Stop, got " + result);
        helper.assertTrue(OAK.equals(result.get(MaterialContent.MATERIAL.get())), "the stop is oak, got " + result.get(MaterialContent.MATERIAL.get()));
        helper.succeed();
    }

    /** Three iron ingots over an oak Farm Rail give one oak Farm Buffer Stop. */
    @ServusGameTest(timeoutTicks = 20)
    public static void farmRecipeKeepsTheRailsWood(GameTestHelper helper) {
        ItemStack rail = new ItemStack(ServusShuttlesItems.FARM_RAIL.get());
        rail.set(MaterialContent.MATERIAL.get(), OAK);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack result = BlockStyleGameTests.craft(helper, "farm_buffer_stop", 3, 2,
                List.of(iron, iron.copy(), iron.copy(), ItemStack.EMPTY, rail, ItemStack.EMPTY));
        helper.assertTrue(result.is(ServusShuttlesItems.FARM_BUFFER_STOP.get()) && result.getCount() == 1, "one Farm Buffer Stop, got " + result);
        helper.assertTrue(OAK.equals(result.get(MaterialContent.MATERIAL.get())), "the stop is oak, got " + result.get(MaterialContent.MATERIAL.get()));
        helper.succeed();
    }

    /** A spruce stop item places a spruce stop. */
    @ServusGameTest(timeoutTicks = 20)
    public static void placedStopKeepsItsWood(GameTestHelper helper) {
        floor(helper);
        ItemStack stack = item();
        stack.set(MaterialContent.MATERIAL.get(), SPRUCE);
        Placing.onTop(helper, stack, SPOT.below(), Direction.NORTH, false);
        MaterialHolder placed = (MaterialHolder) helper.getLevel().getBlockEntity(helper.absolutePos(SPOT));
        helper.assertTrue(SPRUCE.equals(placed.materialSlot().material()), "the placed stop is spruce, got " + placed.materialSlot().material());
        helper.succeed();
    }

    private static ItemStack item() {
        return new ItemStack(ServusShuttlesItems.BUFFER_STOP.get());
    }

    private static ItemStack farmItem() {
        return new ItemStack(ServusShuttlesItems.FARM_BUFFER_STOP.get());
    }

    private static BlockState stop(Direction facing) {
        return stop(ServusShuttlesBlocks.BUFFER_STOP.get(), facing);
    }

    private static BlockState stop(Block block, Direction facing) {
        return block.defaultBlockState().setValue(BufferStopBlock.FACING, facing);
    }

    /** Five Farm Rails along x 3..7 with a Field Head west of them and that stop at {@link #FIELD_STOP} facing the last one. */
    private static FieldHeadBlockEntity fieldEndingAt(GameTestHelper helper, Block stop) {
        for (int x = 3; x <= 7; x++) {
            FieldFixtures.farmRail(helper, new BlockPos(x, 1, FIELD_Z));
        }
        WarehouseFixtures.stone(helper, FIELD_STOP.below());
        helper.setBlock(FIELD_STOP, stop(stop, Direction.WEST));
        return FieldFixtures.head(helper, new BlockPos(2, 1, FIELD_Z));
    }

    private static void assertServesFiveRails(GameTestHelper helper, FieldHeadBlockEntity head) {
        helper.assertBlockProperty(FIELD_STOP.west(), ConnectableBlock.EAST, true);
        helper.assertTrue(head.status() == FieldStatus.SERVING && head.rails().size() == 5,
                "the head serves the five rails, got " + head.status() + " with " + head.rails().size());
    }

    private static void wood(GameTestHelper helper, BlockPos pos, Identifier wood) {
        ((MaterialHolder) helper.getLevel().getBlockEntity(helper.absolutePos(pos))).materialSlot().set(wood);
    }

    private static NodePos node(GameTestHelper helper, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        return new NodePos(absolute.getX(), absolute.getY(), absolute.getZ());
    }

    private static void assertDroppedWith(GameTestHelper helper, Identifier wood) {
        List<ItemEntity> items = GameTestSteps.drops(helper, SPOT, 1).stream()
                .filter(entity -> entity.getItem().is(ServusShuttlesItems.BUFFER_STOP.get())).toList();
        helper.assertTrue(items.size() == 1 && wood.equals(Material.of(items.get(0).getItem())),
                "one dropped stop with " + wood + ", got " + items);
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                WarehouseFixtures.stone(helper, new BlockPos(x, 0, z));
            }
        }
    }
}
