package io.github.dirnei.mc.servus.storage.menu;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Steps shared by the menus' shift click handling. */
final class QuickMoves {

    private QuickMoves() {
    }

    /** Clears the slot when its stack was moved out completely, else marks it changed. */
    static void settle(Slot slot, ItemStack stack) {
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
    }
}
