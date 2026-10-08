package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import io.github.dirnei.mc.servus.storage.terminal.CounterTerminalBlock;
import io.github.dirnei.mc.servus.storage.terminal.CounterTerminalBlockEntity;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Places a Counter Terminal on the data line the way the older tests had it
 * beside the warehouse: an Access Point takes the terminal's old spot (so it
 * joins the same warehouse and is the shuttle destination), a data duct sits
 * on top of it and the terminal on top of the duct.
 */
public final class TerminalWiring {

    private TerminalWiring() {
    }

    /** Access Point at {@code pos}, data duct above, terminal two blocks up. */
    public static void place(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusStorageBlocks.ACCESS_POINT.get().defaultBlockState());
        dataDuct(helper, pos.above());
        helper.setBlock(terminalPos(pos), ServusStorageBlocks.COUNTER_TERMINAL.get().defaultBlockState()
                .setValue(CounterTerminalBlock.FACING, Direction.NORTH));
    }

    /** Where {@link #place} put the terminal. */
    public static BlockPos terminalPos(BlockPos pos) {
        return pos.above(2);
    }

    public static CounterTerminalBlockEntity at(GameTestHelper helper, BlockPos pos) {
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
