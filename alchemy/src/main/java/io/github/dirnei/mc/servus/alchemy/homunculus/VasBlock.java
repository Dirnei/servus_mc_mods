package io.github.dirnei.mc.servus.alchemy.homunculus;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Vas Hermeticum: the sealed glass flask the homunculus lives in. It stands only on a Receptaculum and breaks
 * when the Receptaculum goes. {@link #FREED} shows the broken seal to everyone once a Nether Star freed it.
 */
public class VasBlock extends BaseEntityBlock {

    public static final MapCodec<VasBlock> CODEC = simpleCodec(VasBlock::new);
    public static final BooleanProperty FREED = BooleanProperty.create("freed");
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 11, 15), Block.box(6, 11, 6, 10, 17, 10));

    public VasBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FREED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FREED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).getBlock() instanceof ReceptaculumBlock;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VasBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ServusAlchemyBlockEntities.VAS_HERMETICUM.get(), VasBlockEntity::serverTick);
    }

    /** A Nether Star frees a grown homunculus; any other item, or a homunculus not ready, does nothing here. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Items.NETHER_STAR) || !(level.getBlockEntity(pos) instanceof VasBlockEntity vas) || !vas.canBeFreed()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && vas.free(player)) {
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }
}
