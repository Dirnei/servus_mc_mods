package io.github.dirnei.mc.servus.storage.zone;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Decides which zoned blocks take an item: those listing the exact item
 * first, then those matching by tag. Everything else is general storage,
 * which the caller tries last. Pure, no Minecraft imports.
 */
public final class PutAwayPlan {

    private PutAwayPlan() {
    }

    /** How a filter relates to an item. */
    public enum Match { EXACT, TAG, NONE }

    /** Why nothing could be stored. */
    public enum Reason { NO_ROOM, NO_STORAGE }

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
}
