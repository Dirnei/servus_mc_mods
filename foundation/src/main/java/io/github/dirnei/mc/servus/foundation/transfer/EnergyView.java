package io.github.dirnei.mc.servus.foundation.transfer;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

/**
 * View of any energy handler (usually one read from a capability) with
 * simulate flag methods. Every change opens its own
 * transaction, nested in the one open on this thread if there is one.
 *
 * @param handler the wrapped handler
 */
public record EnergyView(EnergyHandler handler) {

    /** A view of {@code handler}, or null when there is no handler. */
    public static @Nullable EnergyView of(@Nullable EnergyHandler handler) {
        return handler == null ? null : new EnergyView(handler);
    }

    public int getEnergyStored() {
        return handler.getAmountAsInt();
    }

    public int getMaxEnergyStored() {
        return handler.getCapacityAsInt();
    }

    /**
     * Whether energy can come out right now: simulates taking one unit, as
     * the transfer API has no fixed "can extract" flag.
     */
    public boolean canExtract() {
        return extractEnergy(1, true) > 0;
    }

    /** Whether energy can go in right now: simulates giving one unit. */
    public boolean canReceive() {
        return receiveEnergy(1, true) > 0;
    }

    public int receiveEnergy(int amount, boolean simulate) {
        return Transfer.receiveEnergy(handler, amount, simulate);
    }

    public int extractEnergy(int amount, boolean simulate) {
        return Transfer.extractEnergy(handler, amount, simulate);
    }
}
