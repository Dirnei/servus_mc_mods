package io.github.dirnei.mc.servus.manufacturing.machine.pump;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Block side helpers shared by both pump blocks. */
final class PumpBlocks {

    private PumpBlocks() {
    }

    /** A neighbour of the pump changed: its source is checked again on the next tick. */
    static void requestCheck(Level level, BlockPos pos) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PumpMachine pump) {
            pump.pump().requestCheck();
        }
    }
}
