package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.data.DataCableBlock;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationBlocks;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.crate.CrateBlock;
import io.github.dirnei.mc.servus.storage.crate.CrateBlockEntity;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlock;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Lib Data Cable with Storage blocks as its members. */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DataCableGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos A = new BlockPos(3, 1, 3);
    private static final BlockPos B = new BlockPos(4, 1, 3);
    private static final BlockPos C = new BlockPos(5, 1, 3);
    private static final BlockPos WEST_END = new BlockPos(2, 1, 3);
    private static final BlockPos EAST_END = new BlockPos(6, 1, 3);
    private static final BlockPos ABOVE_B = new BlockPos(4, 2, 3);

    private DataCableGameTests() {
    }

    private static void cable(GameTestHelper helper, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = Block.updateFromNeighbourShapes(ServusFoundationBlocks.DATA_CABLE.get().defaultBlockState(),
                helper.getLevel(), absolute);
        helper.setBlock(pos, state);
    }

    private static void cableRow(GameTestHelper helper) {
        cable(helper, A);
        cable(helper, B);
        cable(helper, C);
    }

    private static Optional<DataNetwork> network(GameTestHelper helper, BlockPos pos) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(pos));
    }

    private static DataNetwork requireNetwork(GameTestHelper helper, BlockPos pos) {
        return network(helper, pos).orElseThrow(() -> new AssertionError("no data network at " + pos));
    }

    private static boolean arm(GameTestHelper helper, BlockPos cable, Direction side) {
        return helper.getBlockState(cable).getValue(ConnectableBlock.property(side));
    }

    private static CraftingStationBlockEntity sticksStation(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
        CraftingStationBlockEntity station = helper.getBlockEntity(pos);
        station.pattern().setStackInSlot(1, new ItemStack(Items.OAK_PLANKS));
        station.pattern().setStackInSlot(4, new ItemStack(Items.OAK_PLANKS));
        helper.assertTrue(station.saveFromPattern().isEmpty(), "the stick pattern should save");
        return station;
    }

    private static void controller(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState()
                .setValue(WarehouseControllerBlock.FACING, Direction.NORTH));
    }

    /** Three cables in a row form one network and link to each other. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void dataCableRowIsOneNetwork(GameTestHelper helper) {
        cableRow(helper);
        helper.runAfterDelay(2, () -> {
            DataNetwork network = requireNetwork(helper, A);
            helper.assertTrue(network == requireNetwork(helper, C), "both ends should be in one network");
            helper.assertTrue(network.cables() == 3, "three cables, got " + network.cables());
            helper.assertTrue(arm(helper, A, Direction.EAST) && arm(helper, B, Direction.WEST) && arm(helper, B, Direction.EAST),
                    "cables should show arms toward each other");
            helper.succeed();
        });
    }

    /** Breaking the middle cable splits the row into two networks. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void dataCableSplitsWhenBroken(GameTestHelper helper) {
        cableRow(helper);
        helper.runAfterDelay(2, () -> {
            helper.setBlock(B, Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(2, () -> {
                DataNetwork west = requireNetwork(helper, A);
                DataNetwork east = requireNetwork(helper, C);
                helper.assertTrue(west != east, "the ends should be two networks after the split");
                helper.assertTrue(west.cables() == 1 && east.cables() == 1, "one cable each");
                helper.assertTrue(!arm(helper, A, Direction.EAST), "the arm toward the gap should be gone");
                helper.succeed();
            });
        });
    }

    /** A Crafting Station is a producer and a Warehouse Controller a stock member; both get an arm. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void dataCableFindsStationAndController(GameTestHelper helper) {
        cableRow(helper);
        sticksStation(helper, WEST_END);
        controller(helper, EAST_END);
        helper.runAfterDelay(2, () -> {
            DataNetwork network = requireNetwork(helper, B);
            helper.assertTrue(network.dataMembers(DataRole.PRODUCER).size() == 1
                            && network.dataMembers(DataRole.PRODUCER).get(0).pos().equals(helper.absolutePos(WEST_END)),
                    "the station should be the one producer, got " + network.dataMembers());
            helper.assertTrue(network.dataMembers(DataRole.STOCK).size() == 1
                            && network.dataMembers(DataRole.STOCK).get(0).pos().equals(helper.absolutePos(EAST_END)),
                    "the controller should be the one stock member, got " + network.dataMembers());
            helper.assertTrue(arm(helper, A, Direction.WEST) && arm(helper, C, Direction.EAST), "arms toward both members");
            helper.succeed();
        });
    }

    /** The click summary counts cables, producers and stock members. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void dataCableSummaryCounts(GameTestHelper helper) {
        cableRow(helper);
        sticksStation(helper, WEST_END);
        controller(helper, EAST_END);
        helper.runAfterDelay(2, () -> {
            Component summary = DataCableBlock.summary(requireNetwork(helper, B));
            helper.assertTrue(summary.getContents() instanceof TranslatableContents contents
                            && contents.getKey().equals("message.servus_lib.data_cable.summary")
                            && Arrays.equals(contents.getArgs(), new Object[] {3, 1, 1}),
                    "summary should say 3 cables, 1 producer, 1 stock, got " + summary.getString());
            helper.succeed();
        });
    }

    /** A sneak click on the top of a straight run, east of centre, places past the east end and not on top. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void dataCableSneakExtendsRun(GameTestHelper helper) {
        cableRow(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        BlockPos middle = helper.absolutePos(B);
        ItemStack stack = new ItemStack(ServusFoundationItems.DATA_CABLE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(new Vec3(middle.getX() + 0.7, middle.getY() + 0.625, middle.getZ() + 0.5), Direction.UP, middle, false)));
        helper.assertBlockPresent(ServusFoundationBlocks.DATA_CABLE.get(), EAST_END);
        helper.assertBlockNotPresent(ServusFoundationBlocks.DATA_CABLE.get(), ABOVE_B);
        helper.succeed();
    }

    /** A chest is no member and gets no arm. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void dataCableIgnoresChest(GameTestHelper helper) {
        cableRow(helper);
        helper.setBlock(EAST_END, Blocks.CHEST.defaultBlockState());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(requireNetwork(helper, B).dataMembers().isEmpty(), "a chest must not be a member");
            helper.assertTrue(!arm(helper, C, Direction.EAST), "no arm toward the chest");
            helper.succeed();
        });
    }

    /** A member placed later joins, and leaves again when it is broken. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void dataCableMemberBrokenLeaves(GameTestHelper helper) {
        cableRow(helper);
        helper.runAfterDelay(2, () -> {
            sticksStation(helper, EAST_END);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(requireNetwork(helper, A).dataMembers(DataRole.PRODUCER).size() == 1, "the station placed later should join");
                helper.setBlock(EAST_END, Blocks.AIR.defaultBlockState());
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(requireNetwork(helper, A).dataMembers().isEmpty(), "the broken station should leave");
                    helper.assertTrue(!arm(helper, C, Direction.EAST), "the arm should go with it");
                    helper.succeed();
                });
            });
        });
    }

    /** Two cables of one network touching the same controller count it once. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void dataCableTouchedTwiceCountsOnce(GameTestHelper helper) {
        controller(helper, B);
        cable(helper, A);
        cable(helper, ABOVE_B);
        cable(helper, A.above());
        helper.runAfterDelay(2, () -> {
            DataNetwork network = requireNetwork(helper, A);
            helper.assertTrue(network == requireNetwork(helper, ABOVE_B), "the three cables are one network");
            helper.assertTrue(network.dataMembers().size() == 1, "one member, got " + network.dataMembers());
            helper.succeed();
        });
    }

    /** A cable linking a controller with planks and a sticks station moves nothing on its own. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void dataCableMovesNothing(GameTestHelper helper) {
        BlockPos crate = new BlockPos(7, 1, 3);
        helper.setBlock(crate, ServusStorageBlocks.CRATE.get().defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
        CrateBlockEntity planks = helper.getBlockEntity(crate);
        planks.insert(new ItemStack(Items.OAK_PLANKS, 32), false);
        controller(helper, EAST_END);
        cableRow(helper);
        CraftingStationBlockEntity station = sticksStation(helper, WEST_END);
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(planks.count(0) == 32, "the planks should stay in the crate, got " + planks.count(0));
            for (int slot = 0; slot < station.inputs().getSlots(); slot++) {
                helper.assertTrue(station.inputs().getStackInSlot(slot).isEmpty(), "the station inputs should stay empty");
            }
            helper.succeed();
        });
    }

    /** The query finds the network from a member position and nothing at a stone block. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void dataNetworksAtMemberAndStone(GameTestHelper helper) {
        cableRow(helper);
        controller(helper, EAST_END);
        helper.setBlock(ABOVE_B.above(), Blocks.STONE.defaultBlockState());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(network(helper, EAST_END).orElse(null) == requireNetwork(helper, A), "the controller should find the cable network");
            helper.assertTrue(network(helper, ABOVE_B.above()).isEmpty(), "stone has no network");
            helper.succeed();
        });
    }
}
