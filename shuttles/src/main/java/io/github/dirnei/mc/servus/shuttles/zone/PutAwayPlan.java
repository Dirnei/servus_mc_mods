package io.github.dirnei.mc.servus.shuttles.zone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Decides which zoned blocks take an item: those listing the exact item
 * first, then those matching by tag. Everything else is general storage,
 * which the caller tries last. Within a tier the racks already holding the
 * item come first, then the other racks. Pure Java.
 */
public final class PutAwayPlan {

    private PutAwayPlan() {
    }

    /** How a filter relates to an item. */
    public enum Match { EXACT, TAG, NONE }

    /** Why nothing could be stored: no room anywhere, no inventory at all, or room only in racks no shuttle reaches. */
    public enum Reason { NO_ROOM, NO_STORAGE, NO_SHUTTLE }

    /**
     * A zone's filter as copied onto a block.
     *
     * @param items exact item keys the block takes
     * @param tags  item tag ids (as strings) the block takes
     */
    public record Filter<K>(Set<K> items, Set<String> tags) {
        public boolean isEmpty() {
            return items.isEmpty() && tags.isEmpty();
        }

        /** @param keyTags the tags of the item, looked up by the caller */
        public Match match(K key, Set<String> keyTags) {
            if (items.contains(key)) {
                return Match.EXACT;
            }
            if (!tags.isEmpty() && !Collections.disjoint(tags, keyTags)) {
                return Match.TAG;
            }
            return Match.NONE;
        }
    }

    /**
     * The filtered blocks that take an item, split by how they matched. Both
     * sets keep the iteration order of the input.
     */
    public record Tiers<S>(Set<S> exact, Set<S> byTag) {
        public boolean matched() {
            return !exact.isEmpty() || !byTag.isEmpty();
        }
    }

    /**
     * @param key      the item
     * @param filtered blocks with a non empty filter
     * @param tagsOf   tag ids of an item key, consulted at most once
     */
    public static <S, K> Tiers<S> tiers(K key, Map<S, Filter<K>> filtered, Function<K, Set<String>> tagsOf) {
        Set<S> exact = new LinkedHashSet<>();
        Set<S> byTag = new LinkedHashSet<>();
        Set<String> tags = null;
        for (Map.Entry<S, Filter<K>> entry : filtered.entrySet()) {
            Filter<K> filter = entry.getValue();
            if (filter.isEmpty()) {
                continue;
            }
            if (tags == null && !filter.tags().isEmpty()) {
                tags = tagsOf.apply(key);
            }
            switch (filter.match(key, tags == null ? Set.of() : tags)) {
                case EXACT -> exact.add(entry.getKey());
                case TAG -> byTag.add(entry.getKey());
                case NONE -> { }
            }
        }
        return new Tiers<>(Collections.unmodifiableSet(exact), Collections.unmodifiableSet(byTag));
    }

    /**
     * The target lists in order: exact matches, tag matches, then general
     * storage; each list holds the blocks already holding the item first (in
     * the order of {@code holders}), then the others in their own order.
     *
     * @param tiers   the zoned blocks that took the item
     * @param general the blocks without a filter
     * @param holders blocks holding the item, in index order
     */
    public static <S> List<List<S>> targets(Tiers<S> tiers, Collection<S> general, List<S> holders) {
        return List.of(order(tiers.exact(), holders), order(tiers.byTag(), holders), order(general, holders));
    }

    /** Candidates holding the item first, in the holders' order, then the rest in candidate order. */
    public static <S> List<S> order(Collection<S> candidates, List<S> holders) {
        List<S> result = new ArrayList<>();
        Set<S> holding = new HashSet<>();
        for (S holder : holders) {
            if (candidates.contains(holder) && holding.add(holder)) {
                result.add(holder);
            }
        }
        for (S candidate : candidates) {
            if (!holding.contains(candidate)) {
                result.add(candidate);
            }
        }
        return result;
    }

    /** The first block in tier order that {@code fits}, or empty when none does. */
    public static <S> Optional<S> first(List<List<S>> targets, Predicate<S> fits) {
        for (List<S> tier : targets) {
            for (S candidate : tier) {
                if (fits.test(candidate)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }
}
