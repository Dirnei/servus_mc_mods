package io.github.dirnei.mc.servus.shuttles.network;

import java.util.HashMap;
import java.util.Map;

/**
 * The last machine number a warehouse gave, one counter per machine kind id.
 * Numbers start at 1 and are never given twice, also after a machine is broken.
 */
public final class MachineCounters {

    private final Map<String, Integer> last = new HashMap<>();

    /** The last number given for the kind, 0 when none was given. */
    public int last(String kind) {
        return last.getOrDefault(kind, 0);
    }

    /** Gives the next number for the kind. */
    public int take(String kind) {
        return last.merge(kind, 1, Integer::sum);
    }

    /** The counters as saved and carried: the last number per kind id. */
    public Map<String, Integer> toMap() {
        return Map.copyOf(last);
    }

    /** Continues from saved counters, as when a carried warehouse count is placed again; negative values count as 0. */
    public void load(Map<String, Integer> saved) {
        last.clear();
        saved.forEach((kind, number) -> {
            if (number > 0) {
                last.put(kind, number);
            }
        });
    }
}
