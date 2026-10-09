package io.github.dirnei.mc.servus.manufacturing.alchemy.calciner;

import io.github.dirnei.mc.servus.foundation.item.Wrenchable;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Role;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/** One custom part of the Calciner structure: the Firebox, Ash Pan, Alembic, Receiver or Sulfur Hood. */
public class CalcinerPartBlock extends Block implements Wrenchable {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final Role role;

    public CalcinerPartBlock(Role role, Properties properties) {
        super(properties);
        this.role = role;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORMED, false).setValue(LIT, false));
    }

    public Role role() {
        return role;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, LIT);
    }

    /** The Calciner block entity of the formed structure this part belongs to, or null. */
    public static CalcinerBlockEntity calciner(Level level, BlockState state, BlockPos pos) {
        return CalcinerStructure.controllerOf(state, pos)
                .map(controller -> level.getBlockEntity(controller) instanceof CalcinerBlockEntity calciner ? calciner : null)
                .orElse(null);
    }

    /** The Firebox takes fuel by hand. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (role == Role.FIREBOX && state.getValue(FORMED) && stack.getBurnTime(null) > 0) {
            CalcinerBlockEntity calciner = calciner(level, state, pos);
            if (calciner != null) {
                if (!level.isClientSide()) {
                    player.setItemInHand(hand, calciner.insertFuel(stack));
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** The carrot never turns or configures a part of the Calciner. */
    @Override
    public InteractionResult onWrench(UseOnContext context, BlockState state) {
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            CalcinerStructure.broken(level, state, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** A formed part holds the structure together and cannot be pushed. */
    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return state.getValue(FORMED) ? PushReaction.BLOCK : null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
}
