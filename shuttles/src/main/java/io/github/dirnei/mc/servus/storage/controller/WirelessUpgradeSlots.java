package io.github.dirnei.mc.servus.storage.controller;

import io.github.dirnei.mc.servus.foundation.upgrade.UpgradeSlots;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import net.minecraft.world.item.ItemStack;

/** The controller's Wireless Upgrade slot: one slot, only that item, one item. */
public class WirelessUpgradeSlots extends UpgradeSlots {

    public WirelessUpgradeSlots(Runnable onChanged) {
        super(1, onChanged);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.is(ServusStorageItems.WIRELESS_UPGRADE.get());
    }

    /** Whether the slot holds a Wireless Upgrade. */
    public boolean held() {
        return !getStackInSlot(0).isEmpty();
    }
}
