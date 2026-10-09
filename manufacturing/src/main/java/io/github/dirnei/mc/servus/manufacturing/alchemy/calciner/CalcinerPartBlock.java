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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
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

    /** The Firebox takes fuel by hand. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (role == Role.FIREBOX && state.getValue(FORMED) && stack.getBurnTime(null) > 0) {
            CalcinerBlockEntity calciner = CalcinerPorts.calciner(level, pos, state);
            if (calciner != null) {
                if (!level.isClientSide()) {
                    player.setItemInHand(hand, calciner.insertFuel(stack));
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        if (CalcinerPorts.principle(role) != null && state.getValue(FORMED)
                && stack.getCapability(Capabilities.FluidHandler.ITEM) != null) {
            // Empty containers fill with the port's principle; filled ones do nothing here.
            IFluidHandler port = CalcinerPorts.fluid(level, pos, state);
            if (port != null && !level.isClientSide()) {
                FluidUtil.interactWithFluidHandler(player, hand, port);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Flames at the grate of a burning Firebox, smoke over a working Hood. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(FORMED) || !state.getValue(LIT)) {
            return;
        }
        Direction front = state.getValue(FACING);
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        if (role == Role.FIREBOX) {
            double side = (random.nextDouble() - 0.5) * 0.5;
            double fx = x + front.getStepX() * 0.45 + front.getClockWise().getStepX() * side;
            double fz = z + front.getStepZ() * 0.45 + front.getClockWise().getStepZ() * side;
            level.addParticle(ParticleTypes.FLAME, fx, pos.getY() + 0.25 + random.nextDouble() * 0.3, fz, 0, 0, 0);
            if (random.nextDouble() < 0.1) {
                level.playLocalSound(x, pos.getY(), z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }
        } else if (role == Role.HOOD && random.nextFloat() < 0.6F) {
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                    x + (random.nextDouble() - 0.5) * 0.2, pos.getY() + 1.0, z + (random.nextDouble() - 0.5) * 0.2, 0, 0.05, 0);
        }
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
