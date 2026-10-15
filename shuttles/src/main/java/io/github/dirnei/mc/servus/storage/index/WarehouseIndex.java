package io.github.dirnei.mc.servus.storage.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aggregated stock over many sources. Each source contributes a snapshot
 * (key to count); replacing a snapshot updates totals incrementally. Pure Java.
 *
 * @param <S> source identity
 * @param <K> item key
 */
public final class WarehouseIndex<S, K> {

    private final Map<S, Map<K, Integer>> snapshots = new HashMap<>();
    private final Map<K, Long> totals = new HashMap<>();
    private final Map<K, LinkedHashSet<S>> holders = new HashMap<>();
    private long version;
    private long sortedVersion = -1;
    private List<Map.Entry<K, Long>> sorted = List.of();

    /**
     * Replaces the contribution of {@code source} with {@code snapshot}. A
     * snapshot equal to the current one changes nothing, not even the version.
     */
    public void update(S source, Map<K, Integer> snapshot) {
        Map<K, Integer> copy = new HashMap<>();
        snapshot.forEach((key, count) -> {
            if (count != null && count > 0) {
                copy.put(key, count);
            }
        });
        if (copy.equals(snapshots.getOrDefault(source, Map.of()))) {
            return;
        }
        remove(source);
        if (copy.isEmpty()) {
            return;
        }
        copy.forEach((key, count) -> {
            totals.merge(key, (long) count, Long::sum);
            holders.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(source);
        });
        snapshots.put(source, copy);
        version++;
    }

    /** Moves whenever the totals change; equal versions mean equal stock. */
    public long version() {
        return version;
    }

    /** Drops a source and everything it contributed. */
    public void remove(S source) {
        Map<K, Integer> old = snapshots.remove(source);
        if (old == null) {
            return;
        }
        version++;
        old.forEach((key, count) -> {
            long left = totals.merge(key, (long) -count, Long::sum);
            Set<S> set = holders.get(key);
            if (set != null) {
                set.remove(source);
            }
            if (left <= 0 || set == null || set.isEmpty()) {
                totals.remove(key);
                holders.remove(key);
            }
        });
    }

    public long count(K key) {
        return totals.getOrDefault(key, 0L);
    }

    /** All keys with their totals, largest first. The same read only list while the version holds. */
    public List<Map.Entry<K, Long>> entries() {
        if (sortedVersion != version) {
            List<Map.Entry<K, Long>> list = new ArrayList<>(totals.size());
            totals.forEach((key, total) -> list.add(Map.entry(key, total)));
            list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
            sorted = Collections.unmodifiableList(list);
            sortedVersion = version;
        }
        return sorted;
    }

    /** Sources that currently hold the key, in the order they reported it. */
    public List<S> sourcesHolding(K key) {
        Set<S> set = holders.get(key);
        return set == null ? Collections.emptyList() : new ArrayList<>(set);
    }

    /** What a source last reported, empty when unknown or empty. */
    public Map<K, Integer> snapshot(S source) {
        Map<K, Integer> map = snapshots.get(source);
        return map == null ? Collections.emptyMap() : Collections.unmodifiableMap(map);
    }

    public boolean isEmptySource(S source) {
        return !snapshots.containsKey(source);
    }

    public int typeCount() {
        return totals.size();
    }

    public long totalItems() {
        long sum = 0;
        for (long value : totals.values()) {
            sum += value;
        }
        return sum;
    }
}
