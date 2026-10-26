package io.github.dirnei.mc.servus.shuttles.station;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * The last station number a warehouse gave, one counter per station kind.
 * Numbers start at 1 and are never given twice, also after a station is broken.
 */
public final class StationCounters {

    /** The kinds of station that count on their own. */
    public enum Kind { INBOUND, OUTBOUND, PACKING }

    private final Map<Kind, Integer> last = new EnumMap<>(Kind.class);

    public StationCounters() {
    }

    /** Counters that continue after the given last numbers; negative values count as 0. */
    public StationCounters(int inbound, int outbound, int packing) {
        last.put(Kind.INBOUND, Math.max(0, inbound));
        last.put(Kind.OUTBOUND, Math.max(0, outbound));
        last.put(Kind.PACKING, Math.max(0, packing));
    }

    /** The last number given for the kind, 0 when none was given. */
    public int last(Kind kind) {
        return last.getOrDefault(kind, 0);
    }

    /** Gives the next number for the kind. */
    public int take(Kind kind) {
        return last.merge(kind, 1, Integer::sum);
    }

    /** Continues from the other counters, as when a carried warehouse count is placed again. */
    public void copyFrom(StationCounters other) {
        last.clear();
        last.putAll(other.last);
    }

    /**
     * Whether a station needs a number from the current warehouse: it has none, or
     * the warehouse that gave it is another one.
     */
    public static boolean needsNumber(UUID numberedBy, int number, UUID current) {
        return number <= 0 || numberedBy == null || !Objects.equals(numberedBy, current);
    }

    /**
     * The counter that gave a saved number, read from the saved counter name. A
     * save without a known name was numbered by the Inbound counter for a
     * Packing Station and by its own counter for every other kind.
     */
    public static Kind counterOf(String saved, Kind kind) {
        for (Kind candidate : Kind.values()) {
            if (candidate.name().equals(saved)) {
                return candidate;
            }
        }
        return kind == Kind.PACKING ? Kind.INBOUND : kind;
    }
}
