package io.github.dirnei.mc.servus.foundation.upgrade;

import io.github.dirnei.mc.servus.foundation.machine.MachineContent;

/** Anchor Upgrade slots of an Ender Anchor or a Warehouse Controller: only that item fits, one per slot. */
public class AnchorUpgradeSlots extends SingleItemUpgradeSlots {

    public AnchorUpgradeSlots(int slots, Runnable onChanged) {
        super(slots, onChanged, MachineContent.ANCHOR_UPGRADE);
    }
}
