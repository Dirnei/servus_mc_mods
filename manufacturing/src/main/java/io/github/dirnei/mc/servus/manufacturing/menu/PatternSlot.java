package io.github.dirnei.mc.servus.manufacturing.menu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** A ghost slot: shows an item but never gives or takes real items. */
public class PatternSlot extends SlotItemHandler {

    public PatternSlot(IItemHandler pattern, int index, int x, int y) {
        super(pattern, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    /** Sets the ghost item to a single copy of the stack, or clears it. */
    public void setGhost(ItemStack stack) {
        set(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }
}
