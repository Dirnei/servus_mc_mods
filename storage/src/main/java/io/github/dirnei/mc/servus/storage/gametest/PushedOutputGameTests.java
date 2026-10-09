package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlock;
import io.github.dirnei.mc.servus.storage.crafting.CraftingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.order.WorkOrder;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * A producer beside a Packing Station that pushes its outputs on its own. The
 * station below has only Input sides, so the CPU can not collect its outputs;
 * the test moves them through the Packing Station face that touches the station,
 * which is what a pushing machine does.
 */
@GameTestHolder(ServusStorageIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PushedOutputGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos ACCESS = new BlockPos(2, 1, 3);
    private static final BlockPos BESIDE = new BlockPos(3, 1, 3);
    private static final ItemKey STICK = ItemKey.of(new ItemStack(Items.STICK));

    private PushedOutputGameTests() {
    }

    /** Places the station east of the Packing Station with every side Input. */
    private static CraftingStationBlockEntity pushingStation(GameTestHelper helper) {
        StationHandsGameTests.setup(helper, 8);
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.AIR.defaultBlockState());
        helper.setBlock(new BlockPos(5, 1, 2), Blocks.AIR.defaultBlockState());
        helper.setBlock(BESIDE, ServusStorageBlocks.CRAFTING_STATION.get().defaultBlockState().setValue(CraftingStationBlock.FACING, Direction.NORTH));
        CraftingStationBlockEntity station = helper.getBlockEntity(BESIDE);
        SideConfig sides = station.sideConfig();
        for (RelativeSide side : RelativeSide.values()) {
            if (!sides.isLocked(side) && sides.supported().contains(SideMode.INPUT)) {
                sides.set(side, SideMode.INPUT);
            }
        }
        return station;
    }

    private static boolean ready(GameTestHelper helper) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 2)))
                .map(network -> network.dataMembers(DataRole.PRODUCER).size() == 1
                        && CpuOrders.cpus(helper.getLevel(), network).size() == 1)
                .orElse(false);
    }

    /** Takes the crafted sticks out of the station and inserts them into the Packing Station through the given face. */
    private static void push(GameTestHelper helper, CraftingStationBlockEntity station, Direction face) {
        int count = station.outputs().getStackInSlot(0).getCount();
        ItemStack sticks = station.outputs().extractItem(0, count, false);
        PackingStationBlockEntity point = helper.getBlockEntity(TerminalWiring.packingStationPos(ACCESS));
        ItemStack left = ItemHandlerHelper.insertItem(point.handlerFor(face), sticks, false);
        helper.assertTrue(left.isEmpty(), "the Packing Station should take the sticks, left " + left);
    }

    private static boolean crafted(CraftingStationBlockEntity station) {
        ItemStack out = station.outputs().getStackInSlot(0);
        return out.is(Items.STICK) && out.getCount() >= 4;
    }

    /** Sticks pushed in through the face touching the producer count for its step, so the order finishes. */
    @GameTest(template = EMPTY, timeoutTicks = 800)
    public static void pushedOutputsCountForTheStep(GameTestHelper helper) {
        CraftingStationBlockEntity station = pushingStation(helper);
        TerminalWiring.when(helper, () -> ready(helper), 60, () -> "the station and the CPU should join the line", () -> {
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), StationHandsGameTests.line(helper), STICK, 4, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            WorkOrder order = placed.created().order();
            TerminalWiring.when(helper, () -> crafted(station), 400, () -> "the station should craft 4 sticks", () -> {
                push(helper, station, Direction.EAST);
                TerminalWiring.when(helper, () -> order.state() == WorkOrder.State.DONE, 100,
                        () -> "the order should finish, state " + order.state() + " " + order.reason()
                                + ", counted " + order.steps().get(0).collected(STICK), helper::succeed);
            });
        });
    }

    /** Sticks entering through a face that does not touch the producer count for nothing. */
    @GameTest(template = EMPTY, timeoutTicks = 800)
    public static void otherFaceCountsNothing(GameTestHelper helper) {
        CraftingStationBlockEntity station = pushingStation(helper);
        TerminalWiring.when(helper, () -> ready(helper), 60, () -> "the station and the CPU should join the line", () -> {
            CpuOrders.Placed placed = CpuOrders.create(helper.getLevel(), StationHandsGameTests.line(helper), STICK, 4, 0);
            helper.assertTrue(placed.made(), "the order should be made, refused " + placed.refusal());
            WorkOrder order = placed.created().order();
            TerminalWiring.when(helper, () -> crafted(station), 400, () -> "the station should craft 4 sticks", () -> {
                push(helper, station, Direction.WEST);
                helper.runAfterDelay(40, () -> {
                    helper.assertTrue(order.steps().get(0).collected(STICK) == 0,
                            "sticks through another face must not count, got " + order.steps().get(0).collected(STICK));
                    helper.assertTrue(order.state() != WorkOrder.State.DONE, "the order should still run");
                    helper.succeed();
                });
            });
        });
    }
}
