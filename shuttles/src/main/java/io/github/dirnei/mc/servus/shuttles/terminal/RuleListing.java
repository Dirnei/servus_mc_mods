package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.foundation.search.SearchFacts;
import io.github.dirnei.mc.servus.foundation.search.SearchQuery;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** The rows of the Storage Terminal's Rules tab: rules sorted by item name and filtered by the search. Pure, no Minecraft imports. */
public final class RuleListing {

    private RuleListing() {
    }

    /**
     * The indices of the shown rules in name order, the first of each row's
     * {@link SearchFacts#names()}, case insensitive, ties kept in list order;
     * only rows matching the search syntax of {@link SearchQuery}.
     */
    public static List<Integer> shown(List<? extends SearchFacts> rows, String query) {
        SearchQuery search = SearchQuery.parse(query);
        List<Integer> shown = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            if (search.matches(rows.get(i))) {
                shown.add(i);
            }
        }
        shown.sort(Comparator.comparing((Integer i) -> sortName(rows.get(i))));
        return shown;
    }

    private static String sortName(SearchFacts row) {
        List<String> names = row.names();
        return names.isEmpty() ? "" : names.getFirst().toLowerCase(Locale.ROOT);
    }
}
