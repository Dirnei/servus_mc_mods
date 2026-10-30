package io.github.dirnei.mc.servus.shuttles.client;

import java.util.Locale;

/** A stock count short enough for a narrow table cell: grouped up to 99,999, then k and M. Pure Java. */
final class CompactCount {

    private CompactCount() {
    }

    static String of(long count) {
        if (count < 100_000) {
            return String.format(Locale.ROOT, "%,d", count);
        }
        if (count < 1_000_000) {
            return count / 1_000 + "k";
        }
        if (count < 10_000_000) {
            return String.format(Locale.ROOT, "%.1fM", Math.floor(count / 100_000.0) / 10);
        }
        return count / 1_000_000 + "M";
    }

    /** A count short enough for an item slot: as is up to 999, then k and M with one decimal below 10 and none above. */
    static String slot(long count) {
        if (count < 1_000) {
            return Long.toString(count);
        }
        if (count < 1_000_000) {
            return scaled(count, 1_000, "k");
        }
        if (count < 1_000_000_000) {
            return scaled(count, 1_000_000, "M");
        }
        return scaled(count, 1_000_000_000, "G");
    }

    private static String scaled(long count, long unit, String suffix) {
        long tenths = count * 10 / unit;
        if (tenths < 100 && tenths % 10 != 0) {
            return tenths / 10 + "." + tenths % 10 + suffix;
        }
        return tenths / 10 + suffix;
    }
}
