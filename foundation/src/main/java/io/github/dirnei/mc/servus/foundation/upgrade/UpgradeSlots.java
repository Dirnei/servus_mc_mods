package io.github.dirnei.mc.servus.foundation.upgrade;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Upgrade slots of a block: only upgrades go in, one per slot. The block
 * multiplies its rate with {@link #multiplier()}; the callback given to the
 * constructor runs when an upgrade goes in or out.
 */
public class UpgradeSlots extends ItemStackHandler {

    private final Runnable onChanged;

    public UpgradeSlots(int slots, Runnable onChanged) {
        super(Math.max(0, slots));
        this.onChanged = onChanged;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return Upgrades.isUpgrade(stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    protected void onContentsChanged(int slot) {
        if (onChanged != null) {
            onChanged.run();
        }
    }

    /** The product of the multipliers of the upgrades held, 1 without upgrades. */
    public double multiplier() {
        List<Double> values = new ArrayList<>();
        for (int slot = 0; slot < getSlots(); slot++) {
            ItemStack stack = getStackInSlot(slot);
            if (!stack.isEmpty()) {
                values.add(Upgrades.multiplier(stack));
            }
        }
        return UpgradeMath.multiplier(values);
    }

    /** Takes every upgrade out, for dropping when the block breaks. */
    public List<ItemStack> takeAll() {
        List<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < getSlots(); slot++) {
            if (!getStackInSlot(slot).isEmpty()) {
                result.add(getStackInSlot(slot).copy());
                setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        return result;
    }
}
