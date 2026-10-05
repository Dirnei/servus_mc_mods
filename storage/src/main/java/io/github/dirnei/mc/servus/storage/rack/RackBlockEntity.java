package io.github.dirnei.mc.servus.storage.rack;

import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Four visible compartments, one stack each. */
public class RackBlockEntity extends StorageNodeBlockEntity {

    private static final int SYNC_INTERVAL = 10;

    private final ItemStackHandler items = new ItemStackHandler(RackCompartments.COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            syncPending = true;
            postDirty();
        }
    };
    private boolean syncPending;
    private int syncTimer;

    public RackBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.RACK.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    public boolean isEmpty() {
        for (int i = 0; i < RackCompartments.COUNT; i++) {
            if (!items.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RackBlockEntity rack) {
        if (!rack.syncPending) {
            return;
        }
        if (++rack.syncTimer < SYNC_INTERVAL) {
            return;
        }
        rack.syncTimer = 0;
        rack.syncPending = false;
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            ItemStackHandler saved = new ItemStackHandler();
            saved.deserializeNBT(registries, tag.getCompound("Items"));
            for (int i = 0; i < RackCompartments.COUNT; i++) {
                items.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
