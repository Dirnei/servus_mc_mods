package io.github.dirnei.mc.servus.shuttles.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Picks the items an Any pattern crafts with: cells that accept the same
 * items form one group, and each group takes the accepted item the stock
 * holds most of, ties to the pattern's own item, with none in stock its own
 * items. Cells that accept only their own item stay as they are.
 *
 * @param <K> the item key, {@code null} for an empty cell
 */
public final class VariantChoice<K> {

    private final List<K> own;
    private final List<List<K>> groups;
    private final int[] groupOf;

    private VariantChoice(List<K> own, List<List<K>> groups, int[] groupOf) {
        this.own = Collections.unmodifiableList(new ArrayList<>(own));
        this.groups = groups;
        this.groupOf = groupOf;
    }

    /** A choice that always keeps the pattern's own items. */
    public static <K> VariantChoice<K> exact(List<K> own) {
        int[] groupOf = new int[own.size()];
        Arrays.fill(groupOf, -1);
        return new VariantChoice<>(own, List.of(), groupOf);
    }

    /**
     * A choice over the accepted items of each cell.
     *
     * @param own        the pattern's item per cell, {@code null} for empty cells
     * @param candidates the items each cell accepts, own item included; one or none keeps the cell fixed
     */
    public static <K> VariantChoice<K> of(List<K> own, List<? extends List<K>> candidates) {
        Map<Set<K>, Integer> indexBySet = new LinkedHashMap<>();
        List<List<K>> groups = new ArrayList<>();
        int[] groupOf = new int[own.size()];
        for (int cell = 0; cell < own.size(); cell++) {
            List<K> accepted = cell < candidates.size() && candidates.get(cell) != null ? candidates.get(cell) : List.of();
            if (own.get(cell) == null || accepted.size() < 2) {
                groupOf[cell] = -1;
                continue;
            }
            Set<K> set = new LinkedHashSet<>(accepted);
            Integer index = indexBySet.get(set);
            if (index == null) {
                index = groups.size();
                indexBySet.put(set, index);
                List<K> ordered = new ArrayList<>();
                ordered.add(own.get(cell));
                for (K key : accepted) {
                    if (!ordered.contains(key)) {
                        ordered.add(key);
                    }
                }
                groups.add(List.copyOf(ordered));
            }
            groupOf[cell] = index;
        }
        return new VariantChoice<>(own, List.copyOf(groups), groupOf);
    }

    /** Whether some cell may take another item than its own. */
    public boolean varies() {
        return !groups.isEmpty();
    }

    /** The accepted items of each group, the own item of its first cell first. */
    public List<List<K>> groups() {
        return groups;
    }

    /** The pattern's own items, per cell. */
    public List<K> own() {
        return own;
    }

    /** The items per cell to craft with from this stock. */
    public List<K> pick(Map<K, ? extends Number> stock) {
        List<K> chosen = new ArrayList<>(own);
        for (int group = 0; group < groups.size(); group++) {
            K best = mostStocked(groups.get(group), stock);
            if (best == null) {
                continue;
            }
            for (int cell = 0; cell < groupOf.length; cell++) {
                if (groupOf[cell] == group) {
                    chosen.set(cell, best);
                }
            }
        }
        return chosen;
    }

    private static <K> K mostStocked(List<K> accepted, Map<K, ? extends Number> stock) {
        K best = null;
        long bestCount = 0;
        for (K key : accepted) {
            Number held = stock.get(key);
            long count = held == null ? 0 : held.longValue();
            if (count > bestCount) {
                best = key;
                bestCount = count;
            }
        }
        return best;
    }
}
