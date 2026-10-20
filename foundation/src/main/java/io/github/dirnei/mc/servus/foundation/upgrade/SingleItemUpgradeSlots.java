package io.github.dirnei.mc.servus.foundation.upgrade;

import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Upgrade slots that take one kind of item only, one per slot, and count how many they hold. */
public class SingleItemUpgradeSlots extends UpgradeSlots {

    private final Supplier<? extends Item> item;

    public SingleItemUpgradeSlots(int slots, Runnable onChanged, Supplier<? extends Item> item) {
        super(slots, onChanged);
        this.item = item;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.is(item.get());
    }

    /** How many upgrades the slots hold. */
    public int count() {
        int count = 0;
        for (int slot = 0; slot < getSlots(); slot++) {
            if (!getStackInSlot(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
