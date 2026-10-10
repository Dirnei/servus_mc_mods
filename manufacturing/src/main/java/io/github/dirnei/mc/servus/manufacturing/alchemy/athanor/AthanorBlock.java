package io.github.dirnei.mc.servus.manufacturing.alchemy.athanor;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.item.ConfigurationCarrotItem;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import io.github.dirnei.mc.servus.manufacturing.journal.ParacelsusJournalItem;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * The Athanor: items are laid on it, an empty hand takes the result, sneaking with an empty hand takes the item
 * back. It has no GUI. {@link #TIER} drives the flame colour.
 */
public class AthanorBlock extends MachineBlock {

    public static final MapCodec<AthanorBlock> CODEC = simpleCodec(AthanorBlock::new);
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, 3);
    /** Whether the Athanor stands formed on its hearth; only then it crafts. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public AthanorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TIER, FORMED);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> blockEntityType() {
        return ServusManufacturingBlockEntities.ATHANOR.get();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(state.getBlock()) && level.getBlockEntity(pos) instanceof AthanorBlockEntity athanor) {
            athanor.checkTier();
        }
    }

    /** The brick right under a formed Athanor is gone: unform at once; the other hearth bricks wait for the slow check. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (!level.isClientSide() && state.getValue(FORMED) && fromPos.equals(pos.below())
                && level.getBlockEntity(pos) instanceof AthanorBlockEntity athanor) {
            athanor.checkHearth();
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem || !(level.getBlockEntity(pos) instanceof AthanorBlockEntity athanor)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (stack.getItem() instanceof ParacelsusJournalItem) {
            if (!level.isClientSide()) {
                athanor.tryForm(player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!level.isClientSide()) {
            athanor.checkTier();
        }
        if (athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT).isEmpty() && athanor.isItemValid(AthanorBlockEntity.SLOT_INPUT, stack)) {
            if (!level.isClientSide()) {
                athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, stack.split(stack.getCount()));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** An empty hand takes the result; sneaking takes the item back. Never opens a menu. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof AthanorBlockEntity athanor && !level.isClientSide()) {
            athanor.checkTier();
            int slot = player.isShiftKeyDown() ? AthanorBlockEntity.SLOT_INPUT : AthanorBlockEntity.SLOT_OUTPUT;
            ItemStack taken = athanor.inventory().getStackInSlot(slot);
            if (!taken.isEmpty()) {
                athanor.inventory().setStackInSlot(slot, ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(taken);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
