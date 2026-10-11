package io.github.dirnei.mc.servus.foundation.adapter;

import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import net.minecraft.world.item.ItemStack;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;

/**
 * The adapter's item handler on every face. Slot 0 takes items and always
 * reads empty: expected upkeep goes to the machine's upkeep side, everything
 * else to its ingredient side. The slots after it mirror the machine's output
 * side for extraction.
 */
public final class AdapterItemHandler extends LegacyItemHandler {

    private final MachineAdapterBlockEntity adapter;

    AdapterItemHandler(MachineAdapterBlockEntity adapter) {
        this.adapter = adapter;
    }

    private ItemView output() {
        return adapter.machineHandler(MachineProfile.Role.OUTPUT);
    }

    @Override
    public int getSlots() {
        ItemView output = output();
        return 1 + (output == null ? 0 : output.getSlots());
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot == 0) {
            // The input slot always reads empty, so stacking inserters (packets) offer it any item.
            return ItemStack.EMPTY;
        }
        ItemView output = output();
        return output == null || slot - 1 >= output.getSlots() ? ItemStack.EMPTY : output.getStackInSlot(slot - 1);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot != 0 || stack.isEmpty()) {
            return stack;
        }
        ItemStack rest = adapter.receiveUpkeep(stack, simulate);
        if (rest.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemView ingredients = adapter.machineHandler(MachineProfile.Role.INGREDIENTS);
        return ingredients == null ? rest : ingredients.insertItemStacked(rest, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot == 0) {
            return ItemStack.EMPTY;
        }
        ItemView output = output();
        return output == null || slot - 1 >= output.getSlots() ? ItemStack.EMPTY : output.extractItem(slot - 1, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0;
    }
}
