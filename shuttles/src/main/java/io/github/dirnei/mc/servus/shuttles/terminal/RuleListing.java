package io.github.dirnei.mc.servus.shuttles.terminal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** The rows of the Storage Terminal's Rules tab: rules sorted by item name and filtered by the search. Pure, no Minecraft imports. */
public final class RuleListing {

    private RuleListing() {
    }

    /**
     * The indices of the shown rules in name order, case insensitive, ties
     * kept in list order; only names containing the query, ignoring case and
     * surrounding spaces, when it is not empty.
     */
    public static List<Integer> shown(List<String> names, String query) {
        String wanted = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Integer> shown = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            if (wanted.isEmpty() || names.get(i).toLowerCase(Locale.ROOT).contains(wanted)) {
                shown.add(i);
            }
        }
        shown.sort(Comparator.comparing((Integer i) -> names.get(i).toLowerCase(Locale.ROOT)));
        return shown;
    }
}
