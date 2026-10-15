package io.github.dirnei.mc.servus.storage.client;

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
}
