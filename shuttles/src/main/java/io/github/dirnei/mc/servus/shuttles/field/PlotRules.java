package io.github.dirnei.mc.servus.shuttles.field;

import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How a field reads its plots beyond the block on them: what it grows and,
 * for a Trees field, its sapling, whether it plants, its 2x2 layout, which
 * positions are field plots and which trees it gave up on.
 *
 * @param kind       what the field grows
 * @param sapling    the sapling block of a Trees field, null when none is set
 * @param plants     a sapling is set and fell only is off
 * @param twoByTwo   the sapling is planted in squares of four
 * @param squares    squares may be planted: the head holds a Range Upgrade
 * @param inField    whether a position is a plot candidate of the field
 * @param unfellable whether the plot, holding that block, has a tree the head gave up felling
 */
public record PlotRules(FieldKind kind, Block sapling, boolean plants, boolean twoByTwo, boolean squares, Predicate<BlockPos> inField,
                        BiPredicate<BlockPos, BlockState> unfellable) {

    /** The rules of a Crops field, which reads every plot by its block alone. */
    public static final PlotRules CROPS = new PlotRules(FieldKind.CROPS, null, false, false, false, pos -> false, (pos, state) -> false);

    /** The block whose survival tells soil a sapling grows on; oak while no sapling is set. */
    public Block soilProbe() {
        return sapling == null ? Blocks.OAK_SAPLING : sapling;
    }
}
