package io.github.dirnei.mc.servus.delicacies.kitchen;

import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import java.util.function.BiPredicate;
import net.minecraft.world.item.ItemStack;

/**
 * The view automation gets of a kitchen block's slots: it inserts only where {@code insert} allows and takes out only
 * what {@code extract} allows. Both are asked with the slot and the stack.
 */
public final class KitchenSlots extends LegacyItemHandler {

    private final ItemSlots slots;
    private final BiPredicate<Integer, ItemStack> insert;
    private final BiPredicate<Integer, ItemStack> extract;

    public KitchenSlots(ItemSlots slots, BiPredicate<Integer, ItemStack> insert, BiPredicate<Integer, ItemStack> extract) {
        this.slots = slots;
        this.insert = insert;
        this.extract = extract;
    }

    @Override
    public int getSlots() {
        return slots.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slots.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !insert.test(slot, stack)) {
            return stack;
        }
        return slots.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack stack = slots.getStackInSlot(slot);
        if (stack.isEmpty() || !extract.test(slot, stack)) {
            return ItemStack.EMPTY;
        }
        return slots.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return slots.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return insert.test(slot, stack);
    }
}
