package io.github.dirnei.mc.servus.shuttles.field;

import java.util.ArrayList;
import java.util.List;

/**
 * What a harvester carries on a field run: a fixed number of stacks, each of
 * one item type up to its stack size. Additions fill partial stacks of the
 * same type first, then free slots. Pure Java.
 *
 * @param <K> the item type, compared by equality
 */
public final class FieldCargo<K> {

    /** A count of one item type with that type's stack size. */
    public record Entry<K>(K key, int count, int max) {
    }

    private static final class Slot<K> {
        final K key;
        final int max;
        int count;

        Slot(K key, int max, int count) {
            this.key = key;
            this.max = max;
            this.count = count;
        }
    }

    private final int slots;
    private final List<Slot<K>> held = new ArrayList<>();

    public FieldCargo(int slots) {
        this.slots = Math.max(1, slots);
    }

    public int slots() {
        return slots;
    }

    public boolean isEmpty() {
        return held.isEmpty();
    }

    /** Whether all of the entries fit at once. */
    public boolean fits(List<Entry<K>> adds) {
        FieldCargo<K> copy = copy();
        return copy.add(adds).isEmpty();
    }

    /** Adds the entries as far as they fit; returns what did not fit. */
    public List<Entry<K>> add(List<Entry<K>> adds) {
        List<Entry<K>> overflow = new ArrayList<>();
        for (Entry<K> entry : adds) {
            int left = entry.count();
            int max = Math.max(1, entry.max());
            for (Slot<K> slot : held) {
                if (left <= 0) {
                    break;
                }
                if (slot.key.equals(entry.key()) && slot.count < slot.max) {
                    int moved = Math.min(left, slot.max - slot.count);
                    slot.count += moved;
                    left -= moved;
                }
            }
            while (left > 0 && held.size() < slots) {
                int moved = Math.min(left, max);
                held.add(new Slot<>(entry.key(), max, moved));
                left -= moved;
            }
            if (left > 0) {
                overflow.add(new Entry<>(entry.key(), left, max));
            }
        }
        return overflow;
    }

    /** How many of the key are carried. */
    public int count(K key) {
        int count = 0;
        for (Slot<K> slot : held) {
            if (slot.key.equals(key)) {
                count += slot.count;
            }
        }
        return count;
    }

    /** How many more of the key would fit. */
    public int room(K key, int max) {
        int room = (slots - held.size()) * Math.max(1, max);
        for (Slot<K> slot : held) {
            if (slot.key.equals(key)) {
                room += slot.max - slot.count;
            }
        }
        return room;
    }

    /** Takes up to n of the key out, emptying the fullest partial stacks last; returns how many were taken. */
    public int take(K key, int n) {
        int taken = 0;
        for (int i = held.size() - 1; i >= 0 && taken < n; i--) {
            Slot<K> slot = held.get(i);
            if (!slot.key.equals(key)) {
                continue;
            }
            int moved = Math.min(n - taken, slot.count);
            slot.count -= moved;
            taken += moved;
            if (slot.count == 0) {
                held.remove(i);
            }
        }
        return taken;
    }

    /**
     * Takes everything out except up to {@code keepMax} of {@code keep} and
     * returns it, one entry per stack; a null key keeps nothing.
     */
    public List<Entry<K>> unload(K keep, int keepMax) {
        int kept = keep == null ? 0 : Math.min(count(keep), Math.max(0, keepMax));
        List<Entry<K>> out = new ArrayList<>();
        int keepLeft = kept;
        int keepStack = 1;
        for (Slot<K> slot : held) {
            int count = slot.count;
            if (keep != null && slot.key.equals(keep)) {
                keepStack = slot.max;
                int stays = Math.min(keepLeft, count);
                keepLeft -= stays;
                count -= stays;
            }
            if (count > 0) {
                out.add(new Entry<>(slot.key, count, slot.max));
            }
        }
        held.clear();
        if (kept > 0) {
            add(List.of(new Entry<>(keep, kept, keepStack)));
        }
        return out;
    }

    /** The carried stacks in slot order. */
    public List<Entry<K>> entries() {
        List<Entry<K>> list = new ArrayList<>();
        for (Slot<K> slot : held) {
            list.add(new Entry<>(slot.key, slot.count, slot.max));
        }
        return list;
    }

    private FieldCargo<K> copy() {
        FieldCargo<K> copy = new FieldCargo<>(slots);
        for (Slot<K> slot : held) {
            copy.held.add(new Slot<>(slot.key, slot.max, slot.count));
        }
        return copy;
    }
}
