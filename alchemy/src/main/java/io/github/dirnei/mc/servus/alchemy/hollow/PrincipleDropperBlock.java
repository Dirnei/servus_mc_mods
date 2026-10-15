package io.github.dirnei.mc.servus.alchemy.hollow;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.alchemy.PrincipleBuckets;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/**
 * The Principle Dropper block: a copper drip vessel with a glass bulb. Set directly on a Gnome Hollow it drips the
 * principles the vein still drinks; pipes and principle buckets fill it.
 */
public class PrincipleDropperBlock extends BaseEntityBlock {

    public static final MapCodec<PrincipleDropperBlock> CODEC = simpleCodec(PrincipleDropperBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(3, 0, 3, 4.5, 4, 4.5), Block.box(11.5, 0, 3, 13, 4, 4.5),
            Block.box(3, 0, 11.5, 4.5, 4, 13), Block.box(11.5, 0, 11.5, 13, 4, 13),
            Block.box(7, 0.25, 7, 9, 4, 9),
            Block.box(3, 4, 3, 13, 5, 13), Block.box(4, 5, 4, 12, 13, 12),
            Block.box(3.5, 13, 3.5, 12.5, 14, 12.5), Block.box(6, 14, 6, 10, 16, 10));

    public PrincipleDropperBlock(Properties properties) {
        super(properties);
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrincipleDropperBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ServusAlchemyBlockEntities.PRINCIPLE_DROPPER.get(), PrincipleDropperBlockEntity::serverTick);
    }

    /** A principle bucket pours in; any other item does nothing here. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!PrincipleBuckets.holdsPrinciple(stack) || !(level.getBlockEntity(pos) instanceof PrincipleDropperBlockEntity dropper)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            FluidUtil.interactWithFluidHandler(player, hand, pos, dropper.tanks(), null);
        }
        return InteractionResult.SUCCESS;
    }
}
