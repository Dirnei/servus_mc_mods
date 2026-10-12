package io.github.dirnei.mc.servus.storage.cpu;

import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import net.minecraft.world.item.ItemStack;

/**
 * The packet inlet of a formed CPU block: takes only items its master's
 * buffer expects from open arrivals and gives nothing out. Item pipes and
 * hoppers never see it, since the CPU registers no item capability.
 */
final class CpuInlet extends LegacyItemHandler {

    private final CpuBlockEntity block;

    CpuInlet(CpuBlockEntity block) {
        this.block = block;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        CpuBlockEntity master = block.master();
        if (master == null) {
            return stack;
        }
        ItemKey key = ItemKey.of(stack);
        int fits = Math.min(stack.getCount(), master.buffer().expected(key));
        if (fits <= 0) {
            return stack;
        }
        if (!simulate) {
            fits = master.landInBuffer(key, fits);
        }
        return fits >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - fits);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }
}
