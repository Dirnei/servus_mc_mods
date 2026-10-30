package io.github.dirnei.mc.servus.alchemy.venter;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.alchemy.PlayerItems;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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

/**
 * The Cucurbit, the sealed gourd in the middle of the Venter Equinus and its controller: rot goes in here
 * and putrefies while the bed around it stays warm. No GUI; worked by hand and pipe.
 */
public class CucurbitBlock extends BaseEntityBlock {

    public static final MapCodec<CucurbitBlock> CODEC = simpleCodec(CucurbitBlock::new);

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** The way the formed bed's front faces; before forming, the first facing the Journal checks. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Whether a batch is putrefying, for the steam. */
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);
    /** The inside of the bed in block units from the Cucurbit's corner, where the steam rises. */
    private static final double BED_FROM = -13.45 / 16;
    private static final double BED_SIZE = 42.9 / 16;
    private static final int STEAM_CHANCE = 3;

    public CucurbitBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(FACING, Direction.NORTH).setValue(WORKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, FACING, WORKING);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VenterEquinusBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ServusAlchemyBlockEntities.VENTER_EQUINUS.get(), VenterEquinusBlockEntity::serverTick);
    }

    /** The Journal forms the bed; bedding and rot go in, as on every block of the bed. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        return VenterStructure.use(stack, level, pos, state, player, hand);
    }

    /** Sneaking with an empty hand takes the rot back and starts the batch over. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!state.getValue(FORMED) || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof VenterEquinusBlockEntity venter) {
            venter.takeRot().forEach(stack -> PlayerItems.give(player, stack));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof VenterEquinusBlockEntity venter) {
            venter.setOwner(player.getUUID());
        }
    }

    /** Steam rises from the straw while a batch putrefies. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(WORKING) || random.nextInt(STEAM_CHANCE) != 0) {
            return;
        }
        double height = level.getBlockEntity(pos) instanceof VenterEquinusBlockEntity venter ? venter.beddingHeight() / 16.0 : 0.25;
        double x = pos.getX() + BED_FROM + random.nextDouble() * BED_SIZE;
        double z = pos.getZ() + BED_FROM + random.nextDouble() * BED_SIZE;
        level.addParticle(ParticleTypes.WHITE_SMOKE, x, pos.getY() + height + 0.1, z, 0.0, 0.02, 0.0);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        VenterStructure.broken(level, state, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
