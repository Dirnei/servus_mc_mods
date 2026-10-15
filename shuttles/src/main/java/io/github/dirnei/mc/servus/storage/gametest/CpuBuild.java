package io.github.dirnei.mc.servus.storage.gametest;

import io.github.dirnei.mc.servus.storage.cpu.CpuBlock;
import io.github.dirnei.mc.servus.storage.cpu.CpuBlockEntity;
import io.github.dirnei.mc.servus.storage.cpu.CpuBox;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlocks;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Builds Crafting CPU boxes of CPU and Buffer blocks in GameTests and forms
 * them with the carrot's logic. Positions are relative to the test.
 */
public final class CpuBuild {

    private CpuBuild() {
    }

    /**
     * Builds a box at {@code origin}; offsets that {@code buffer} accepts get
     * a Buffer block, the rest CPU blocks.
     */
    public static void box(GameTestHelper helper, BlockPos origin, int sx, int sy, int sz, Predicate<BlockPos> buffer) {
        for (int y = 0; y < sy; y++) {
            for (int z = 0; z < sz; z++) {
                for (int x = 0; x < sx; x++) {
                    BlockPos offset = new BlockPos(x, y, z);
                    helper.setBlock(origin.offset(offset), buffer.test(offset)
                            ? ServusStorageBlocks.CPU_BUFFER.get().defaultBlockState()
                            : ServusStorageBlocks.CRAFTING_CPU.get().defaultBlockState());
                }
            }
        }
    }

    /** A box whose first {@code cpuBlocks} places (lowest first by y, z, x) are CPU blocks and the rest Buffer blocks. */
    public static void box(GameTestHelper helper, BlockPos origin, int sx, int sy, int sz, int cpuBlocks) {
        box(helper, origin, sx, sy, sz, offset -> offset.getX() + sx * offset.getZ() + sx * sz * offset.getY() >= cpuBlocks);
    }

    /** A 3x3x3 cube of CPU blocks, without a buffer. */
    public static void cube(GameTestHelper helper, BlockPos origin) {
        box(helper, origin, 3, 3, 3, 27);
    }

    /** A 2x2x2 box of 2 CPU blocks and 6 Buffer blocks: 2 order slots, 6 buffer stacks. */
    public static void slotted(GameTestHelper helper, BlockPos origin) {
        box(helper, origin, 2, 2, 2, 2);
    }

    /** Uses the carrot's forming on the block at {@code at}. */
    public static CpuBox.Result form(GameTestHelper helper, BlockPos at) {
        return CpuBlock.tryForm(helper.getLevel(), helper.absolutePos(at));
    }

    /** Forms the box at {@code at} and fails the test when it does not form. */
    public static CpuBlockEntity formed(GameTestHelper helper, BlockPos at) {
        CpuBox.Result result = form(helper, at);
        helper.assertTrue(result.formed(), "the box should form, got " + result.reason());
        return cpu(helper, at);
    }

    /** Builds and forms a 3x3x3 cube of CPU blocks. */
    public static CpuBlockEntity formCube(GameTestHelper helper, BlockPos origin) {
        cube(helper, origin);
        return formed(helper, origin);
    }

    /** Builds and forms a 2x2x2 box of 2 CPU blocks and 6 Buffer blocks. */
    public static CpuBlockEntity formSlotted(GameTestHelper helper, BlockPos origin) {
        slotted(helper, origin);
        return formed(helper, origin);
    }

    /** The master of the formed CPU at {@code at}. */
    public static CpuBlockEntity cpu(GameTestHelper helper, BlockPos at) {
        CpuBlockEntity cpu = CpuBlock.master(helper.getLevel(), helper.absolutePos(at));
        if (cpu == null) {
            throw new AssertionError("no formed CPU at " + at);
        }
        return cpu;
    }
}
