package io.github.dirnei.mc.servus.foundation.machine;

import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/** FE buffer of a machine with direct access for the owner. */
public class MachineEnergyStorage extends SimpleEnergyHandler {

    private final Runnable onChanged;

    public MachineEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChanged) {
        super(capacity, maxReceive, maxExtract);
        this.onChanged = onChanged;
    }

    @Override
    protected void onEnergyChanged(int previousAmount) {
        onChanged.run();
    }

    /** Receives energy within the receive limit, outside of any transaction. */
    public int receiveEnergy(int toReceive, boolean simulate) {
        return Transfer.receiveEnergy(this, toReceive, simulate);
    }

    /** Extracts energy within the extract limit, outside of any transaction. */
    public int extractEnergy(int toExtract, boolean simulate) {
        return Transfer.extractEnergy(this, toExtract, simulate);
    }

    public int getEnergyStored() {
        return energy;
    }

    public int getMaxEnergyStored() {
        return capacity;
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
