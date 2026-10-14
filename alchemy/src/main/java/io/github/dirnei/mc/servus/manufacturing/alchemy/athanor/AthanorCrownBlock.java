package io.github.dirnei.mc.servus.manufacturing.alchemy.athanor;

import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The invisible space above a formed Athanor, where the item lies. Picking tests only the block in
 * each cell a look passes, so this block takes the look there: its outline reaches down over the
 * Athanor, use goes to the Athanor below and breaking it breaks the Athanor. It has no collision.
 */
public class AthanorCrownBlock extends Block {

    /** The same two block box the formed Athanor shows, seen from the cell above. */
    private static final VoxelShape OUTLINE = Block.box(0, -16, 0, 16, 16, 16);

    public AthanorCrownBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockState below = level.getBlockState(pos.below());
        if (below.getBlock() instanceof AthanorBlock) {
            return below.useItemOn(stack, level, player, hand, hitResult.withPosition(pos.below()));
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockState below = level.getBlockState(pos.below());
        if (below.getBlock() instanceof AthanorBlock) {
            return below.useWithoutItem(level, player, hitResult.withPosition(pos.below()));
        }
        return InteractionResult.PASS;
    }

    /** Breaking the space above breaks the Athanor, with its drop unless in creative. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockState(pos.below()).getBlock() instanceof AthanorBlock) {
            level.destroyBlock(pos.below(), !player.isCreative(), player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Otherwise gone (an explosion, a command): the Athanor below unforms. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (level.getBlockEntity(pos.below()) instanceof AthanorBlockEntity athanor) {
            athanor.unform();
        }
    
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    /** Without a formed Athanor below it has no reason to stay. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        BlockState below = level.getBlockState(pos.below());
        if (!level.isClientSide() && !(below.getBlock() instanceof AthanorBlock && below.getValue(AthanorBlock.FORMED))) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(ServusManufacturingItems.ATHANOR.get());
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
