package io.github.dirnei.mc.servus.delicacies.crock;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.machine.PartShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Fermenting Crock: brown stoneware with a water seal. Ingredients go in by right click while it is open; once they
 * match a fermenting recipe the lid goes on until the time is up. It wakes through scheduled block ticks.
 */
public class FermentingCrockBlock extends BaseEntityBlock {

    public static final MapCodec<FermentingCrockBlock> CODEC = simpleCodec(FermentingCrockBlock::new);
    /** The lid is on and it ferments. */
    public static final BooleanProperty SEALED = BooleanProperty.create("sealed");

    private static final VoxelShape OPEN_SHAPE = PartShapes.shapeOf(CrockParts.shape(), Direction.NORTH);
    private static final VoxelShape SEALED_SHAPE = PartShapes.shapeOf(CrockParts.sealedShape(), Direction.NORTH);

    public FermentingCrockBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SEALED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SEALED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SEALED) ? SEALED_SHAPE : OPEN_SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FermentingCrockBlockEntity(pos, state);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof FermentingCrockBlockEntity crock) {
            crock.check();
        }
    }

    /** Open: an ingredient or a fluid container goes in. Sealed: it tells what it makes. Anything else passes. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof FermentingCrockBlockEntity crock)) {
            return InteractionResult.PASS;
        }
        if (crock.sealed()) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(crock.status());
            }
            return InteractionResult.PASS;
        }
        if (crock.useFluidContainer(player, hand)) {
            return InteractionResult.SUCCESS;
        }
        if (!crock.accepts(stack)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack left = crock.insertInputs(stack.copy());
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, left);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Sealed: tells what it makes and how long it takes. Open: takes the result, or the last ingredient. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof FermentingCrockBlockEntity crock) {
            if (crock.sealed()) {
                player.sendOverlayMessage(crock.status());
            } else {
                ItemStack taken = crock.takeOut();
                if (taken.isEmpty()) {
                    player.sendOverlayMessage(crock.status());
                } else {
                    player.getInventory().placeItemBackInInventory(taken);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Now and then a bubble pops in the water seal of a sealed crock. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(SEALED) || random.nextFloat() > 0.15F) {
            return;
        }
        double along = 3.5 + random.nextDouble() * 9;
        double across = random.nextBoolean() ? 3.5 : 12.5;
        boolean onX = random.nextBoolean();
        double x = pos.getX() + (onX ? along : across) / 16.0;
        double z = pos.getZ() + (onX ? across : along) / 16.0;
        level.addParticle(ParticleTypes.BUBBLE_POP, x, pos.getY() + 9.0 / 16.0, z, 0, 0.01, 0);
    }
}
