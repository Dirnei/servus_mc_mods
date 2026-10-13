package io.github.dirnei.mc.servus.foundation.upgrade;

import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import net.minecraft.world.item.ItemStack;

/** Anchor Upgrade slots of an Ender Anchor or a Warehouse Controller: only that item fits, one per slot. */
public class AnchorUpgradeSlots extends UpgradeSlots {

    public AnchorUpgradeSlots(int slots, Runnable onChanged) {
        super(slots, onChanged);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isAnchorUpgrade(stack);
    }

    public static boolean isAnchorUpgrade(ItemStack stack) {
        return stack.is(MachineContent.ANCHOR_UPGRADE.get());
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
