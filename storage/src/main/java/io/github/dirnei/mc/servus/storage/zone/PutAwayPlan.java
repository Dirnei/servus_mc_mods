package io.github.dirnei.mc.servus.storage.zone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Decides where an item belongs: the filtered zone it matches, else general
 * storage, then the overflow zone. Pure, no Minecraft imports.
 *
 * @param <K> item key
 */
public final class PutAwayPlan {

    private PutAwayPlan() {
    }

    /**
     * One zone's rule.
     *
     * @param items    exact item keys the zone takes
     * @param tags     item tag ids (as strings) the zone takes
     * @param overflow whether this zone catches what fits nowhere else
     */
    public record Rule<K>(Set<K> items, Set<String> tags, boolean overflow) {
        public boolean filtered() {
            return !items.isEmpty() || !tags.isEmpty();
        }

        /** Whether a zone with this rule takes part in general storage. */
        public boolean general() {
            return !filtered() && !overflow;
        }
    }

    /** Where to try: a named zone or general storage. */
    public sealed interface Pool permits Zone, General {
    }

    public record Zone(String key) implements Pool {
    }

    public record General() implements Pool {
        public static final General INSTANCE = new General();
    }

    /** Why nothing could be stored, once every pool was tried. */
    public enum Reason { ZONE_FULL_NO_OVERFLOW, OVERFLOW_FULL, NO_STORAGE }

    /**
     * The pools to try in order and the reason to report when all of them fail.
     *
     * @param pools   pools in the order to try
     * @param blocked what to say when every pool has no room
     * @param matched the filtered zone the item matched, or null
     */
    public record Route(List<Pool> pools, Reason blocked, String matched) {
    }

    /**
     * @param key   the item
     * @param rules rules per zone key
     * @param tagsOf tag ids of an item key
     */
    public static <K> Route route(K key, Map<String, Rule<K>> rules, Function<K, Set<String>> tagsOf) {
        String best = null;
        boolean bestExact = false;
        Set<String> tags = null;
        String overflow = null;
        for (Map.Entry<String, Rule<K>> entry : rules.entrySet()) {
            Rule<K> rule = entry.getValue();
            if (rule.overflow()) {
                overflow = entry.getKey();
            }
            if (!rule.filtered()) {
                continue;
            }
            boolean exact = rule.items().contains(key);
            boolean byTag = false;
            if (!exact && !rule.tags().isEmpty()) {
                if (tags == null) {
                    tags = tagsOf.apply(key);
                }
                byTag = !Collections.disjoint(rule.tags(), tags);
            }
            if (!exact && !byTag) {
                continue;
            }
            if (best == null || (exact && !bestExact) || (exact == bestExact && entry.getKey().compareTo(best) < 0)) {
                best = entry.getKey();
                bestExact = exact;
            }
        }
        List<Pool> pools = new ArrayList<>(2);
        pools.add(best == null ? General.INSTANCE : new Zone(best));
        if (overflow != null && !overflow.equals(best)) {
            pools.add(new Zone(overflow));
        }
        Reason reason;
        if (overflow != null && !overflow.equals(best)) {
            reason = Reason.OVERFLOW_FULL;
        } else if (best != null) {
            reason = Reason.ZONE_FULL_NO_OVERFLOW;
        } else {
            reason = Reason.NO_STORAGE;
        }
        return new Route(List.copyOf(pools), reason, best);
    }
}
