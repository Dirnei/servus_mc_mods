package io.github.dirnei.mc.servus.foundation.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * Slot style view of any item handler (usually one read from a capability)
 * with simulate flag methods. Every change opens its own
 * transaction, nested in the one open on this thread if there is one.
 *
 * @param handler the wrapped handler
 */
public record ItemView(ResourceHandler<ItemResource> handler) {

    /** A view of {@code handler}, or null when there is no handler. */
    public static @Nullable ItemView of(@Nullable ResourceHandler<ItemResource> handler) {
        return handler == null ? null : new ItemView(handler);
    }

    public int getSlots() {
        return handler.size();
    }

    /** A copy of the stack in the slot. */
    public ItemStack getStackInSlot(int slot) {
        return Transfer.stackInSlot(handler, slot);
    }

    public int getSlotLimit(int slot) {
        ItemResource resource = handler.getResource(slot);
        return handler.getCapacityAsInt(slot, resource);
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.isEmpty() || handler.isValid(slot, ItemResource.of(stack));
    }

    /** Inserts into one slot; returns what did not fit. */
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return Transfer.insertItem(handler, slot, stack, simulate);
    }

    /** Inserts slot by slot from the first; returns what did not fit. */
    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        return Transfer.insertItem(handler, stack, simulate);
    }

    /** Inserts into slots that already hold the item first, then into empty ones; returns what did not fit. */
    public ItemStack insertItemStacked(ItemStack stack, boolean simulate) {
        return Transfer.insertItemStacked(handler, stack, simulate);
    }

    /** Takes up to {@code amount} out of one slot. */
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return Transfer.extractItem(handler, slot, amount, simulate);
    }
}
