package io.github.dirnei.mc.servus.manufacturing.alchemy.purifier;

import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.redstone.Orientation;
import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.manufacturing.journal.ParacelsusJournalItem;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/** The Leaching Vat, the controller of the Purifier build: Ash and water in, Caput Mortuum out. Worked by hand, hopper and pipe; no GUI. */
public class PurifierBlock extends BaseEntityBlock {

    public static final MapCodec<PurifierBlock> CODEC = simpleCodec(PurifierBlock::new);

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** The way the formed build's front faces. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public PurifierBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, FACING);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        PurifierStructure.neighbourChanged(level, state, pos);
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PurifierBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ServusManufacturingBlockEntities.PURIFIER.get(), PurifierBlockEntity::serverTick);
    }

    /** Buckets fill water in or take Salt out; purifiable items go into the vat. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof PurifierBlockEntity purifier)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (stack.getItem() instanceof ParacelsusJournalItem) {
            if (!level.isClientSide()) {
                PurifierStructure.tryForm(level, pos, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (!state.getValue(FORMED)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (Transfer.isFluidContainer(stack)) {
            if (!level.isClientSide()) {
                FluidUtil.interactWithFluidHandler(player, hand, pos, purifier.fluidPort(), null);
            }
            return InteractionResult.SUCCESS;
        }
        if (purifier.purifiable(stack)) {
            if (!level.isClientSide()) {
                player.setItemInHand(hand, purifier.insert(stack));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Sneaking with an empty hand takes the Ash back; the residue is taken at the Residue Crock. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!state.getValue(FORMED) || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PurifierBlockEntity purifier) {
            ItemStack taken = purifier.takeItems();
            if (!taken.isEmpty() && !player.addItem(taken)) {
                player.drop(taken, false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof PurifierBlockEntity purifier) {
            purifier.setOwner(player.getUUID());
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        // The contents drop in PurifierBlockEntity#preRemoveSideEffects, while the block entity still exists.
        PurifierStructure.broken(level, state, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
