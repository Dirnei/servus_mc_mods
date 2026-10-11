package io.github.dirnei.mc.servus.storage.menu;

import java.util.function.Supplier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;

/**
 * A ghost slot that reads and writes through whichever handler the supplier
 * returns right now, so one row of slots can show the filter of the selected zone.
 */
public class GhostSlot extends Slot {

    private static final Container NONE = new SimpleContainer(0);

    private final Supplier<ItemSlots> handler;
    private final int index;

    public GhostSlot(Supplier<ItemSlots> handler, int index, int x, int y) {
        super(NONE, index, x, y);
        this.handler = handler;
        this.index = index;
    }

    @Override
    public ItemStack getItem() {
        ItemSlots current = handler.get();
        return current == null ? ItemStack.EMPTY : current.getStackInSlot(index);
    }

    @Override
    public void set(ItemStack stack) {
        ItemSlots current = handler.get();
        if (current != null) {
            current.setStackInSlot(index, stack);
        }
        setChanged();
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean hasItem() {
        return !getItem().isEmpty();
    }

    @Override
    public ItemStack remove(int amount) {
        return ItemStack.EMPTY;
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
