package io.github.dirnei.mc.servus.shuttles.pattern;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Counts what a crafting template consumes: one item per used cell, empty
 * cells given as {@code null}. Two templates are the same work when their
 * counts are equal, whatever the layout.
 */
public final class TemplateCounts {

    private TemplateCounts() {
    }

    /** How many of each key the template uses, in the order keys first appear. */
    public static <K> Map<K, Integer> counts(List<K> cells) {
        Map<K, Integer> counts = new LinkedHashMap<>();
        for (K cell : cells) {
            if (cell != null) {
                counts.merge(cell, 1, Integer::sum);
            }
        }
        return counts;
    }

    /** Whether both templates use the same keys in the same counts. */
    public static <K> boolean same(List<K> a, List<K> b) {
        return counts(a).equals(counts(b));
    }
}
