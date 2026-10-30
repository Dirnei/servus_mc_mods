package io.github.dirnei.mc.servus.alchemy.venter;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A mixed store of rot: counts per kind in the order the kinds came in, up to a total capacity.
 * Batches take from the oldest matching kinds first. Pure Java.
 *
 * @param <K> the kind of rot
 */
public final class RotStore<K> {

    private final Map<K, Integer> counts = new LinkedHashMap<>();
    private int capacity;

    public RotStore(int capacity) {
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int total() {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int room() {
        return Math.max(0, capacity - total());
    }

    public boolean isEmpty() {
        return counts.isEmpty();
    }

    /** Adds up to {@code n} of {@code kind} as room allows and returns how many went in. */
    public int add(K kind, int n) {
        int accepted = Math.min(Math.max(0, n), room());
        if (accepted > 0) {
            counts.merge(kind, accepted, Integer::sum);
        }
        return accepted;
    }

    /** How many of the kinds {@code matches} accepts the store holds. */
    public int count(Predicate<K> matches) {
        return counts.entrySet().stream().filter(entry -> matches.test(entry.getKey())).mapToInt(Map.Entry::getValue).sum();
    }

    /** Takes {@code n} of the kinds {@code matches} accepts, oldest kinds first; takes nothing and returns false when fewer are held. */
    public boolean take(Predicate<K> matches, int n) {
        if (count(matches) < n) {
            return false;
        }
        int left = n;
        Iterator<Map.Entry<K, Integer>> entries = counts.entrySet().iterator();
        while (left > 0 && entries.hasNext()) {
            Map.Entry<K, Integer> entry = entries.next();
            if (!matches.test(entry.getKey())) {
                continue;
            }
            int taken = Math.min(left, entry.getValue());
            left -= taken;
            if (taken == entry.getValue()) {
                entries.remove();
            } else {
                entry.setValue(entry.getValue() - taken);
            }
        }
        return true;
    }

    /** The counts per kind in the order they came in; a read-only view. */
    public Map<K, Integer> contents() {
        return Collections.unmodifiableMap(counts);
    }

    /** Takes everything out and returns it, oldest kind first. */
    public Map<K, Integer> drain() {
        Map<K, Integer> all = new LinkedHashMap<>(counts);
        counts.clear();
        return all;
    }
}
