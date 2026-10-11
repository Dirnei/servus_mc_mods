package io.github.dirnei.mc.servus.foundation.transfer;

import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * A fixed size item inventory on the NeoForge transfer API, with the slot
 * methods the Servus code used on the old item stack handler. It is a full
 * {@code ResourceHandler<ItemResource>}, so it can be exposed as a capability
 * and used in menus with {@code ResourceHandlerSlot}.
 *
 * <p>The {@code insertItem} and {@code extractItem} helpers open their own
 * transaction and must only be called outside of one (from block entity ticks
 * and menus), like the old handler methods.
 */
public class ItemSlots extends ItemStacksResourceHandler {

    public ItemSlots(int size) {
        super(size);
    }

    /** Called after a slot changed, like the old handler's hook. */
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
        return ItemUtil.insertItemReturnRemaining(this, slot, stack, simulate, null);
    }

    /** Takes up to {@code amount} items out of one slot. */
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack current = stacks.get(slot);
        if (current.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemResource resource = ItemResource.of(current);
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = extract(slot, resource, amount, tx);
            if (!simulate) {
                tx.commit();
            }
            return extracted <= 0 ? ItemStack.EMPTY : resource.toStack(extracted);
        }
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

    /** Empties every slot without calling the change hook. */
    public void clearSilently() {
        for (int slot = 0; slot < size(); slot++) {
            stacks.set(slot, ItemStack.EMPTY);
        }
    }
}
