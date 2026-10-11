package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseConnectionRule;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlock;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Places a Storage Terminal on the data line the way the older tests had it
 * beside the warehouse: a Packing Station takes the terminal's old spot, a data
 * duct sits on top of it and the terminal on top of the duct. Stations join
 * only through rails, so unless a rail already touches the Packing Station a rail
 * is laid under it and under every storage block beside it.
 */
public final class TerminalWiring {

    private TerminalWiring() {
    }

    /** Packing Station at {@code pos}, data duct above, terminal two blocks up, linked to the warehouse by rail. */
    public static void place(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.PACKING_STATION.get().defaultBlockState());
        dataDuct(helper, pos.above());
        helper.setBlock(terminalPos(pos), ServusStorageBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
        railUnder(helper, pos);
        // Some tests place the controller after the terminal; link again once they did.
        helper.runAfterDelay(1, () -> railUnder(helper, pos));
    }

    /**
     * Unless a rail already touches the station at {@code pos}, lays a rail under it and under
     * every storage block beside it, so the station joins their warehouse the only way it can.
     */
    public static void railUnder(GameTestHelper helper, BlockPos pos) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (helper.getBlockState(pos.relative(side)).is(ServusStorageBlocks.RAIL.get())) {
                return;
            }
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos beside = pos.relative(side);
            if (helper.getLevel().getBlockEntity(helper.absolutePos(beside)) instanceof StorageNodeBlockEntity node
                    && node.kind() == WarehouseConnectionRule.NodeKind.STOCK
                    && !helper.getBlockState(beside.below()).is(ServusStorageBlocks.RAIL.get())) {
                rail(helper, beside.below());
            }
        }
        if (!helper.getBlockState(pos.below()).is(ServusStorageBlocks.RAIL.get())) {
            rail(helper, pos.below());
        }
    }

    /** Where {@link #place} put the terminal. */
    public static BlockPos terminalPos(BlockPos pos) {
        return pos.above(2);
    }

    /** Where {@link #place} put the Packing Station. */
    public static BlockPos packingStationPos(BlockPos pos) {
        return pos;
    }

    /** A warehouse rail shaped by its neighbours. */
    public static void rail(GameTestHelper helper, BlockPos pos) {
        BlockState state = Block.updateFromNeighbourShapes(ServusStorageBlocks.RAIL.get().defaultBlockState(),
                helper.getLevel(), helper.absolutePos(pos));
        helper.setBlock(pos, state);
    }

    public static StorageTerminalBlockEntity at(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(terminalPos(pos), StorageTerminalBlockEntity.class);
    }

    /**
     * Polls every tick until {@code condition} holds, then runs {@code then}; fails after {@code maxTicks}.
     * Each poll is a fresh lambda: the GameTest scheduler keys tasks by instance and would drop a re-added one.
     */
    public static void when(GameTestHelper helper, BooleanSupplier condition, int maxTicks, Supplier<String> failure, Runnable then) {
        int[] left = {maxTicks};
        Runnable[] poll = new Runnable[1];
        poll[0] = () -> {
            if (condition.getAsBoolean()) {
                then.run();
            } else if (--left[0] <= 0) {
                helper.fail(failure.get());
            } else {
                helper.runAfterDelay(1, () -> poll[0].run());
            }
        };
        helper.runAfterDelay(1, () -> poll[0].run());
    }

    /** A Utility Duct carrying only the data line. */
    public static UtilityDuctBlockEntity dataDuct(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos, UtilityDuctBlockEntity.class);
        duct.addLine(Line.DATA);
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
        return duct;
    }
}
