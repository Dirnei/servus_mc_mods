package io.github.dirnei.mc.servus.shuttles.rack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Picks the keys with the highest counts. Ties keep the map's iteration
 * order, so callers pass a map in slot order. Pure Java.
 */
public final class TopCounts {

    private TopCounts() {
    }

    /**
     * @param counts totals per key, in the order ties should be resolved
     * @param n      how many keys to return at most
     * @return the keys with the largest counts, largest first
     */
    public static <K> List<K> pick(Map<K, Integer> counts, int n) {
        List<K> result = new ArrayList<>();
        if (n <= 0) {
            return result;
        }
        List<Map.Entry<K, Integer>> entries = new ArrayList<>(counts.entrySet());
        // Stable sort: equal counts stay in insertion order.
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        for (Map.Entry<K, Integer> entry : entries) {
            if (result.size() >= n) {
                break;
            }
            if (entry.getValue() > 0) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
}
