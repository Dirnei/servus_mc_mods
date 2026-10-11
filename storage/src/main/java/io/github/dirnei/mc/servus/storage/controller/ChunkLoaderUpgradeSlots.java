package io.github.dirnei.mc.servus.storage.controller;

import io.github.dirnei.mc.servus.foundation.upgrade.UpgradeSlots;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import net.minecraft.world.item.ItemStack;

/** The controller's Chunk Loader Upgrade slots: only that item fits, one per slot. */
public class ChunkLoaderUpgradeSlots extends UpgradeSlots {

    public ChunkLoaderUpgradeSlots(int slots, Runnable onChanged) {
        super(slots, onChanged);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.is(ServusStorageItems.CHUNK_LOADER_UPGRADE.get());
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
