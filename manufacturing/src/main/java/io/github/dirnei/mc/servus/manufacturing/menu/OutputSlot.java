package io.github.dirnei.mc.servus.manufacturing.menu;

import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/** A machine output slot. Players can only take from it. */
public class OutputSlot extends SlotItemHandler {

    public OutputSlot(MachineBlockEntity machine, int slot, int x, int y) {
        super(machine.inventory(), slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }
}
