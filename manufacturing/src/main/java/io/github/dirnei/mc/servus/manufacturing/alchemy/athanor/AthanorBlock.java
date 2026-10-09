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
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * The Athanor: buckets of principles fill it, other items are laid on it, sneaking with an empty hand takes the item
 * back, a plain click opens the GUI. {@link #TIER} drives the flame colour.
 */
public class AthanorBlock extends MachineBlock {

    public static final MapCodec<AthanorBlock> CODEC = simpleCodec(AthanorBlock::new);
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, 3);

    public AthanorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TIER);
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

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof ConfigurationCarrotItem || !(level.getBlockEntity(pos) instanceof AthanorBlockEntity athanor)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (!level.isClientSide()) {
            athanor.checkTier();
        }
        if (FluidUtil.getFluidContained(stack).isPresent()) {
            if (!level.isClientSide()) {
                FluidUtil.interactWithFluidHandler(player, hand, athanor.tanks());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT).isEmpty() && athanor.isItemValid(AthanorBlockEntity.SLOT_INPUT, stack)) {
            if (!level.isClientSide()) {
                athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, stack.split(stack.getCount()));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof AthanorBlockEntity athanor && !level.isClientSide()) {
            athanor.checkTier();
            ItemStack input = athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT);
            if (player.isShiftKeyDown() && !input.isEmpty()) {
                athanor.inventory().setStackInSlot(AthanorBlockEntity.SLOT_INPUT, ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(input);
                return InteractionResult.SUCCESS;
            }
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }
}
