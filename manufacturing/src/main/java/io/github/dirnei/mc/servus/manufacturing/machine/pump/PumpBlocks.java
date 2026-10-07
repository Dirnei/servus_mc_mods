package io.github.dirnei.mc.servus.manufacturing.machine.pump;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Block side helpers shared by both pump blocks. */
final class PumpBlocks {

    /**
     * The compact pump: a base plate resting on the source below, a narrower
     * body with the front, an outlet on top. Symmetric around the vertical axis,
     * so it fits every facing.
     */
    static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 2, 15),
            Block.box(3, 2, 3, 13, 12, 13),
            Block.box(6, 12, 6, 10, 15, 10));

    private PumpBlocks() {
    }

    /** A neighbour of the pump changed: its source is checked again on the next tick. */
    static void requestCheck(Level level, BlockPos pos) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PumpMachine pump) {
            pump.pump().requestCheck();
        }
    }
}
