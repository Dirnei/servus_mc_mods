package io.github.dirnei.mc.servus.foundation.upgrade;

import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;

/**
 * Capacity Upgrade slots: only Capacity Upgrades fit, one per slot. The owner
 * reads {@link #count()} and decides from its own data how much room one adds.
 */
public class CapacityUpgradeSlots extends SingleItemUpgradeSlots {

    public CapacityUpgradeSlots(int slots, Runnable onChanged) {
        super(slots, onChanged, ServusFoundationItems.CAPACITY_UPGRADE);
    }
}
