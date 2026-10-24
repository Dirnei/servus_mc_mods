package io.github.dirnei.mc.servus.alchemy.journal;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinationDimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Lays out the journal's calcination table over book pages: rows grouped by dimension in enum order, each
 * group under its heading and sorted by recipe id. A heading never ends a page, and a group that goes on
 * on the next page repeats its heading there. Also derives the lang keys of a row. Pure Java.
 */
public final class CalcinationTable {

    /** Prefix of every table lang key. */
    public static final String KEY = "journal." + ServusAlchemyIds.MOD_ID + ".calcination.";
    private static final String RECIPE_FOLDER = "calcination/";

    /**
     * Pixel heights of the layout.
     *
     * @param first   room for lines on the first page, below its title and column header
     * @param rest    room for lines on every later page, below its column header
     * @param heading one group heading
     * @param row     one recipe row
     * @param footer  the left out note on the last page
     */
    public record Heights(int first, int rest, int heading, int row, int footer) {
    }

    /** The journal page: 155 px high, the title takes 19 px and the column header 11 px. */
    public static final Heights PAGE = new Heights(125, 144, 12, 20, 10);

    /** One line of a page. */
    public sealed interface Line<T> permits Heading, Entry {
    }

    /** The heading of a dimension's group. */
    public record Heading<T>(CalcinationDimension dimension) implements Line<T> {
    }

    /** One recipe row. */
    public record Entry<T>(T row) implements Line<T> {
    }

    /**
     * The laid out table.
     *
     * @param pages   the lines of each page
     * @param leftOut rows that did not fit within the page limit
     */
    public record Table<T>(List<List<Line<T>>> pages, int leftOut) {
    }

    private CalcinationTable() {
    }

    /**
     * Lays out {@code rows} on at most {@code maxPages} pages. When they do not fit, the last page keeps room
     * for the footer and the rows after it count as left out.
     */
    public static <T> Table<T> layout(List<T> rows, Function<T, String> id, Function<T, CalcinationDimension> dimension,
                                      Heights heights, int maxPages) {
        List<List<T>> groups = groups(rows, id, dimension);
        int limit = Math.max(1, maxPages);
        Table<T> table = flow(groups, dimension, heights, limit, false);
        return table.leftOut() == 0 ? table : flow(groups, dimension, heights, limit, true);
    }

    private static <T> List<List<T>> groups(List<T> rows, Function<T, String> id, Function<T, CalcinationDimension> dimension) {
        Map<CalcinationDimension, List<T>> byDimension = new EnumMap<>(CalcinationDimension.class);
        for (T row : rows) {
            byDimension.computeIfAbsent(dimension.apply(row), ignored -> new ArrayList<>()).add(row);
        }
        List<List<T>> groups = new ArrayList<>();
        for (List<T> group : byDimension.values()) {
            group.sort(Comparator.comparing(id));
            groups.add(group);
        }
        return groups;
    }

    private static <T> Table<T> flow(List<List<T>> groups, Function<T, CalcinationDimension> dimension, Heights heights,
                                     int limit, boolean reserveFooter) {
        List<List<Line<T>>> pages = new ArrayList<>();
        List<Line<T>> page = new ArrayList<>();
        pages.add(page);
        int used = 0;
        int leftOut = 0;
        for (List<T> group : groups) {
            boolean headed = false;
            for (T row : group) {
                if (pages.size() > limit) {
                    leftOut++;
                    continue;
                }
                int cost = heights.row() + (headed ? 0 : heights.heading());
                if (!page.isEmpty() && used + cost > room(heights, pages.size() - 1, limit, reserveFooter)) {
                    if (pages.size() == limit) {
                        pages.add(List.of());
                        leftOut++;
                        continue;
                    }
                    page = new ArrayList<>();
                    pages.add(page);
                    used = 0;
                    headed = false;
                    cost = heights.row() + heights.heading();
                }
                if (!headed) {
                    page.add(new Heading<>(dimension.apply(row)));
                    headed = true;
                }
                page.add(new Entry<>(row));
                used += cost;
            }
        }
        List<List<Line<T>>> kept = pages.subList(0, Math.min(pages.size(), limit));
        if (kept.size() == 1 && kept.getFirst().isEmpty()) {
            return new Table<>(List.of(), leftOut);
        }
        return new Table<>(kept.stream().map(List::copyOf).toList(), leftOut);
    }

    private static int room(Heights heights, int index, int limit, boolean reserveFooter) {
        int room = index == 0 ? heights.first() : heights.rest();
        return reserveFooter && index == limit - 1 ? room - heights.footer() : room;
    }

    /** The lang key of a recipe's hint, like {@code journal.servus_alchemy.calcination.servus_alchemy.blaze_rod.hint}. */
    public static String hintKey(String recipeId) {
        return KEY + recipeKey(recipeId) + ".hint";
    }

    /** The lang key of a recipe's name in the table. */
    public static String nameKey(String recipeId) {
        return KEY + recipeKey(recipeId) + ".name";
    }

    /** The lang key of a dimension's heading. */
    public static String headingKey(CalcinationDimension dimension) {
        return KEY + "dimension." + dimension.id();
    }

    private static String recipeKey(String recipeId) {
        int colon = recipeId.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : recipeId.substring(0, colon);
        String path = recipeId.substring(colon + 1);
        if (path.startsWith(RECIPE_FOLDER)) {
            path = path.substring(RECIPE_FOLDER.length());
        }
        return namespace + "." + path.replace('/', '.');
    }
}
