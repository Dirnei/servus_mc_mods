package io.github.dirnei.mc.servus.foundation.machine;

import net.neoforged.neoforge.energy.EnergyStorage;

/** FE buffer of a machine with direct access for the owner. */
public class MachineEnergyStorage extends EnergyStorage {

    private final Runnable onChanged;

    public MachineEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChanged) {
        super(capacity, maxReceive, maxExtract);
        this.onChanged = onChanged;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int received = super.receiveEnergy(toReceive, simulate);
        if (received > 0 && !simulate) {
            onChanged.run();
        }
        return received;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int extracted = super.extractEnergy(toExtract, simulate);
        if (extracted > 0 && !simulate) {
            onChanged.run();
        }
        return extracted;
    }

    /** Takes energy out regardless of the extract limit. The owner spends its own energy this way. */
    public boolean consume(int amount) {
        if (energy < amount) {
            return false;
        }
        energy -= amount;
        onChanged.run();
        return true;
    }

    /** Adds energy regardless of the receive limit. Generators fill themselves this way. */
    public int add(int amount) {
        int added = Math.min(amount, capacity - energy);
        if (added > 0) {
            energy += added;
            onChanged.run();
        }
        return added;
    }

    public boolean isFull() {
        return energy >= capacity;
    }

    public void setEnergy(int value) {
        energy = Math.max(0, Math.min(value, capacity));
    }

    public void setCapacity(int value) {
        capacity = Math.max(0, value);
        energy = Math.min(energy, capacity);
    }
}
