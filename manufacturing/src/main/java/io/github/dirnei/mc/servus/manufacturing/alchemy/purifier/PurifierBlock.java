package io.github.dirnei.mc.servus.manufacturing.alchemy.purifier;

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
import net.minecraft.world.ItemInteractionResult;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;

/** The Leaching Vat, the controller of the Purifier build: Ash and water in, Caput Mortuum out. Worked by hand, hopper and pipe; no GUI. */
public class PurifierBlock extends BaseEntityBlock {

    public static final MapCodec<PurifierBlock> CODEC = simpleCodec(PurifierBlock::new);

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public PurifierBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, AXIS);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof PurifierBlockEntity purifier)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof ParacelsusJournalItem) {
            if (!level.isClientSide()) {
                PurifierStructure.tryForm(level, pos, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!state.getValue(FORMED)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getCapability(Capabilities.FluidHandler.ITEM) != null) {
            if (!level.isClientSide()) {
                FluidUtil.interactWithFluidHandler(player, hand, purifier.fluidPort());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (purifier.purifiable(stack)) {
            if (!level.isClientSide()) {
                player.setItemInHand(hand, purifier.insert(stack));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** An empty hand takes the residue; sneaking with an empty hand takes the Ash back. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!state.getValue(FORMED)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PurifierBlockEntity purifier) {
            ItemStack taken = player.isShiftKeyDown() ? purifier.takeItems() : purifier.takeResidue();
            if (!taken.isEmpty() && !player.addItem(taken)) {
                player.drop(taken, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof PurifierBlockEntity purifier) {
            purifier.setOwner(player.getUUID());
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            PurifierStructure.broken(level, state, pos);
            if (level.getBlockEntity(pos) instanceof PurifierBlockEntity purifier) {
                purifier.dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
