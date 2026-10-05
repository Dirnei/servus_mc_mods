package io.github.dirnei.mc.servus.storage.crate;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** One virtual slot holding the crate's whole content. */
final class CrateItemHandler implements IItemHandler {

    private final CrateBlockEntity crate;

    CrateItemHandler(CrateBlockEntity crate) {
        this.crate = crate;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return crate.isEmpty() ? ItemStack.EMPTY : crate.type().copyWithCount(crate.count());
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int inserted = crate.insert(stack, simulate);
        if (inserted <= 0) {
            return stack;
        }
        int left = stack.getCount() - inserted;
        return left <= 0 ? ItemStack.EMPTY : stack.copyWithCount(left);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return crate.extract(amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return crate.capacity();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return crate.matches(stack);
    }
}
