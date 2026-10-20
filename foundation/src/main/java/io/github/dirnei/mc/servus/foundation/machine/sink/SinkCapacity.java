package io.github.dirnei.mc.servus.foundation.machine.sink;

/**
 * How much of a Sink's buffer is open: one row of slots and one tank, plus one
 * of each per Capacity Upgrade. Pure Java.
 *
 * @param slotsPerRow  item slots in one row
 * @param upgradeSlots the most Capacity Upgrades a Sink holds
 */
public record SinkCapacity(int slotsPerRow, int upgradeSlots) {

    /** Rows, and tanks, in the handlers: one per upgrade slot plus the base one. */
    public int maxRows() {
        return upgradeSlots + 1;
    }

    /** Item slots in the handler, open or not. */
    public int maxSlots() {
        return slotsPerRow * maxRows();
    }

    /** Open rows, and open tanks, with {@code upgrades} Capacity Upgrades. */
    public int openRows(int upgrades) {
        return Math.clamp(upgrades, 0, upgradeSlots) + 1;
    }

    public int openSlots(int upgrades) {
        return openRows(upgrades) * slotsPerRow;
    }

    public int openTanks(int upgrades) {
        return openRows(upgrades);
    }

    /** The row an item slot belongs to. */
    public int rowOf(int slot) {
        return slot / slotsPerRow;
    }

    /** Whether one Capacity Upgrade may come out: there is one, and the last open row and tank are empty. */
    public static boolean canRemoveUpgrade(int upgrades, boolean lastRowEmpty, boolean lastTankEmpty) {
        return upgrades > 0 && lastRowEmpty && lastTankEmpty;
    }
}
