package io.github.dirnei.mc.servus.foundation.machine.sink;

/**
 * Which slots and tanks of a Sink buffer hold something, counted per item row and in all, so the questions the menu
 * asks every tick need no scan. Pure Java. The owner reports every slot and tank that changed.
 */
public final class SinkFill {

    private final int slotsPerRow;
    private final boolean[] slots;
    private final int[] filledInRow;
    private final boolean[] tanks;
    private int filledSlots;
    private int filledTanks;

    public SinkFill(int slotCount, int slotsPerRow, int tankCount) {
        this.slotsPerRow = slotsPerRow;
        this.slots = new boolean[slotCount];
        this.filledInRow = new int[(slotCount + slotsPerRow - 1) / slotsPerRow];
        this.tanks = new boolean[tankCount];
    }

    /** Records whether {@code slot} holds something now. */
    public void slot(int slot, boolean filled) {
        if (slots[slot] == filled) {
            return;
        }
        slots[slot] = filled;
        int change = filled ? 1 : -1;
        filledInRow[slot / slotsPerRow] += change;
        filledSlots += change;
    }

    /** Records whether {@code tank} holds something now. */
    public void tank(int tank, boolean filled) {
        if (tanks[tank] == filled) {
            return;
        }
        tanks[tank] = filled;
        filledTanks += filled ? 1 : -1;
    }

    /** Whether any slot or tank holds something. */
    public boolean any() {
        return filledSlots > 0 || filledTanks > 0;
    }

    /** Whether every slot of item row {@code row} is empty. */
    public boolean rowEmpty(int row) {
        return filledInRow[row] == 0;
    }

    /** Whether {@code tank} is empty. */
    public boolean tankEmpty(int tank) {
        return !tanks[tank];
    }
}
