package io.github.dirnei.mc.servus.foundation.production;

import io.github.dirnei.mc.servus.foundation.data.UpkeepRequest;
import net.minecraft.world.item.ItemStack;

/**
 * A block that keeps a consumable, such as its fuel, stocked at a level.
 * Exposed as {@code servus_foundation:upkeep}; the data network asks a
 * supplier on the line for whatever is missing, independent of orders.
 */
public interface UpkeepHolder {

    /** The item to keep, empty for none. */
    ItemStack upkeepItem();

    /** How many to keep. */
    int upkeepLevel();

    /** How many must be missing before the network asks again; the network never waits for more than the level. */
    default int upkeepBatch() {
        return 1;
    }

    /** How many of the upkeep item the block holds right now. */
    int upkeepCount();

    /** The open upkeep request, null for none. */
    UpkeepRequest upkeepRequest();

    /** Keeps {@code request} as the open upkeep request, saved with the block; null clears it. */
    void upkeepRequest(UpkeepRequest request);

    /**
     * A supplier will send {@code amount} upkeep items; the request ends at
     * game time {@code until} at the latest.
     */
    default void upkeepRequested(int amount, long until) {
    }
}
