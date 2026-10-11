package io.github.dirnei.mc.servus.storage.rail;

import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.redstone.Orientation;
import java.util.OptionalInt;
import java.util.List;
import io.github.dirnei.mc.servus.storage.menu.ElevatorMenu;
import io.github.dirnei.mc.servus.storage.shuttle.LiftUpgrades;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    /** An empty hand opens the shaft's upgrades, which the lowest segment keeps. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            ElevatorBlockEntity bottom = bottomOf(level, pos);
            if (bottom != null) {
                player.openMenu(bottom, buf -> ElevatorMenu.writeOpening(buf, bottom));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** The lowest segment of the shaft the segment at pos belongs to, which keeps the upgrades; null when there is none. */
    public static ElevatorBlockEntity bottomOf(Level level, BlockPos pos) {
        int bottomY = LiftUpgrades.bottom(pos.getY(), y -> level.getBlockState(pos.atY(y)).getBlock() instanceof ElevatorBlock);
        return level.getBlockEntity(pos.atY(bottomY)) instanceof ElevatorBlockEntity bottom ? bottom : null;
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
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (!level.isClientSide() && !state.canSurvive(level, pos)) {
            dropResources(state, level, pos, level.getBlockEntity(pos));
            level.removeBlock(pos, false);
        }
    }

    /**
     * A new segment joins the shaft above it: that shaft's lowest segment was
     * the one above, so its upgrades move down to the lowest segment of the
     * joined shaft; what finds no free slot drops.
     */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide() || oldState.is(this)) {
            return;
        }
        BlockPos above = pos.above();
        if (!(level.getBlockEntity(above) instanceof ElevatorBlockEntity upper) || upper.upgradeCount() == 0) {
            return;
        }
        int bottomY = LiftUpgrades.bottom(pos.getY(), y -> level.getBlockState(pos.atY(y)).getBlock() instanceof ElevatorBlock);
        // The new segment's block entity is not made yet; asking the level for it makes it now.
        if (level.getBlockEntity(pos.atY(bottomY)) instanceof ElevatorBlockEntity bottom) {
            bottom.upgrades().insertAll(upper.upgrades().takeAll()).forEach(stack -> Block.popResource(level, pos, stack));
        }
    }

    /**
     * A removed lowest segment hands its upgrades to the segment above when
     * that one still stands (the new block holds it up) and so becomes the new
     * lowest. Otherwise the shaft above breaks like scaffolding and the
     * upgrades drop where the lowest segment was.
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof ElevatorBlockEntity removed && removed.upgradeCount() > 0) {
            List<ItemStack> items = removed.upgrades().takeAll();
            OptionalInt heir = LiftUpgrades.heirOnRemove(pos.getY(), y -> level.getBlockState(pos.atY(y)).getBlock() instanceof ElevatorBlock);
            BlockPos heirPos = heir.isPresent() ? pos.atY(heir.getAsInt()) : null;
            if (heirPos != null && level.getBlockState(heirPos).canSurvive(level, heirPos)
                    && level.getBlockEntity(heirPos) instanceof ElevatorBlockEntity next) {
                items = next.upgrades().insertAll(items);
            }
            items.forEach(stack -> Block.popResource(level, pos, stack));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElevatorBlockEntity(pos, state);
    }
}
