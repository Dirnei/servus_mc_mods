package io.github.dirnei.mc.servus.shuttles.field;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a field shuttle and a Field Head see and do on one plot, on the server
 * thread: read the plot's state, roll a ripe crop's drops, break it, till
 * the ground under the plot and plant the field's crop; in a Trees field
 * walk a grown tree, roll its drops, remove it and plant saplings.
 */
public final class HarvesterWork {

    private HarvesterWork() {
    }

    /** The plot's state from the block on it and the block under it, read by the field's rules. */
    public static PlotState classify(Level level, BlockPos plot, BlockState state, PlotRules rules) {
        return rules.kind().trees() ? classifyTree(level, plot, state, rules) : classify(level, plot, state);
    }

    /** The plot's state in a Crops field from the block on it and the block under it. */
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

    /** The plot's state in a Crops field as it is in the world now. */
    public static PlotState classify(Level level, BlockPos plot) {
        return classify(level, plot, level.getBlockState(plot));
    }

    /**
     * A Trees field: a log is a grown tree, a sapling on a spot grows, an
     * empty spot on soil is open to plant while the field plants; leaves,
     * hanging parts, saplings and soil off the spots keep the rails' sight
     * open but have no work.
     */
    private static PlotState classifyTree(Level level, BlockPos plot, BlockState state, PlotRules rules) {
        if (state.is(TreeTags.TREE_LOGS)) {
            return rules.unfellable().test(plot, state) ? PlotState.IDLE : PlotState.RIPE;
        }
        boolean spot = isTreeSpot(level, plot, rules);
        if (state.is(BlockTags.SAPLINGS)) {
            return spot ? PlotState.GROWING : PlotState.IDLE;
        }
        if (state.is(BlockTags.LEAVES) || state.is(TreeTags.TREE_ATTACHMENTS)) {
            return PlotState.IDLE;
        }
        if (!isClear(state) || !isSoil(level, plot, rules)) {
            return PlotState.BLOCKED;
        }
        boolean plantable = spot && rules.plants() && (!rules.twoByTwo() || rules.squares());
        return plantable ? PlotState.PLANTABLE : PlotState.IDLE;
    }

    /** Whether the plot is planted in this Trees field: a single spot, or a plot of a square whose four plots all hold tree ground. */
    public static boolean isTreeSpot(Level level, BlockPos plot, PlotRules rules) {
        Predicate<FieldPos> member = pos -> {
            BlockPos at = FieldPositions.toBlockPos(pos);
            return rules.inField().test(at) && level.isLoaded(at) && isTreeGround(level, at, level.getBlockState(at), rules);
        };
        return TreeSpots.planted(FieldPositions.of(plot), rules.twoByTwo(), member);
    }

    /** A sapling or a log, or empty soil the field's sapling grows on. */
    private static boolean isTreeGround(Level level, BlockPos pos, BlockState state, PlotRules rules) {
        return state.is(BlockTags.SAPLINGS) || state.is(TreeTags.TREE_LOGS) || isClear(state) && isSoil(level, pos, rules);
    }

    private static boolean isSoil(Level level, BlockPos plot, PlotRules rules) {
        return rules.soilProbe().defaultBlockState().canSurvive(level, plot);
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

    /**
     * Plants the sapling on every given plot at once, or on none when one of
     * them is not empty soil it grows on; returns how many were planted.
     */
    public static int plantSaplings(ServerLevel level, Collection<BlockPos> plots, ItemStack sapling) {
        if (!(sapling.getItem() instanceof BlockItem item) || !item.getBlock().defaultBlockState().is(BlockTags.SAPLINGS)) {
            return 0;
        }
        BlockState state = item.getBlock().defaultBlockState();
        for (BlockPos plot : plots) {
            if (!isClear(level.getBlockState(plot)) || !state.canSurvive(level, plot)) {
                return 0;
            }
        }
        for (BlockPos plot : plots) {
            clear(level, plot);
            level.setBlockAndUpdate(plot, state);
        }
        level.playSound(null, plots.iterator().next(), state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return plots.size();
    }

    /** What the tree walk sees at a position: unloaded positions and every other block are {@link TreeWalk.Kind#OTHER}. */
    public static TreeWalk.Kind treeKind(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return TreeWalk.Kind.OTHER;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(TreeTags.TREE_LOGS)) {
            return TreeWalk.Kind.LOG;
        }
        if (state.getBlock() instanceof LeavesBlock && !state.getValue(LeavesBlock.PERSISTENT)) {
            return TreeWalk.Kind.NATURAL_LEAVES;
        }
        return state.is(TreeTags.TREE_ATTACHMENTS) ? TreeWalk.Kind.ATTACHMENT : TreeWalk.Kind.OTHER;
    }

    /**
     * Walks the tree standing on the base plots as the world is now.
     *
     * @param otherBase whether a position on the base level is the base of another spot or square
     */
    public static TreeWalk.Result walkTree(Level level, Collection<BlockPos> base, Predicate<BlockPos> otherBase, int maxLogs, int maxBlocks) {
        return TreeWalk.walk(FieldPositions.of(base), pos -> treeKind(level, FieldPositions.toBlockPos(pos)),
                pos -> otherBase.test(FieldPositions.toBlockPos(pos)), maxLogs, maxBlocks);
    }

    /** Every removed block's drops, rolled as if broken without a tool, so leaves give saplings, sticks and apples by chance. */
    public static List<ItemStack> treeDrops(ServerLevel level, TreeWalk.Felled tree) {
        List<ItemStack> drops = new ArrayList<>();
        for (BlockPos pos : treeBlocks(tree)) {
            drops.addAll(Block.getDrops(level.getBlockState(pos), level, pos, level.getBlockEntity(pos)));
        }
        drops.removeIf(ItemStack::isEmpty);
        return drops;
    }

    /** The felled blocks in the order they are removed: hanging parts, leaves, then logs. */
    public static List<BlockPos> treeBlocks(TreeWalk.Felled tree) {
        List<BlockPos> blocks = new ArrayList<>(tree.size());
        for (Collection<FieldPos> part : List.of(tree.attachments(), tree.leaves(), tree.logs())) {
            for (FieldPos pos : part) {
                blocks.add(FieldPositions.toBlockPos(pos));
            }
        }
        return blocks;
    }

    /**
     * Removes the felled blocks without drops or neighbour updates; shape
     * updates still let the leaves left around recompute their distance, and
     * waterlogged blocks leave their water.
     */
    public static void removeTree(ServerLevel level, TreeWalk.Felled tree) {
        for (BlockPos pos : treeBlocks(tree)) {
            level.setBlock(pos, level.getBlockState(pos).getFluidState().createLegacyBlock(), Block.UPDATE_CLIENTS);
        }
    }

    /** The wood break sound with break particles of that block at the tree's base. */
    public static void showFelled(ServerLevel level, BlockPos base, BlockState log) {
        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, base, Block.getId(log));
    }
}
