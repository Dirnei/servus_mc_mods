package io.github.dirnei.mc.servus.foundation.machine.menu;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/** A machine input or fuel slot. Accepts only what the machine accepts. */
public class MachineSlot extends SlotItemHandler {

    private final MachineBlockEntity machine;
    private final int slot;

    public MachineSlot(MachineBlockEntity machine, int slot, int x, int y) {
        super(machine.inventory(), slot, x, y);
        this.machine = machine;
        this.slot = slot;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return machine.isItemValid(slot, stack);
    }
}
