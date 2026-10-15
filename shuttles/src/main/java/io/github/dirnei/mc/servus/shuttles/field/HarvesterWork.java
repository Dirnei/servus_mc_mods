package io.github.dirnei.mc.servus.shuttles.field;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a harvester and a Field Head see and do on one plot, on the server
 * thread: read the plot's state, roll a ripe crop's drops, break it, till
 * the ground under the plot and plant the field's crop.
 */
public final class HarvesterWork {

    private HarvesterWork() {
    }

    /** The plot's state from the block on it and the block under it. */
    public static PlotState classify(Level level, BlockPos plot, BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state) ? PlotState.RIPE : PlotState.GROWING;
        }
        if (!isClear(state)) {
            return PlotState.BLOCKED;
        }
        BlockState below = level.getBlockState(plot.below());
        if (below.is(Blocks.FARMLAND)) {
            return PlotState.PLANTABLE;
        }
        return isTillable(below) ? PlotState.TILLABLE : PlotState.BLOCKED;
    }

    /** The plot's state as it is in the world now. */
    public static PlotState classify(Level level, BlockPos plot) {
        return classify(level, plot, level.getBlockState(plot));
    }

    /** Air, or a plant a harvester clears away, such as short grass or a fern. */
    public static boolean isClear(BlockState state) {
        return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    /** Clears a plant off the plot, dropping nothing. */
    private static void clear(ServerLevel level, BlockPos plot) {
        if (!level.getBlockState(plot).isAir()) {
            level.destroyBlock(plot, false);
        }
    }

    /** Only dirt and grass blocks are tilled. */
    public static boolean isTillable(BlockState ground) {
        return ground.is(Blocks.DIRT) || ground.is(Blocks.GRASS_BLOCK);
    }

    /** The drops a ripe crop on the plot would give now, rolled without a tool. */
    public static List<ItemStack> rollDrops(ServerLevel level, BlockPos plot) {
        return Block.getDrops(level.getBlockState(plot), level, plot, null);
    }

    /** Breaks the crop on the plot with particles and sound, dropping nothing. */
    public static void breakCrop(ServerLevel level, BlockPos plot) {
        level.destroyBlock(plot, false);
    }

    /** Turns the dirt or grass block under an empty plot into farmland; false when the ground is not tillable. */
    public static boolean till(ServerLevel level, BlockPos plot) {
        BlockPos ground = plot.below();
        if (!isClear(level.getBlockState(plot)) || !isTillable(level.getBlockState(ground))) {
            return false;
        }
        clear(level, plot);
        level.setBlockAndUpdate(ground, Blocks.FARMLAND.defaultBlockState());
        level.playSound(null, ground, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    /** Plants the crop of the crop item at its first age on an empty plot it can grow on; false when it cannot. */
    public static boolean plant(ServerLevel level, BlockPos plot, ItemStack cropItem) {
        if (!(cropItem.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof CropBlock crop)) {
            return false;
        }
        BlockState state = crop.defaultBlockState();
        if (!isClear(level.getBlockState(plot)) || !state.canSurvive(level, plot)) {
            return false;
        }
        clear(level, plot);
        level.setBlockAndUpdate(plot, state);
        level.playSound(null, plot, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }
}
