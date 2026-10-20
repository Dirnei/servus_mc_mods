package io.github.dirnei.mc.servus.brewery.malting;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A low spruce floor on which soaked grain sprouts into green malt. Soaked grain is spread by right click, an empty
 * hand turns it or collects the green malt, and sneaking with an empty hand takes back whatever lies there.
 */
public class MaltingFloorBlock extends BaseEntityBlock {

    public static final MapCodec<MaltingFloorBlock> CODEC = simpleCodec(MaltingFloorBlock::new);
    public static final EnumProperty<FloorContent> CONTENT = EnumProperty.create("content", FloorContent.class);
    /** Height of the floor in pixels. */
    public static final int HEIGHT = 3;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, HEIGHT, 16);

    public MaltingFloorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CONTENT, FloorContent.EMPTY));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONTENT);
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MaltingFloorBlockEntity(pos, state);
    }

    /** Spreads soaked grain; any other item goes on to the empty hand use. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (ServusBreweryRecipes.germinating(level, stack).isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof MaltingFloorBlockEntity floor)) {
            return InteractionResult.FAIL;
        }
        int spread = floor.spread(stack);
        if (spread == 0) {
            return InteractionResult.FAIL;
        }
        stack.consume(spread, player);
        level.playSound(null, pos, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /** With an empty main hand: sneaking takes everything, green malt is collected, soaked grain is turned. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        FloorContent content = state.getValue(CONTENT);
        if (!player.getMainHandItem().isEmpty() || content == FloorContent.EMPTY) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof MaltingFloorBlockEntity floor)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown() || content == FloorContent.GREEN) {
            player.getInventory().placeItemBackInInventory(floor.takeAll());
            level.playSound(null, pos, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (floor.turn()) {
            level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            ((ServerLevel) level).sendParticles(ParticleTypes.COMPOSTER,
                    pos.getX() + 0.5, pos.getY() + HEIGHT / 16.0 + 0.05, pos.getZ() + 0.5, 8, 0.3, 0.02, 0.3, 0);
        }
        return InteractionResult.SUCCESS;
    }

    /** The scheduled germination check. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof MaltingFloorBlockEntity floor) {
            floor.check();
        }
    }
}
