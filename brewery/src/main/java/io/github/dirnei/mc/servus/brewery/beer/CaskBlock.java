package io.github.dirnei.mc.servus.brewery.beer;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import io.github.dirnei.mc.servus.foundation.machine.PartShapes;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/**
 * A wooden barrel with a brass tap on its front. Buckets and pipes fill it with beer; an empty glass held to the tap is
 * filled, and an empty hand tells what the Cask holds.
 */
public class CaskBlock extends BaseEntityBlock {

    public static final MapCodec<CaskBlock> CODEC = simpleCodec(CaskBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** The barrel and the tap within the block, in pixels of the north facing model. */
    public static final float[] BARREL = {1, 0, 1, 15, 16, 15};
    public static final float[] TAP = {7, 2.5F, 0, 9, 8, 1};
    /** Distance of the barrel's front from the block centre in blocks; clicks this far forward hit the front or the tap. */
    private static final double FRONT = 7 / 16.0 - 0.01;
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            SHAPES.put(facing, PartShapes.shapeOf(new float[][] {BARREL, TAP}, facing));
        }
    }

    public CaskBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The tap faces the player who places the Cask. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CaskBlockEntity(pos, state);
    }

    /** Whether {@code hit} lies on the front of the Cask, where the tap is. */
    public static boolean atTap(BlockState state, BlockPos pos, Vec3 hit) {
        Direction front = state.getValue(FACING);
        double forward = (hit.x - pos.getX() - 0.5) * front.getStepX() + (hit.z - pos.getZ() - 0.5) * front.getStepZ();
        return forward >= FRONT;
    }

    /** A fluid container fills or empties the Cask; an empty glass at the tap is filled; a glass elsewhere passes. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof CaskBlockEntity cask)) {
            return InteractionResult.PASS;
        }
        if (Transfer.isFluidContainer(stack)) {
            if (!level.isClientSide()) {
                FluidUtil.interactWithFluidHandler(player, hand, pos, cask.tank(), null);
            }
            return InteractionResult.SUCCESS;
        }
        if (!ServusBreweryRecipes.isGlass(level, stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!atTap(state, pos, hitResult.getLocation())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        switch (cask.tap(stack)) {
            case CaskBlockEntity.Tap.Filled filled -> {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, filled.glass()));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
            case CaskBlockEntity.Tap.Refused refused -> player.sendOverlayMessage(refused.reason());
        }
        return InteractionResult.SUCCESS;
    }

    /** An empty hand tells how much of which beer the Cask holds. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof CaskBlockEntity cask) {
            player.sendOverlayMessage(cask.status());
        }
        return InteractionResult.SUCCESS;
    }
}
