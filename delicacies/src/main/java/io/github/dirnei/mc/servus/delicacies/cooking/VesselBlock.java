package io.github.dirnei.mc.servus.delicacies.cooking;

import io.github.dirnei.mc.servus.foundation.machine.PartShapes;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A cooking vessel placed on top of a stove tile: ingredients go in by right click, the result comes out with an
 * empty hand. It cooks only over a hot firebox, and steams while it does.
 */
public abstract class VesselBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** The vessel has a recipe and sits over a hot firebox. */
    public static final BooleanProperty COOKING = BooleanProperty.create("cooking");

    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    protected VesselBlock(Properties properties, float[][] parts) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(COOKING, false));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            shapes.put(direction, PartShapes.shapeOf(parts, direction));
        }
    }

    /** The height of the vessel's top in pixels, where the steam rises. */
    protected abstract float topHeight();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, COOKING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> {
            if (entity instanceof VesselBlockEntity<?> vessel) {
                vessel.serverTick();
            }
        };
    }

    /** An ingredient or a fluid container goes in; any other item passes. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof VesselBlockEntity<?> vessel)) {
            return InteractionResult.PASS;
        }
        if (vessel.useFluidContainer(player, hand)) {
            return InteractionResult.SUCCESS;
        }
        if (!vessel.accepts(stack)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack left = vessel.insertInputs(stack.copy());
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, left);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** An empty hand takes the result, or the last ingredient when there is none. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof VesselBlockEntity<?> vessel) {
            ItemStack taken = vessel.takeOut();
            if (!taken.isEmpty()) {
                player.getInventory().placeItemBackInInventory(taken);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Steam over a vessel that cooks. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(COOKING) || random.nextFloat() > 0.6F) {
            return;
        }
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
        level.addParticle(ParticleTypes.WHITE_SMOKE, x, pos.getY() + topHeight() / 16.0, z, 0, 0.02, 0);
    }
}
