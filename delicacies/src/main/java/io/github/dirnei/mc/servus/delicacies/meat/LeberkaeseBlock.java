package io.github.dirnei.mc.servus.delicacies.meat;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenTags;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.foundation.machine.PartShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A placed Leberkaese loaf. A knife cuts one slice off its west end per click into the player's inventory; the last
 * slice removes the block. Nothing else takes from it.
 */
public class LeberkaeseBlock extends Block {

    public static final MapCodec<LeberkaeseBlock> CODEC = simpleCodec(LeberkaeseBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Slices already cut off. */
    public static final IntegerProperty CUT = IntegerProperty.create("cut", 0, LoafSlices.MAX_CUT);

    private final VoxelShape[][] shapes = new VoxelShape[LoafSlices.SLICES][4];

    public LeberkaeseBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CUT, 0));
        for (int cut = 0; cut <= LoafSlices.MAX_CUT; cut++) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                shapes[cut][direction.get2DDataValue()] = PartShapes.shapeOf(new float[][] {LoafSlices.box(cut)}, direction);
            }
        }
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CUT);
    }

    /** Turned toward the player, and only on a block with a solid top. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!restsOn(context.getLevel(), context.getClickedPos())) {
            return null;
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private static boolean restsOn(LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[state.getValue(CUT)][state.getValue(FACING).get2DDataValue()];
    }

    /** A knife cuts one slice; anything else tries the empty hand, which does nothing. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(KitchenTags.KNIVES)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel server) {
            cutSlice(server, pos, state, player, hand);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    private static void cutSlice(ServerLevel level, BlockPos pos, BlockState state, Player player, InteractionHand hand) {
        ItemStack slice = new ItemStack(ServusDelicaciesItems.LEBERKAESE_SLICE.get());
        player.getInventory().placeItemBackInInventory(slice.copy());
        player.getItemInHand(hand).hurtAndBreak(1, player, hand);
        level.playSound(null, pos, SoundType.WOOL.getHitSound(), SoundSource.BLOCKS, 1.0F, 1.2F);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, slice.getItem()), pos.getX() + 0.5, pos.getY() + 0.2,
                pos.getZ() + 0.5, 6, 0.15, 0.05, 0.15, 0.05);
        int cut = state.getValue(CUT);
        if (LoafSlices.isLastSlice(cut)) {
            level.removeBlock(pos, false);
            level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
        } else {
            level.setBlock(pos, state.setValue(CUT, LoafSlices.next(cut)), Block.UPDATE_ALL);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
    }
}
