package io.github.dirnei.mc.servus.alchemy.athanor.ritual;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Rubedo Obelisk, two blocks tall, which the Rubedo ritual grows out of Obsidian: a dark red crystal on a plinth of
 * the obsidian. It has no item. Breaking either half removes both, and the lower half drops the Obsidian. The lower half
 * has a block entity whose renderer draws the obelisk; embers rise around it.
 */
public class RubedoObeliskBlock extends Block implements EntityBlock {

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    /** Red of the embers and the obelisk's hot glow. */
    public static final int HOT = 0xE04020;
    public static final int LIGHT = 10;
    /** Height of the obsidian plinth in pixels; the obelisk stands on it. */
    public static final float PLINTH = 3;

    private static final VoxelShape LOWER = Shapes.or(Block.box(0, 0, 0, 16, PLINTH, 16), Block.box(3.5, PLINTH, 3.5, 12.5, 16, 12.5));
    private static final VoxelShape UPPER = Block.box(5, 0, 5, 11, 16, 11);

    public RubedoObeliskBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER;
    }

    /** A half whose other half is gone goes too. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        Direction toOther = half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
        if (direction == toOther && !(neighbourState.is(this) && neighbourState.getValue(HALF) != half)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    /** A creative player breaking the upper half takes the lower half without its drop. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            BlockState lower = level.getBlockState(below);
            if (lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, below, Block.getId(lower));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** A broken half makes the Athanor of its build fall back. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        AthanorBlockEntity.grownBlockBroken(level, pos, state);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    /** Only the lower half has a block entity; it draws the whole obelisk. */
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new RubedoObeliskBlockEntity(pos, state) : null;
    }

    /** Embers rise around the obelisk. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) != DoubleBlockHalf.LOWER || random.nextInt(2) != 0) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = 0.3 + random.nextDouble() * 0.3;
        double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
        double y = pos.getY() + 0.2 + random.nextDouble() * 1.6;
        double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
        level.addParticle(new DustParticleOptions(HOT, 0.5F), x, y, z, 0, 0.03, 0);
    }
}
