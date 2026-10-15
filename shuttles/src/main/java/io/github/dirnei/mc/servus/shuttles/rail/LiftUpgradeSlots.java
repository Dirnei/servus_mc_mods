package io.github.dirnei.mc.servus.shuttles.rail;

import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import io.github.dirnei.mc.servus.foundation.upgrade.UpgradeSlots;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** The upgrade slots of an elevator shaft: only the Foundation Speed Upgrade fits, one per slot. */
public class LiftUpgradeSlots extends UpgradeSlots {

    public LiftUpgradeSlots(int slots, Runnable onChanged) {
        super(slots, onChanged);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.is(ServusFoundationItems.SPEED_UPGRADE.get());
    }

    /** Puts the stacks into the free slots; returns what does not fit. */
    public List<ItemStack> insertAll(List<ItemStack> stacks) {
        List<ItemStack> left = new ArrayList<>();
        for (ItemStack stack : stacks) {
            ItemStack rest = stack.copy();
            for (int slot = 0; slot < getSlots() && !rest.isEmpty(); slot++) {
                rest = insertItem(slot, rest, false);
            }
            if (!rest.isEmpty()) {
                left.add(rest);
            }
        }
        return left;
    }
}
