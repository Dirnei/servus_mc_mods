package io.github.dirnei.mc.servus.foundation.search;

import java.util.ArrayList;
import java.util.List;

/**
 * What a search can ask about one list entry, each as a list of strings in
 * any case. {@link SearchQuery} asks only for the facts its terms need, so an
 * implementation may build them lazily. Facts an entry does not have are an
 * empty list.
 */
public interface SearchFacts {

    /** The texts a plain word matches: the name, plus any extra text the list wants found. */
    List<String> names();

    /** Resource ids, for {@code &}. */
    default List<String> ids() {
        return List.of();
    }

    /** Mod ids and mod names, for {@code @}. */
    default List<String> mods() {
        return List.of();
    }

    /** Full tag ids such as {@code c:ingots}, for {@code $}. */
    default List<String> tags() {
        return List.of();
    }

    /** Tooltip lines below the name, for {@code #}. */
    default List<String> tooltip() {
        return List.of();
    }

    /** Names of the creative tabs holding the entry, for {@code %}. */
    default List<String> tabs() {
        return List.of();
    }

    /** These facts with {@code extra} added to {@link #names()}. */
    default SearchFacts withNames(List<String> extra) {
        return new Extended(this, extra, List.of());
    }

    /** These facts with {@code extra} added to {@link #tooltip()}. */
    default SearchFacts withTooltip(List<String> extra) {
        return new Extended(this, List.of(), extra);
    }

    /** Facts with only names and ids. */
    static SearchFacts of(List<String> names, List<String> ids) {
        return new SearchFacts() {
            @Override
            public List<String> names() {
                return names;
            }

            @Override
            public List<String> ids() {
                return ids;
            }
        };
    }

    /** A base with extra names and tooltip lines; every other fact is the base's. */
    record Extended(SearchFacts base, List<String> extraNames, List<String> extraTooltip) implements SearchFacts {

        @Override
        public List<String> names() {
            return joined(base.names(), extraNames);
        }

        @Override
        public List<String> ids() {
            return base.ids();
        }

        @Override
        public List<String> mods() {
            return base.mods();
        }

        @Override
        public List<String> tags() {
            return base.tags();
        }

        @Override
        public List<String> tooltip() {
            return joined(base.tooltip(), extraTooltip);
        }

        @Override
        public List<String> tabs() {
            return base.tabs();
        }

        private static List<String> joined(List<String> first, List<String> second) {
            if (second.isEmpty()) {
                return first;
            }
            List<String> all = new ArrayList<>(first);
            all.addAll(second);
            return all;
        }
    }
}
