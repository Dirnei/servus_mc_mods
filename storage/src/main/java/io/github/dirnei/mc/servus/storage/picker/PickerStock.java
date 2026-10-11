package io.github.dirnei.mc.servus.storage.picker;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Stock counts shown in an item picker: a change signature and a short count text. Pure logic. */
public final class PickerStock {

    private PickerStock() {
    }

    /**
     * A signature of the counts and whether there is a warehouse at all; equal
     * inputs give equal signatures whatever the map order.
     */
    public static long signature(boolean networked, Map<String, Long> counts) {
        long hash = networked ? 1 : 0;
        for (Map.Entry<String, Long> entry : new TreeMap<>(counts).entrySet()) {
            hash = hash * 1_000_003L + entry.getKey().hashCode();
            hash = hash * 1_000_003L + Long.hashCode(entry.getValue());
        }
        return hash;
    }

    /** The count as a short text: 999, 1.2k, 12k, 1.2M. */
    public static String shortCount(long count) {
        if (count < 1000) {
            return Long.toString(count);
        }
        if (count < 1_000_000) {
            return scaled(count, 1000, "k");
        }
        if (count < 1_000_000_000) {
            return scaled(count, 1_000_000, "M");
        }
        return scaled(count, 1_000_000_000, "G");
    }

    private static String scaled(long count, long unit, String suffix) {
        if (count < 10 * unit) {
            long tenths = count * 10 / unit;
            return tenths % 10 == 0 ? (tenths / 10) + suffix : String.format(Locale.ROOT, "%d.%d%s", tenths / 10, tenths % 10, suffix);
        }
        return (count / unit) + suffix;
    }
}
