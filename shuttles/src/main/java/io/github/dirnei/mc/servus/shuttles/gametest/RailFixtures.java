package io.github.dirnei.mc.servus.shuttles.gametest;

import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;

/** Warehouse rails for GameTests. Positions are relative to the test. */
final class RailFixtures {

    private RailFixtures() {
    }

    /**
     * Stone at y 0 from {@code x - 1} to {@code x + 1} and a north to south rail at
     * y 1 on {@code x} from {@code fromZ} to {@code toZ}, each rail shaped by its neighbours.
     */
    static void railAlongZ(GameTestHelper helper, int x, int fromZ, int toZ) {
        for (int z = fromZ; z <= toZ; z++) {
            for (int dx = -1; dx <= 1; dx++) {
                WarehouseFixtures.stone(helper, new BlockPos(x + dx, 0, z));
            }
            helper.setBlock(new BlockPos(x, 1, z), ServusShuttlesBlocks.RAIL.get().defaultBlockState());
        }
        for (int z = fromZ; z <= toZ; z++) {
            shape(helper, new BlockPos(x, 1, z));
        }
    }

    /** Shapes the block at {@code pos} by its neighbours, as a player placing it would get. */
    static void shape(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Block.updateFromNeighbourShapes(helper.getBlockState(pos), helper.getLevel(), helper.absolutePos(pos)));
    }
}
