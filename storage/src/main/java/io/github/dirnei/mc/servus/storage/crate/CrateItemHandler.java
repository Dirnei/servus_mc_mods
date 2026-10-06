package io.github.dirnei.mc.servus.storage.crate;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** One virtual slot per compartment, each holding that compartment's whole content. */
final class CrateItemHandler implements IItemHandler {

    private final CrateBlockEntity crate;

    CrateItemHandler(CrateBlockEntity crate) {
        this.crate = crate;
    }

    @Override
    public int getSlots() {
        return crate.compartments();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return crate.isEmpty(slot) ? ItemStack.EMPTY : crate.type(slot).copyWithCount(crate.count(slot));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int inserted = crate.insert(slot, stack, simulate);
        if (inserted <= 0) {
            return stack;
        }
        int left = stack.getCount() - inserted;
        return left <= 0 ? ItemStack.EMPTY : stack.copyWithCount(left);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return crate.extract(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return crate.capacityPerCompartment();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return crate.matches(slot, stack);
    }
}
