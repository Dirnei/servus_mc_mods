package io.github.dirnei.mc.servus.storage.crate;

import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/** Bulk storage of one item type. */
public class CrateBlockEntity extends StorageNodeBlockEntity {

    private static final int SYNC_INTERVAL = 10;
    private static final long DOUBLE_CLICK_TICKS = 10;

    private ItemStack type = ItemStack.EMPTY;
    private int count;
    private final CrateItemHandler handler = new CrateItemHandler(this);
    private final Map<UUID, Long> lastRightClick = new HashMap<>();
    private boolean syncPending;
    private int syncTimer;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.CRATE.get(), pos, state);
    }

    // State

    public int capacity() {
        return CrateStatsManager.INSTANCE.crateCapacity();
    }

    public boolean isEmpty() {
        return count <= 0 || type.isEmpty();
    }

    /** The stored item as a one item template, empty when the crate is empty. */
    public ItemStack type() {
        return type;
    }

    public int count() {
        return count;
    }

    public IItemHandler handler() {
        return handler;
    }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && (isEmpty() || ItemStack.isSameItemSameComponents(type, stack));
    }

    /** @return how many of the stack were (or would be) taken in */
    public int insert(ItemStack stack, boolean simulate) {
        if (!matches(stack)) {
            return 0;
        }
        int accepted = Math.min(capacity() - count, stack.getCount());
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            if (isEmpty()) {
                type = stack.copyWithCount(1);
                count = 0;
            }
            count += accepted;
            changed();
        }
        return accepted;
    }

    /** @return up to a stack of the stored item, empty when there is nothing */
    public ItemStack extract(int amount, boolean simulate) {
        if (isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int taken = Math.min(Math.min(amount, count), type.getMaxStackSize());
        ItemStack result = type.copyWithCount(taken);
        if (!simulate) {
            count -= taken;
            if (count <= 0) {
                count = 0;
                type = ItemStack.EMPTY;
            }
            changed();
        }
        return result;
    }

    /** Whether this right click is the second one within the double click window. */
    public boolean registerRightClick(Player player, Level level) {
        long now = level.getGameTime();
        Long last = lastRightClick.put(player.getUUID(), now);
        return last != null && now - last <= DOUBLE_CLICK_TICKS;
    }

    private void changed() {
        setChanged();
        syncPending = true;
    }

    // Ticking: throttle client syncs

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrateBlockEntity crate) {
        if (!crate.syncPending) {
            return;
        }
        if (++crate.syncTimer < SYNC_INTERVAL) {
            return;
        }
        crate.syncTimer = 0;
        crate.syncPending = false;
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    // Persistence and sync

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!type.isEmpty()) {
            tag.put("Type", type.save(registries));
        }
        tag.putInt("Count", count);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        type = tag.contains("Type") ? ItemStack.parseOptional(registries, tag.getCompound("Type")) : ItemStack.EMPTY;
        count = tag.getInt("Count");
        if (type.isEmpty()) {
            count = 0;
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
