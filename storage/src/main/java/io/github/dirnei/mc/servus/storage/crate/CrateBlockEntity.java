package io.github.dirnei.mc.servus.storage.crate;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bulk storage in one, two or four compartments. Each compartment locks to
 * one item type while it holds items and takes the crate capacity divided by
 * the compartment count.
 */
public class CrateBlockEntity extends StorageNodeBlockEntity {

    private static final int SYNC_INTERVAL = 10;
    private static final long DOUBLE_CLICK_TICKS = 10;

    private final ItemStack[] types;
    private final int[] counts;
    private final CrateItemHandler handler = new CrateItemHandler(this);
    private final Map<UUID, long[]> lastRightClick = new HashMap<>();
    private boolean syncPending;
    private int syncTimer;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.CRATE.get(), pos, state);
        int compartments = state.getBlock() instanceof CrateBlock crate ? crate.compartments() : 1;
        types = new ItemStack[compartments];
        Arrays.fill(types, ItemStack.EMPTY);
        counts = new int[compartments];
    }

    // State

    @Override
    public boolean zonable() {
        return true;
    }

    public int compartments() {
        return types.length;
    }

    public int capacity() {
        return StorageStatsManager.INSTANCE.crateCapacity();
    }

    /** What one compartment holds at most. */
    public int capacityPerCompartment() {
        return Math.max(1, capacity() / compartments());
    }

    public boolean isEmpty(int compartment) {
        return counts[compartment] <= 0 || types[compartment].isEmpty();
    }

    public boolean isEmpty() {
        for (int i = 0; i < types.length; i++) {
            if (!isEmpty(i)) {
                return false;
            }
        }
        return true;
    }

    /** The stored item of a compartment as a one item template, empty when the compartment is empty. */
    public ItemStack type(int compartment) {
        return types[compartment];
    }

    /** The stored item of the first filled compartment, empty when the crate is empty. */
    public ItemStack type() {
        for (int i = 0; i < types.length; i++) {
            if (!isEmpty(i)) {
                return types[i];
            }
        }
        return ItemStack.EMPTY;
    }

    public int count(int compartment) {
        return counts[compartment];
    }

    /** Items over all compartments. */
    public int count() {
        int total = 0;
        for (int i = 0; i < types.length; i++) {
            total += isEmpty(i) ? 0 : counts[i];
        }
        return total;
    }

    public ResourceHandler<ItemResource> handler() {
        return handler;
    }

    public boolean matches(int compartment, ItemStack stack) {
        return !stack.isEmpty() && (isEmpty(compartment) || ItemStack.isSameItemSameComponents(types[compartment], stack));
    }

    /** Whether any compartment takes the stack: one already holding it, or an empty one. */
    public boolean matches(ItemStack stack) {
        return preferredCompartment(stack) >= 0;
    }

    /** The compartment an untargeted insert goes to: first holding the item, then first empty, or -1. */
    public int preferredCompartment(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < types.length; i++) {
            if (!isEmpty(i) && ItemStack.isSameItemSameComponents(types[i], stack) && counts[i] < capacityPerCompartment()) {
                return i;
            }
        }
        for (int i = 0; i < types.length; i++) {
            if (isEmpty(i)) {
                return i;
            }
        }
        return -1;
    }

    /** @return how many of the stack were (or would be) taken into the compartment */
    public int insert(int compartment, ItemStack stack, boolean simulate) {
        if (!matches(compartment, stack)) {
            return 0;
        }
        int accepted = Math.min(capacityPerCompartment() - counts[compartment], stack.getCount());
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            if (isEmpty(compartment)) {
                types[compartment] = stack.copyWithCount(1);
                counts[compartment] = 0;
            }
            counts[compartment] += accepted;
            changed();
        }
        return accepted;
    }

    /** Inserts into the preferred compartment. @return how many were (or would be) taken in */
    public int insert(ItemStack stack, boolean simulate) {
        int compartment = preferredCompartment(stack);
        return compartment < 0 ? 0 : insert(compartment, stack, simulate);
    }

    /** @return up to a stack of the compartment's item, empty when there is nothing */
    public ItemStack extract(int compartment, int amount, boolean simulate) {
        if (isEmpty(compartment) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack type = types[compartment];
        int taken = Math.min(Math.min(amount, counts[compartment]), type.getMaxStackSize());
        ItemStack result = type.copyWithCount(taken);
        if (!simulate) {
            counts[compartment] -= taken;
            if (counts[compartment] <= 0) {
                counts[compartment] = 0;
                types[compartment] = ItemStack.EMPTY;
            }
            changed();
        }
        return result;
    }

    /** Extracts from the first filled compartment. */
    public ItemStack extract(int amount, boolean simulate) {
        for (int i = 0; i < types.length; i++) {
            if (!isEmpty(i)) {
                return extract(i, amount, simulate);
            }
        }
        return ItemStack.EMPTY;
    }

    /** Empties every compartment without dropping anything. */
    public void clear() {
        Arrays.fill(types, ItemStack.EMPTY);
        Arrays.fill(counts, 0);
        changed();
    }

    /** Whether this right click on the compartment is the second one within the double click window. */
    public boolean registerRightClick(Player player, Level level, int compartment) {
        long now = level.getGameTime();
        long[] last = lastRightClick.put(player.getUUID(), new long[] {compartment, now});
        return last != null && last[0] == compartment && now - last[1] <= DOUBLE_CLICK_TICKS;
    }

    private void changed() {
        setChanged();
        syncPending = true;
        postDirty();
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = ValueIo.registries(this);
        ListTag list = new ListTag();
        for (int i = 0; i < types.length; i++) {
            CompoundTag entry = new CompoundTag();
            if (!isEmpty(i)) {
                entry.put("Type", ValueIo.saveStack(registries, types[i]));
                entry.putInt("Count", counts[i]);
            }
            list.add(entry);
        }
        tag.put("Compartments", list);
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        HolderLookup.Provider registries = input.lookup();
        Arrays.fill(types, ItemStack.EMPTY);
        Arrays.fill(counts, 0);
        ListTag list = tag.getListOrEmpty("Compartments");
        for (int i = 0; i < Math.min(list.size(), types.length); i++) {
            CompoundTag entry = list.getCompoundOrEmpty(i);
            ItemStack type = entry.contains("Type") ? ValueIo.parseStack(registries, entry.getCompoundOrEmpty("Type")) : ItemStack.EMPTY;
            int count = entry.getIntOr("Count", 0);
            if (!type.isEmpty() && count > 0) {
                types[i] = type;
                counts[i] = count;
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.merge(saveCustomOnly(registries));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Moved from the block's removal hook (26.1): runs on the server when the block is removed. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        Level level = this.level;
        if (level == null) {
            return;
        }
        CrateBlockEntity crate = this;
        for (int i = 0; i < crate.compartments(); i++) {
            while (!crate.isEmpty(i)) {
                Block.popResource(level, pos, crate.extract(i, crate.type(i).getMaxStackSize(), false));
            }
        }
    }
}
