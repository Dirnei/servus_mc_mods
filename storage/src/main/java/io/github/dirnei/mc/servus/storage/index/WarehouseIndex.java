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
 * (key to count); replacing a snapshot updates totals incrementally. Pure
 * Java, no Minecraft imports.
 *
 * @param <S> source identity
 * @param <K> item key
 */
public final class WarehouseIndex<S, K> {

    private final Map<S, Map<K, Integer>> snapshots = new HashMap<>();
    private final Map<K, Long> totals = new HashMap<>();
    private final Map<K, LinkedHashSet<S>> holders = new HashMap<>();

    /** Replaces the contribution of {@code source} with {@code snapshot}. */
    public void update(S source, Map<K, Integer> snapshot) {
        remove(source);
        if (snapshot.isEmpty()) {
            return;
        }
        Map<K, Integer> copy = new HashMap<>();
        snapshot.forEach((key, count) -> {
            if (count == null || count <= 0) {
                return;
            }
            copy.put(key, count);
            totals.merge(key, (long) count, Long::sum);
            holders.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(source);
        });
        if (!copy.isEmpty()) {
            snapshots.put(source, copy);
        }
    }

    /** Drops a source and everything it contributed. */
    public void remove(S source) {
        Map<K, Integer> old = snapshots.remove(source);
        if (old == null) {
            return;
        }
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

    /** All keys with their totals, largest first. */
    public List<Map.Entry<K, Long>> entries() {
        List<Map.Entry<K, Long>> list = new ArrayList<>(totals.entrySet());
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return list;
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
