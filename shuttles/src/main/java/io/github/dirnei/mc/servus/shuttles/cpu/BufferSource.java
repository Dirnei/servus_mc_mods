package io.github.dirnei.mc.servus.shuttles.cpu;

import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import net.minecraft.world.item.ItemStack;

/**
 * The source of a move out of the CPU's buffer: one slot with what one order
 * holds of one key, taken from the buffer as the move takes it. What the move
 * gives back returns to that order. Not exposed as a capability.
 */
final class BufferSource extends LegacyItemHandler {

    private final CpuBlockEntity master;
    private final int orderId;
    private final ItemKey key;
    private final boolean leaving;

    BufferSource(CpuBlockEntity master, int orderId, ItemKey key, boolean leaving) {
        this.master = master;
        this.orderId = orderId;
        this.key = key;
        this.leaving = leaving;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        int held = master.takeFromBuffer(orderId, key, Integer.MAX_VALUE, true);
        return held <= 0 ? ItemStack.EMPTY : key.stack(held);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !key.matches(stack)) {
            return stack;
        }
        if (!simulate) {
            master.returnToBuffer(orderId, key, stack.getCount(), leaving);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int taken = master.takeFromBuffer(orderId, key, amount, simulate);
        return taken <= 0 ? ItemStack.EMPTY : key.stack(taken);
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }
}
