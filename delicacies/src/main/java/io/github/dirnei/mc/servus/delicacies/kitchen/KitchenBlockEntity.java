package io.github.dirnei.mc.servus.delicacies.kitchen;

import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A kitchen block that shows what it holds: its slots are saved, sent to clients after every change for the renderer,
 * and drop when the block goes.
 */
public abstract class KitchenBlockEntity extends BlockEntity {

    protected final ItemSlots slots;

    protected KitchenBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state);
        this.slots = new ItemSlots(size) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slotTakes(slot, stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                contentsChanged();
            }
        };
    }

    /** Whether {@code stack} may go into {@code slot}, by hand or by automation. */
    protected abstract boolean slotTakes(int slot, ItemStack stack);

    /** After a change of what the block holds: saves it and shows it to clients. */
    protected void contentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** The stack in {@code slot}, for renderers and tests. */
    public ItemStack stack(int slot) {
        return slots.getStackInSlot(slot);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            slots.dropAll(level, pos);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        slots.save(output, "Items");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        slots.load(input, "Items");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return ValueIo.write(this, registries, this::saveAdditional);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
