package io.github.dirnei.mc.servus.shuttles.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How many items of each key jobs have reserved, summed over many sources.
 * Each source reports its reserved amounts; replacing a report updates the
 * totals incrementally. Pure Java.
 *
 * @param <S> source identity
 * @param <K> item key
 */
public final class ReservationTally<S, K> {

    private final Map<S, Map<K, Integer>> bySource = new HashMap<>();
    private final Map<K, Long> totals = new HashMap<>();
    private long version;

    /**
     * Replaces what {@code source} reserves with {@code amounts}; counts of 0
     * or less are dropped. A report equal to the current one changes nothing,
     * not even the version.
     */
    public void set(S source, Map<K, Integer> amounts) {
        Map<K, Integer> copy = new HashMap<>();
        amounts.forEach((key, count) -> {
            if (count != null && count > 0) {
                copy.merge(key, count, Integer::sum);
            }
        });
        if (copy.equals(bySource.getOrDefault(source, Map.of()))) {
            return;
        }
        drop(source);
        if (!copy.isEmpty()) {
            copy.forEach((key, count) -> totals.merge(key, (long) count, Long::sum));
            bySource.put(source, copy);
        }
        version++;
    }

    /** Forgets a source and what it reserved. */
    public void remove(S source) {
        if (drop(source)) {
            version++;
        }
    }

    private boolean drop(S source) {
        Map<K, Integer> old = bySource.remove(source);
        if (old == null) {
            return false;
        }
        old.forEach((key, count) -> {
            if (totals.merge(key, (long) -count, Long::sum) <= 0) {
                totals.remove(key);
            }
        });
        return true;
    }

    /** Takes over every source of another tally, replacing reports of the same sources. */
    public void absorb(ReservationTally<S, K> other) {
        other.bySource.forEach(this::set);
    }

    /** Forgets everything. */
    public void clear() {
        if (!bySource.isEmpty()) {
            bySource.clear();
            totals.clear();
            version++;
        }
    }

    /** What one source reports, empty when it reserves nothing. */
    public Map<K, Integer> of(S source) {
        Map<K, Integer> map = bySource.get(source);
        return map == null ? Map.of() : Collections.unmodifiableMap(map);
    }

    /** The total reserved of the key over every source. */
    public long reserved(K key) {
        return totals.getOrDefault(key, 0L);
    }

    /** Every key with a reservation and its total. */
    public Map<K, Long> totals() {
        return Collections.unmodifiableMap(totals);
    }

    /** Moves whenever a total changes; equal versions mean equal totals. */
    public long version() {
        return version;
    }

    /**
     * The stock entries less what is reserved: entries with nothing left are
     * dropped, the rest sorted largest first. The same list when nothing is reserved.
     */
    public static <K> List<Map.Entry<K, Long>> requestable(List<Map.Entry<K, Long>> stock, Map<K, Long> reserved) {
        if (reserved.isEmpty()) {
            return stock;
        }
        List<Map.Entry<K, Long>> list = new ArrayList<>(stock.size());
        for (Map.Entry<K, Long> entry : stock) {
            long left = entry.getValue() - reserved.getOrDefault(entry.getKey(), 0L);
            if (left > 0) {
                list.add(left == entry.getValue() ? entry : Map.entry(entry.getKey(), left));
            }
        }
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return Collections.unmodifiableList(list);
    }
}
