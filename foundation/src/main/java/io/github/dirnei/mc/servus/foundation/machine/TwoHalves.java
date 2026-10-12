package io.github.dirnei.mc.servus.foundation.machine;

import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Machines two blocks tall, placed and broken as one like a door: the Burner
 * Pump, the Electric Pump and the Coal Generator. The lower half is the
 * machine (block entity, inventory, capabilities); the upper half is look only
 * and forwards clicks and the carrot to the lower half. A block calls these
 * from its own overrides, since the machines extend different base classes.
 *
 * <p>Follows the vanilla {@code DoorBlock}: placement returns null without a
 * replaceable block above, {@code setPlacedBy} sets the upper half,
 * {@code updateShape} turns a half without its partner into air (which
 * {@code Block.updateOrDestroy} destroys with drops), and
 * {@code playerWillDestroy} removes the bottom without drops in creative or
 * without the right tool, as {@code DoublePlantBlock.preventDropFromBottomPart}
 * does.
 */
public final class TwoHalves {

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private TwoHalves() {
    }

    public static boolean isUpper(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER;
    }

    /** The position of the lower half, the one with the block entity. */
    public static BlockPos lowerPos(BlockState state, BlockPos pos) {
        return isUpper(state) ? pos.below() : pos;
    }

    /** Whether the block above the clicked position can take the upper half. */
    public static boolean canPlace(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        return pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context);
    }

    /** Puts the upper half above a freshly placed lower half. */
    public static void placeUpper(Level level, BlockPos pos, BlockState upper) {
        level.setBlock(pos.above(), upper, Block.UPDATE_ALL);
    }

    /**
     * For {@code updateShape}: null when {@code direction} does not point at
     * the partner, air when the partner is gone (so the half breaks with it),
     * else {@code state} unchanged; the block copies what the upper half
     * mirrors from the lower one.
     */
    public static BlockState keepTogether(Block block, BlockState state, Direction direction, BlockState neighbour) {
        boolean upper = isUpper(state);
        if (direction != (upper ? Direction.DOWN : Direction.UP)) {
            return null;
        }
        if (!neighbour.is(block) || isUpper(neighbour) == upper) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    /**
     * For {@code playerWillDestroy}: breaking the upper half in creative or
     * without the right tool removes the lower half without drops, so no
     * machine item drops. In survival with the right tool the lower half
     * breaks through {@link #keepTogether} and drops the item.
     */
    public static void breakPartner(Block block, Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide() || !isUpper(state) || !(player.isCreative() || !player.hasCorrectToolForDrops(state, level, pos))) {
            return;
        }
        BlockPos below = pos.below();
        BlockState lower = level.getBlockState(below);
        if (lower.is(block) && !isUpper(lower)) {
            level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            level.levelEvent(player, 2001, below, Block.getId(lower));
        }
    }

    /** For {@code onWrench}: on the upper half, runs {@code lowerWrench} as if the lower half was clicked. */
    public static InteractionResult forwardWrench(UseOnContext context, BlockState state,
                                                  BiFunction<UseOnContext, BlockState, InteractionResult> lowerWrench) {
        if (isUpper(state) && context.getPlayer() != null) {
            BlockPos below = context.getClickedPos().below();
            BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), below, context.isInside());
            return lowerWrench.apply(new UseOnContext(context.getPlayer(), context.getHand(), hit), context.getLevel().getBlockState(below));
        }
        return lowerWrench.apply(context, state);
    }
}
