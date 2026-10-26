package io.github.dirnei.mc.servus.shuttles.pattern;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The variants a material pattern offers: one item per group of cells. A
 * material group, whose item changes the result's material, takes each of its
 * candidates in turn; any other group takes the candidate the stock holds most
 * of, its own item when none is stocked. Variants come ordered by how much of
 * their material items the stock holds, the pattern's own variant first among
 * equals and always kept, at most a cap of them. Pure, no Minecraft imports.
 */
public final class VariantExpansion {

    /** The most variants one material pattern offers the planner. */
    public static final int MAX_VARIANTS = 32;

    private VariantExpansion() {
    }

    /**
     * @param groups   the candidates of each group, its own item first
     * @param material whether each group's item changes the result
     * @param stock    what the stock holds per item
     * @param cap      the most variants returned, at least 1
     * @return one item per group for each variant; a single empty variant for no groups
     */
    public static <K> List<List<K>> expand(List<List<K>> groups, List<Boolean> material, Map<K, ? extends Number> stock, int cap) {
        List<List<K>> choices = new ArrayList<>(groups.size());
        for (int group = 0; group < groups.size(); group++) {
            List<K> candidates = groups.get(group);
            choices.add(material.get(group) ? byStock(candidates, stock, cap) : List.of(mostStocked(candidates, stock)));
        }
        List<List<K>> variants = new ArrayList<>();
        variants.add(new ArrayList<>());
        for (List<K> options : choices) {
            List<List<K>> next = new ArrayList<>(variants.size() * options.size());
            for (List<K> variant : variants) {
                for (K option : options) {
                    List<K> longer = new ArrayList<>(variant);
                    longer.add(option);
                    next.add(longer);
                }
            }
            variants = next;
        }
        List<K> own = new ArrayList<>(groups.size());
        for (int group = 0; group < groups.size(); group++) {
            own.add(material.get(group) ? groups.get(group).get(0) : choices.get(group).get(0));
        }
        List<List<K>> sorted = new ArrayList<>(variants);
        sorted.sort(Comparator.comparingLong((List<K> variant) -> -score(variant, material, stock))
                .thenComparing(variant -> !variant.equals(own)));
        List<List<K>> kept = new ArrayList<>(sorted.subList(0, Math.min(Math.max(1, cap), sorted.size())));
        if (!kept.contains(own)) {
            kept.set(kept.size() - 1, own);
        }
        return kept;
    }

    /** The candidates the stock holds most of first, the rest in their order, at most {@code cap}. */
    private static <K> List<K> byStock(List<K> candidates, Map<K, ? extends Number> stock, int cap) {
        List<K> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingLong(key -> -count(stock, key)));
        List<K> kept = new ArrayList<>(sorted.subList(0, Math.min(Math.max(1, cap), sorted.size())));
        if (!kept.contains(candidates.get(0))) {
            kept.set(kept.size() - 1, candidates.get(0));
        }
        return kept;
    }

    private static <K> K mostStocked(List<K> candidates, Map<K, ? extends Number> stock) {
        K best = candidates.get(0);
        long bestCount = 0;
        for (K key : candidates) {
            long count = count(stock, key);
            if (count > bestCount) {
                best = key;
                bestCount = count;
            }
        }
        return best;
    }

    private static <K> long score(List<K> variant, List<Boolean> material, Map<K, ? extends Number> stock) {
        long score = 0;
        for (int group = 0; group < variant.size(); group++) {
            if (material.get(group)) {
                score += count(stock, variant.get(group));
            }
        }
        return score;
    }

    private static <K> long count(Map<K, ? extends Number> stock, K key) {
        Number held = stock.get(key);
        return held == null ? 0 : held.longValue();
    }
}
