package io.github.dirnei.mc.servus.foundation.logistics.duct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The fluid whitelist of one duct side: {@link #SLOTS} slots, each empty or
 * naming a fluid by id, with an amount in mB (0 for none) that goes with the
 * slot. An empty list passes every fluid. Pure Java.
 */
public final class FluidFilter {

    public static final int SLOTS = 9;
    public static final int NONE = -1;

    private final String[] ids = new String[SLOTS];
    private final int[] amounts = new int[SLOTS];

    /** The fluid id of a slot, or null when the slot is empty or out of range. */
    public String get(int slot) {
        return inRange(slot) ? ids[slot] : null;
    }

    /**
     * Sets a slot to a fluid id; null or blank clears it. A slot that changes
     * its fluid loses its amount. Out of range slots are ignored.
     */
    public void set(int slot, String id) {
        if (!inRange(slot)) {
            return;
        }
        String next = id == null || id.isBlank() ? null : id;
        if (!Objects.equals(ids[slot], next)) {
            amounts[slot] = 0;
        }
        ids[slot] = next;
    }

    /** The amount of a slot in mB, 0 for none, an empty or an out of range slot. */
    public int amount(int slot) {
        return inRange(slot) && ids[slot] != null ? amounts[slot] : 0;
    }

    /** Sets the amount of a set slot; empty and out of range slots are ignored, negative amounts read as none. */
    public void setAmount(int slot, int amount) {
        if (inRange(slot) && ids[slot] != null) {
            amounts[slot] = Math.max(0, amount);
        }
    }

    /** The amount of the first slot naming the fluid, 0 when none names it. */
    public int amountFor(String id) {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (ids[slot] != null && ids[slot].equals(id)) {
                return amounts[slot];
            }
        }
        return 0;
    }

    /** How many set slots carry an amount. */
    public int amountEntries() {
        int count = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            if (amount(slot) > 0) {
                count++;
            }
        }
        return count;
    }

    public void clear(int slot) {
        set(slot, null);
    }

    public void clearAll() {
        Arrays.fill(ids, null);
        Arrays.fill(amounts, 0);
    }

    /** The first empty slot, or {@link #NONE} when all are set. */
    public int firstFree() {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (ids[slot] == null) {
                return slot;
            }
        }
        return NONE;
    }

    /** The set fluid ids in slot order. */
    public List<String> entries() {
        List<String> entries = new ArrayList<>();
        for (String id : ids) {
            if (id != null) {
                entries.add(id);
            }
        }
        return entries;
    }

    public boolean isEmpty() {
        return firstSet() == NONE;
    }

    /** Whether the fluid with that id passes: an empty list passes all, otherwise only the listed ones. */
    public boolean allows(String id) {
        if (isEmpty()) {
            return true;
        }
        for (String entry : ids) {
            if (entry != null && entry.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private int firstSet() {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (ids[slot] != null) {
                return slot;
            }
        }
        return NONE;
    }

    private static boolean inRange(int slot) {
        return slot >= 0 && slot < SLOTS;
    }
}
