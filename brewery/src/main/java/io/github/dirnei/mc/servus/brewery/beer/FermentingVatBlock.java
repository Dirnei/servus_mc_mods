package io.github.dirnei.mc.servus.brewery.beer;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/**
 * An open spruce vat with iron bands that ferments wort into beer. Buckets fill it with wort and take the beer, Yeast
 * goes in by hand, an empty hand tells what it holds and sneaking takes the Yeast back between batches.
 */
public class FermentingVatBlock extends BaseEntityBlock {

    public static final MapCodec<FermentingVatBlock> CODEC = simpleCodec(FermentingVatBlock::new);
    public static final EnumProperty<VatContent> CONTENT = EnumProperty.create("content", VatContent.class);
    /** Height of the vat's rim in pixels. */
    public static final int HEIGHT = 14;
    /** Height of the contents' surface in pixels. */
    public static final int SURFACE = 12;
    private static final VoxelShape SHAPE = Shapes.join(Block.box(0, 0, 0, 16, HEIGHT, 16), Block.box(1, 1, 1, 15, HEIGHT, 15),
            BooleanOp.ONLY_FIRST);

    public FermentingVatBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CONTENT, VatContent.EMPTY));
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
        return new FermentingVatBlockEntity(pos, state);
    }

    /** A fluid container fills the vat or takes its beer; Yeast goes in; anything else goes on to the empty hand use. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof FermentingVatBlockEntity vat)) {
            return InteractionResult.PASS;
        }
        if (Transfer.isFluidContainer(stack)) {
            if (!level.isClientSide()) {
                FluidUtil.interactWithFluidHandler(player, hand, pos, vat.fluids(), null);
            }
            return InteractionResult.SUCCESS;
        }
        if (!ServusBreweryRecipes.isYeast(level, stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            int added = vat.insertYeast(stack);
            if (added == 0) {
                return InteractionResult.FAIL;
            }
            stack.consume(added, player);
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Sneaking takes the Yeast back between batches; otherwise the player is told what the vat holds. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level instanceof ServerLevel) || !(level.getBlockEntity(pos) instanceof FermentingVatBlockEntity vat)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack taken = player.isShiftKeyDown() && player.getMainHandItem().isEmpty() ? vat.takeYeast() : ItemStack.EMPTY;
        if (taken.isEmpty()) {
            player.sendOverlayMessage(vat.status());
        } else {
            player.getInventory().placeItemBackInInventory(taken);
        }
        return InteractionResult.SUCCESS;
    }

    /** The scheduled check of a running batch. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof FermentingVatBlockEntity vat) {
            vat.check();
        }
    }

    /** Bubbles pop on the foam while the vat ferments. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(CONTENT) != VatContent.FERMENTING || random.nextFloat() > 0.3F) {
            return;
        }
        double x = pos.getX() + 0.15 + random.nextDouble() * 0.7;
        double z = pos.getZ() + 0.15 + random.nextDouble() * 0.7;
        level.addParticle(ParticleTypes.BUBBLE_POP, x, pos.getY() + SURFACE / 16.0 + 0.02, z, 0, 0.01, 0);
    }
}
