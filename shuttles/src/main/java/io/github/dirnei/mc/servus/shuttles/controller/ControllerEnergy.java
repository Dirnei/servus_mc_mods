package io.github.dirnei.mc.servus.shuttles.controller;

import io.github.dirnei.mc.servus.shuttles.power.PowerBudget;
import java.util.Locale;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * The Warehouse Controller's FE buffer: takes FE from every side through the
 * standard energy capability and never gives any out, so a Utility Duct power
 * line only ever treats the controller as a sink. Every consumer of the
 * warehouse draws from it through {@link #draw}: paid in full or not at all,
 * with the no power state and the use readout kept by a {@link PowerBudget}.
 */
public class ControllerEnergy extends SimpleEnergyHandler {

    private final Runnable onChanged;
    private final PowerBudget budget = new PowerBudget();

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

    /** An FE amount with thousands separators, as the GUI and the item tooltip show it. */
    public static String format(long fe) {
        return String.format(Locale.ROOT, "%,d", fe);
    }

    /** FE stored. */
    public int stored() {
        return energy;
    }

    /** FE the buffer holds at most. */
    public int capacity() {
        return capacity;
    }

    /**
     * A consumer's draw at that tick: paid in full and taken from the buffer,
     * or refused and nothing taken. Counts into the use readout or the no
     * power state.
     *
     * @return whether the draw was paid
     */
    public boolean draw(long tick, long amount) {
        if (amount <= 0) {
            return true;
        }
        if (!budget.draw(tick, energy, amount)) {
            return false;
        }
        int before = energy;
        energy = (int) Math.max(0, energy - amount);
        onEnergyChanged(before);
        return true;
    }

    /** Spends up to the amount regardless of the extract limit, never below zero, and counts it as used. */
    public void spend(long tick, long amount) {
        if (amount <= 0 || energy <= 0) {
            return;
        }
        int before = energy;
        long taken = Math.min(energy, amount);
        energy = (int) (energy - taken);
        budget.use(tick, taken);
        onEnergyChanged(before);
    }

    /** Records a draw the consumer found it could not pay without asking for it. */
    public void refused(long tick) {
        budget.observe(tick, false);
    }

    /** Whether the warehouse is in the no power state at that tick. */
    public boolean noPower(long tick) {
        return budget.state(tick, energy) == PowerBudget.State.NO_POWER;
    }

    /** FE drawn during the last second. */
    public long usedLastSecond(long tick) {
        return budget.usedLastSecond(tick);
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
