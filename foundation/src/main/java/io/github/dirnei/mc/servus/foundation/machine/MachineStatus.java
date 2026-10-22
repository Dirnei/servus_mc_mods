package io.github.dirnei.mc.servus.foundation.machine;

/** Why a machine is dark, or that it runs, as its GUI status line shows it. Synced to the menu by ordinal. */
public enum MachineStatus {
    /** Nothing to do, for example a pump without a valid source. */
    IDLE,
    RUNNING,
    /** The buffer filled up and the machine waits until it drops below its restart threshold. */
    WAITING,
    /** A burner machine has work but no fuel. */
    OUT_OF_FUEL,
    /** A powered machine has work but not enough energy. */
    NO_POWER;

    /**
     * The status after a tick.
     *
     * @param working whether the machine worked this tick
     * @param waiting whether its restart threshold holds it off
     * @param hasWork whether it had work this tick
     * @param starved what the machine lacks when it has work but did not work
     */
    public static MachineStatus of(boolean working, boolean waiting, boolean hasWork, MachineStatus starved) {
        if (working) {
            return RUNNING;
        }
        if (waiting) {
            return WAITING;
        }
        return hasWork ? starved : IDLE;
    }

    /** The status for a synced ordinal; unknown values read as idle. */
    public static MachineStatus byOrdinal(int ordinal) {
        MachineStatus[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : IDLE;
    }
}
