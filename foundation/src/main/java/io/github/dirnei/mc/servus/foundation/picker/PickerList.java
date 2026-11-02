package io.github.dirnei.mc.servus.foundation.picker;

import io.github.dirnei.mc.servus.foundation.search.SearchFacts;
import io.github.dirnei.mc.servus.foundation.search.SearchQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The entries of an item picker: sorted by name with the "none" entry first,
 * narrowed by a search text and ranked by what can be picked. Pure logic.
 */
public final class PickerList {

    /** The id of the "none" entry, which always stays first. */
    public static final String NONE_ID = "";

    private static final Comparator<Item> ORDER = Comparator
            .comparing((Item item) -> !item.isNone())
            .thenComparing(item -> item.name().toLowerCase(Locale.ROOT))
            .thenComparing(Item::id);

    private PickerList() {
    }

    /** One entry: its id (for items the registry id) and its shown name. */
    public record Item(String id, String name) {

        public boolean isNone() {
            return NONE_ID.equals(id);
        }
    }

    /** The entries in name order, case insensitive, ties by id, the none entry first. */
    public static List<Item> sorted(Collection<Item> items) {
        List<Item> list = new ArrayList<>(items);
        list.sort(ORDER);
        return list;
    }

    /**
     * The entries in picking order: the none entry first, then the ones not
     * locked before the locked ones, each by stock, most first, then by name.
     * A null stock ranks by name only.
     */
    public static List<Item> ranked(List<Item> items, Set<String> locked, Map<String, Long> stock) {
        List<Item> list = new ArrayList<>(items);
        list.sort(Comparator
                .comparing((Item item) -> !item.isNone())
                .thenComparing(item -> locked.contains(item.id()))
                .thenComparing(item -> stock == null ? 0L : -stock.getOrDefault(item.id(), 0L))
                .thenComparing(ORDER));
        return list;
    }

    /**
     * The entries matching the search syntax of {@link SearchQuery}, in their
     * given order, each asked through its facts from {@code facts}, looked up
     * by entry id. A blank query keeps all; the none entry always stays.
     */
    public static List<Item> filter(List<Item> items, String query, Function<String, SearchFacts> facts) {
        SearchQuery search = SearchQuery.parse(query);
        if (search.isEmpty()) {
            return List.copyOf(items);
        }
        List<Item> found = new ArrayList<>();
        for (Item item : items) {
            if (item.isNone() || search.matches(facts.apply(item.id()))) {
                found.add(item);
            }
        }
        return found;
    }

    /** Facts for an entry known only by its name and id; a plain word matches either. */
    public static SearchFacts plainFacts(Item item) {
        return SearchFacts.of(List.of(item.name(), item.id()), List.of(item.id()));
    }
}
