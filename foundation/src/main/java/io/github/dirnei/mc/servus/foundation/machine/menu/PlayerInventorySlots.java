package io.github.dirnei.mc.servus.foundation.machine.menu;

import java.util.function.Consumer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** The 36 player inventory slots of a menu: three rows of main inventory, then the hotbar. */
public final class PlayerInventorySlots {

    /** Slot count added by {@link #add}. */
    public static final int COUNT = 36;
    /** Distance from the top of the main inventory to the hotbar. */
    public static final int HOTBAR_OFFSET = 58;

    private PlayerInventorySlots() {
    }

    /** Adds the slots with the main inventory's top left corner at {@code x}, {@code y}. */
    public static void add(Consumer<Slot> addSlot, Inventory playerInventory, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot.accept(new Slot(playerInventory, column + row * 9 + 9, x + column * 18, y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot.accept(new Slot(playerInventory, column, x + column * 18, y + HOTBAR_OFFSET));
        }
    }
}
