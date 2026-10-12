package io.github.dirnei.mc.servus.storage.controller;

import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * The Warehouse Controller's FE buffer: takes FE from every side through the
 * standard energy capability and never gives any out, so a Utility Duct power
 * line only ever treats the controller as a sink. The controller spends from
 * it directly.
 */
public class ControllerEnergy extends SimpleEnergyHandler {

    private final Runnable onChanged;

    public ControllerEnergy(int capacity, Runnable onChanged) {
        super(Math.max(0, capacity), Math.max(0, capacity), 0);
        this.onChanged = onChanged;
    }

    @Override
    protected void onEnergyChanged(int previousAmount) {
        if (onChanged != null) {
            onChanged.run();
        }
    }

    /** FE stored. */
    public int stored() {
        return energy;
    }

    /** FE the buffer holds at most. */
    public int capacity() {
        return capacity;
    }

    /** The owner spends regardless of the extract limit; never below zero. */
    public void spend(long amount) {
        if (amount <= 0 || energy <= 0) {
            return;
        }
        int before = energy;
        energy = (int) Math.max(0, energy - amount);
        onEnergyChanged(before);
    }

    /** Sets the FE stored, clamped to the capacity; for loading and GameTests. */
    public void setStored(int amount) {
        int before = energy;
        energy = Math.max(0, Math.min(capacity, amount));
        if (before != energy) {
            onEnergyChanged(before);
        }
    }

    /** Sets the FE stored without telling the owner, for loading. */
    void load(int amount) {
        energy = Math.max(0, Math.min(capacity, amount));
    }
}
