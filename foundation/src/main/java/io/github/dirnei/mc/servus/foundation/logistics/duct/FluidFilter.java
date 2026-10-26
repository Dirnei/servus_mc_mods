package io.github.dirnei.mc.servus.foundation.logistics.duct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The fluid whitelist of one duct side: {@link #SLOTS} slots, each empty or
 * naming a fluid by id. An empty list passes every fluid. Pure Java.
 */
public final class FluidFilter {

    public static final int SLOTS = 9;
    public static final int NONE = -1;

    private final String[] ids = new String[SLOTS];

    /** The fluid id of a slot, or null when the slot is empty or out of range. */
    public String get(int slot) {
        return inRange(slot) ? ids[slot] : null;
    }

    /** Sets a slot to a fluid id; null or blank clears it. Out of range slots are ignored. */
    public void set(int slot, String id) {
        if (inRange(slot)) {
            ids[slot] = id == null || id.isBlank() ? null : id;
        }
    }

    public void clear(int slot) {
        set(slot, null);
    }

    public void clearAll() {
        Arrays.fill(ids, null);
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
