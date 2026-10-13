package io.github.dirnei.mc.servus.foundation.transfer;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/**
 * A fixed size item inventory on the NeoForge transfer API with slot style
 * simulate flag methods. It is a full
 * {@code ResourceHandler<ItemResource>}, so it can be exposed as a capability
 * and used in menus with {@code ResourceHandlerSlot}.
 *
 * <p>The {@code insertItem} and {@code extractItem} helpers open their own
 * transaction, nested in the one open on this thread if there is one.
 */
public class ItemSlots extends ItemStacksResourceHandler {

    public ItemSlots(int size) {
        super(size);
    }

    /** An empty inventory that takes its size from {@link #deserializeNBT}. */
    public ItemSlots() {
        this(0);
    }

    /** The slots as a tag, for code that still builds compound tags; {@link #save} is the value output form. */
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put(VALUE_IO_KEY, codec.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stacks).getOrThrow());
        return tag;
    }

    /** Replaces the slots (and the size) with a tag from {@link #serializeNBT}. */
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        Tag list = tag.get(VALUE_IO_KEY);
        if (list != null) {
            codec.parse(registries.createSerializationContext(NbtOps.INSTANCE), list).result().ifPresent(this::setStacks);
        }
    }

    /**
     * Fills these fixed size slots from a tag of {@link #serializeNBT}: extra saved
     * slots are dropped and missing ones are emptied.
     */
    public void fillFrom(HolderLookup.Provider registries, CompoundTag tag) {
        fillFrom(registries, tag, new ItemSlots());
    }

    /** As {@link #fillFrom(HolderLookup.Provider, CompoundTag)}, reading the tag through {@code reader} first. */
    public void fillFrom(HolderLookup.Provider registries, CompoundTag tag, ItemSlots reader) {
        reader.deserializeNBT(registries, tag);
        for (int slot = 0; slot < getSlots(); slot++) {
            setStackInSlot(slot, slot < reader.getSlots() ? reader.getStackInSlot(slot) : ItemStack.EMPTY);
        }
    }

    /** Pops every stack at {@code pos} and empties its slot. Server side. */
    public void dropAll(Level level, BlockPos pos) {
        for (int slot = 0; slot < getSlots(); slot++) {
            ItemStack stack = getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
                setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    /** Called after a slot changed. */
    protected void onContentsChanged(int slot) {
    }

    @Override
    protected final void onContentsChanged(int index, ItemStack previousContents) {
        onContentsChanged(index);
    }

    /** The most items one slot holds, before the item's own stack limit. */
    public int getSlotLimit(int slot) {
        return Item.ABSOLUTE_MAX_STACK_SIZE;
    }

    /** Whether the stack may go into the slot at all. */
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return resource.isEmpty() || isItemValid(index, resource.toStack());
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        int limit = getSlotLimit(index);
        return resource.isEmpty() ? limit : Math.min(limit, resource.getMaxStackSize());
    }

    public int getSlots() {
        return size();
    }

    /** The live stack in the slot; do not change it without calling {@link #setStackInSlot}. */
    public ItemStack getStackInSlot(int slot) {
        return stacks.get(slot);
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        ItemStack previous = stacks.set(slot, stack);
        onContentsChanged(slot, previous);
    }

    /** Inserts into one slot and returns what did not fit. */
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return Transfer.insertItem(this, slot, stack, simulate);
    }

    /** Inserts slot by slot from the first; returns what did not fit. */
    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        return Transfer.insertItem(this, stack, simulate);
    }

    /** Inserts into slots that already hold the item first, then into empty ones; returns what did not fit. */
    public ItemStack insertItemStacked(ItemStack stack, boolean simulate) {
        return Transfer.insertItemStacked(this, stack, simulate);
    }

    /** Takes up to {@code amount} items out of one slot. */
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return Transfer.extractItem(this, slot, amount, simulate);
    }

    /** Saves the slots under {@code key}. */
    public void save(ValueOutput output, String key) {
        serialize(output.child(key));
    }

    /**
     * Loads the slots saved under {@code key} into this fixed size inventory:
     * a saved size never resizes it, extra saved slots are dropped and missing
     * ones are emptied.
     */
    public void load(ValueInput input, String key) {
        Optional<NonNullList<ItemStack>> saved = input.child(key).flatMap(child -> child.read(VALUE_IO_KEY, codec));
        NonNullList<ItemStack> list = saved.orElse(NonNullList.create());
        for (int slot = 0; slot < size(); slot++) {
            stacks.set(slot, slot < list.size() ? list.get(slot) : ItemStack.EMPTY);
        }
    }
}
