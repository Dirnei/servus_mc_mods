package io.github.dirnei.mc.servus.foundation.picker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Matches and orders tag ids such as {@code c:ingots} for a tag search typed
 * as {@code #text}. Pure logic.
 */
public final class TagSearch {

    /** The character that starts a tag search and prefixes a tag entry's id. */
    public static final String PREFIX = "#";

    private TagSearch() {
    }

    /** Whether the search text asks for tags. */
    public static boolean isTagQuery(String text) {
        return text != null && text.startsWith(PREFIX);
    }

    /**
     * The tag ids matching the search, best first: ids whose path starts with
     * the search, then ids containing it anywhere, each group in id order. The
     * search may start with {@link #PREFIX}; an empty search lists every id.
     * Case is ignored.
     */
    public static List<String> search(Collection<String> ids, String query) {
        String needle = needle(query);
        List<String> prefix = new ArrayList<>();
        List<String> contains = new ArrayList<>();
        for (String id : ids) {
            String lower = id.toLowerCase(Locale.ROOT);
            if (path(lower).startsWith(needle)) {
                prefix.add(id);
            } else if (lower.contains(needle)) {
                contains.add(id);
            }
        }
        prefix.sort(null);
        contains.sort(null);
        prefix.addAll(contains);
        return prefix;
    }

    /** The id Tab completes to: the first of {@link #search}, if any. */
    public static Optional<String> topHit(Collection<String> ids, String query) {
        List<String> found = search(ids, query);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.getFirst());
    }

    private static String needle(String query) {
        String text = query == null ? "" : query.trim();
        if (isTagQuery(text)) {
            text = text.substring(PREFIX.length());
        }
        return text.toLowerCase(Locale.ROOT);
    }

    private static String path(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }
}
