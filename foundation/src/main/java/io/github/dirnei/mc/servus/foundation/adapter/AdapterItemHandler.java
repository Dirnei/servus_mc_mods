package io.github.dirnei.mc.servus.foundation.adapter;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The adapter's item handler on every face. Slot 0 takes items: expected
 * upkeep goes to the machine's upkeep side, everything else to its ingredient
 * side. The slots after it mirror the machine's output side for extraction.
 */
public final class AdapterItemHandler implements IItemHandler {

    private final MachineAdapterBlockEntity adapter;

    AdapterItemHandler(MachineAdapterBlockEntity adapter) {
        this.adapter = adapter;
    }

    private IItemHandler output() {
        return adapter.machineHandler(MachineProfile.Role.OUTPUT);
    }

    @Override
    public int getSlots() {
        IItemHandler output = output();
        return 1 + (output == null ? 0 : output.getSlots());
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot == 0) {
            IItemHandler ingredients = adapter.machineHandler(MachineProfile.Role.INGREDIENTS);
            return ingredients == null || ingredients.getSlots() == 0 ? ItemStack.EMPTY : ingredients.getStackInSlot(0);
        }
        IItemHandler output = output();
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
        IItemHandler ingredients = adapter.machineHandler(MachineProfile.Role.INGREDIENTS);
        return ingredients == null ? rest : ItemHandlerHelper.insertItemStacked(ingredients, rest, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot == 0) {
            return ItemStack.EMPTY;
        }
        IItemHandler output = output();
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
