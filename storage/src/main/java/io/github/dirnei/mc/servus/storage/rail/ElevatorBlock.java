package io.github.dirnei.mc.servus.storage.rail;

import io.github.dirnei.mc.servus.storage.menu.ElevatorMenu;
import io.github.dirnei.mc.servus.storage.shuttle.LiftUpgrades;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One segment of a rail elevator shaft: four corner posts around an open
 * middle. Segments stack vertically into a shaft with one lift car and take
 * rails on their four sides; the network joins anything touching them.
 * Entities pass through and climb the shaft, and stand on each level from
 * above like on scaffolding.
 */
public class ElevatorBlock extends ConnectableBlock {

    public static final MapCodec<ElevatorBlock> CODEC = simpleCodec(ElevatorBlock::new);

    /** The walkable top, as thick as a scaffolding top. */
    private static final VoxelShape TOP = Block.box(0, 14, 0, 16, 16, 16);

    public ElevatorBlock(Properties properties) {
        super(properties, 4);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean connectsTo(LevelAccessor level, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        if (direction.getAxis().isVertical()) {
            return neighborState.getBlock() instanceof ElevatorBlock;
        }
        return neighborState.getBlock() instanceof RailBlock;
    }

    /** An item in hand keeps its own use: an elevator builds the shaft up, the carrot dismantles. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        return stack.isEmpty() ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    /** An empty hand opens the shaft's upgrades, which the lowest segment keeps. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            int bottomY = LiftUpgrades.bottom(pos.getY(), y -> level.getBlockState(pos.atY(y)).getBlock() instanceof ElevatorBlock);
            if (level.getBlockEntity(pos.atY(bottomY)) instanceof ElevatorBlockEntity bottom) {
                player.openMenu(bottom, buf -> ElevatorMenu.writeOpening(buf, bottom));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** The whole block is the outline, so a segment is easy to target, like scaffolding. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    /**
     * Like scaffolding: an entity above the segment stands on a thin plate at
     * its top unless it is descending; inside or from the side it walks
     * through, and the climbable tag lets it climb.
     */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ShaftFooting.stands(context.isAbove(Shapes.block(), pos, true), context.isDescending()) ? TOP : Shapes.empty();
    }

    /** Sneaking sinks through the shaft instead of holding on like on a ladder. */
    @Override
    public boolean isScaffolding(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return true;
    }

    /** A segment stands on a solid block or on another segment. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).getBlock() instanceof ElevatorBlock || canSupportRigidBlock(level, below);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide() && !state.canSurvive(level, pos)) {
            dropResources(state, level, pos, level.getBlockEntity(pos));
            level.removeBlock(pos, false);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElevatorBlockEntity(pos, state);
    }
}
