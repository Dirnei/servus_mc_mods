package io.github.dirnei.mc.servus.foundation.production;

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

    /** How many of the upkeep item, or of another item doing its job such as another fuel, the block holds right now. */
    int upkeepCount();

    /**
     * A supplier will send {@code amount} upkeep items; the request ends at
     * game time {@code until} at the latest.
     */
    default void upkeepRequested(int amount, long until) {
    }
}
