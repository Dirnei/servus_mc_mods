package io.github.dirnei.mc.servus.alchemy.structure;

import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

/** One slot of a build's inventory as a port: it either only takes what it accepts or only gives. */
public final class SlotPort extends LegacyItemHandler {

    private final ItemSlots inventory;
    private final int slot;
    /** What the port takes, or null for a port that only gives. */
    private final Predicate<ItemStack> accepts;

    private SlotPort(ItemSlots inventory, int slot, Predicate<ItemStack> accepts) {
        this.inventory = inventory;
        this.slot = slot;
        this.accepts = accepts;
    }

    /** A port that takes what {@code accepts} allows into {@code slot} and never gives. */
    public static SlotPort insertOnly(ItemSlots inventory, int slot, Predicate<ItemStack> accepts) {
        return new SlotPort(inventory, slot, accepts);
    }

    /** A port that gives from {@code slot} and takes nothing. */
    public static SlotPort extractOnly(ItemSlots inventory, int slot) {
        return new SlotPort(inventory, slot, null);
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return inventory.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
        return isItemValid(index, stack) ? inventory.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int index, int amount, boolean simulate) {
        return accepts == null ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int index) {
        return inventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int index, ItemStack stack) {
        return accepts != null && accepts.test(stack);
    }
}
