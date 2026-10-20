package io.github.dirnei.mc.servus.delicacies.board;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.delicacies.cooking.VesselParts;
import io.github.dirnei.mc.servus.foundation.machine.PartShapes;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** The Cutting Board: lay a vegetable on it and cut it with the Kitchen Knife, one item per click. */
public class CuttingBoardBlock extends BaseEntityBlock {

    public static final MapCodec<CuttingBoardBlock> CODEC = simpleCodec(CuttingBoardBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    public CuttingBoardBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            shapes.put(direction, PartShapes.shapeOf(new float[][] {VesselParts.BOARD}, direction));
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CuttingBoardBlockEntity(pos, state);
    }

    /** A knife cuts, a cuttable item goes on the board; anything else passes. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board)) {
            return InteractionResult.PASS;
        }
        if (board.cutsWith(stack)) {
            if (!level.isClientSide()) {
                board.cut(player, hand);
            }
            return InteractionResult.SUCCESS;
        }
        if (!board.cuttable(stack)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack left = board.place(stack.copy());
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, left);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** An empty hand takes the pieces, or the item to cut when there are none. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board) {
            ItemStack taken = board.takeOut();
            if (!taken.isEmpty()) {
                player.getInventory().placeItemBackInInventory(taken);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
