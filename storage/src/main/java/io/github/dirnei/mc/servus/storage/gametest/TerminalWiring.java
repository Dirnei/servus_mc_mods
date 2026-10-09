package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlock;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Places a Storage Terminal on the data line the way the older tests had it
 * beside the warehouse: a rail on stone takes the terminal's old spot (so it
 * joins the same warehouse), the Access Point stands on the rail (stations
 * join only through rails and shuttles serve it from that rail), a data duct
 * sits on top of it and the terminal on top of the duct.
 */
public final class TerminalWiring {

    private TerminalWiring() {
    }

    /** Rail at {@code pos}, Access Point above, data duct above that, terminal three blocks up. */
    public static void place(GameTestHelper helper, BlockPos pos) {
        if (helper.getBlockState(pos.below()).isAir()) {
            helper.setBlock(pos.below(), Blocks.STONE.defaultBlockState());
        }
        rail(helper, pos);
        helper.setBlock(accessPointPos(pos), ServusStorageBlocks.ACCESS_POINT.get().defaultBlockState());
        dataDuct(helper, pos.above(2));
        helper.setBlock(terminalPos(pos), ServusStorageBlocks.STORAGE_TERMINAL.get().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.NORTH));
    }

    /** Where {@link #place} put the terminal. */
    public static BlockPos terminalPos(BlockPos pos) {
        return pos.above(3);
    }

    /** Where {@link #place} put the Access Point. */
    public static BlockPos accessPointPos(BlockPos pos) {
        return pos.above();
    }

    /** A warehouse rail shaped by its neighbours. */
    public static void rail(GameTestHelper helper, BlockPos pos) {
        BlockState state = Block.updateFromNeighbourShapes(ServusStorageBlocks.RAIL.get().defaultBlockState(),
                helper.getLevel(), helper.absolutePos(pos));
        helper.setBlock(pos, state);
    }

    public static StorageTerminalBlockEntity at(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(terminalPos(pos));
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
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos);
        duct.addLine(Line.DATA);
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
        return duct;
    }
}
