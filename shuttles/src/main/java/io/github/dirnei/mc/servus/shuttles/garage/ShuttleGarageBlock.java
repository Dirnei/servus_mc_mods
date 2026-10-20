package io.github.dirnei.mc.servus.shuttles.garage;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.rail.RailBlock;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import io.github.dirnei.mc.servus.foundation.wood.Wood;

/**
 * Holds shuttles behind a doorway with a rail. Right click opens the slot GUI.
 * While it holds shuttles it cannot be broken or dismantled.
 */
public class ShuttleGarageBlock extends BaseEntityBlock implements Wrenchable {

    public static final MapCodec<ShuttleGarageBlock> CODEC = simpleCodec(ShuttleGarageBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public ShuttleGarageBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(StorageNodeBlocks.ONLINE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, StorageNodeBlocks.ONLINE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Side doorway = GarageFacing.doorway(clickedRail(context), railsBeside(context), Conversions.toSide(context.getHorizontalDirection()));
        return defaultBlockState().setValue(FACING, Conversions.toDirection(doorway).getOpposite());
    }

    /** The side towards the rail whose side face the player clicked to place the garage. */
    private static Optional<Side> clickedRail(BlockPlaceContext context) {
        Direction towardsRail = context.getClickedFace().getOpposite();
        if (!towardsRail.getAxis().isHorizontal()) {
            return Optional.empty();
        }
        BlockPos clicked = context.getClickedPos().relative(towardsRail);
        return context.getLevel().getBlockState(clicked).getBlock() instanceof RailBlock
                ? Optional.of(Conversions.toSide(towardsRail))
                : Optional.empty();
    }

    private static Set<Side> railsBeside(BlockPlaceContext context) {
        Set<Side> sides = EnumSet.noneOf(Side.class);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (context.getLevel().getBlockState(context.getClickedPos().relative(direction)).getBlock() instanceof RailBlock) {
                sides.add(Conversions.toSide(direction));
            }
        }
        return sides;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShuttleGarageBlockEntity(pos, state);
    }

    /** Whether the garage holds shuttles and therefore stays put. */
    public static boolean occupied(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ShuttleGarageBlockEntity garage && garage.shuttleCount() > 0;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem) {
            return InteractionResult.PASS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ShuttleGarageBlockEntity garage) {
            player.openMenu(garage, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        boolean dismantle = player != null && player.isShiftKeyDown();
        if (!level.isClientSide()) {
            if (dismantle) {
                if (occupied(level, pos)) {
                    player.sendOverlayMessage(Component.translatable("message.servus_shuttles.garage.occupied"));
                } else {
                    Block.popResource(level, pos, Wood.stamp(new ItemStack(this), level.getBlockEntity(pos)));
                    level.removeBlock(pos, false);
                }
            } else {
                level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), Block.UPDATE_ALL);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
