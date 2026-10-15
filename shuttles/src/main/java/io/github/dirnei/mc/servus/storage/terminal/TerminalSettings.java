package io.github.dirnei.mc.servus.storage.terminal;

import java.util.Locale;
import java.util.Properties;

/**
 * The player's view settings of the Storage Terminal: sort, direction, view,
 * search mode and height. Stored as properties; unknown or missing values
 * fall back to the defaults. Pure, no Minecraft imports.
 */
public record TerminalSettings(Sort sort, Direction direction, View view, SearchMode search, Height height) {

    public enum Sort { NAME, AMOUNT, MOD }

    public enum Direction { ASCENDING, DESCENDING }

    public enum View { ALL, STOCK, CRAFTABLE }

    public enum SearchMode { NORMAL, JEI_SYNCED, JEI_TWO_WAY }

    /** Grid rows asked for; auto takes as many as fit the screen. */
    public enum Height {
        SMALL(4), MEDIUM(6), TALL(9), AUTO(Integer.MAX_VALUE);

        private final int rows;

        Height(int rows) {
            this.rows = rows;
        }

        public int rows() {
            return rows;
        }
    }

    public static final TerminalSettings DEFAULT =
            new TerminalSettings(Sort.AMOUNT, Direction.DESCENDING, View.ALL, SearchMode.NORMAL, Height.AUTO);

    public TerminalSettings withSort(Sort value) {
        return new TerminalSettings(value, direction, view, search, height);
    }

    public TerminalSettings withDirection(Direction value) {
        return new TerminalSettings(sort, value, view, search, height);
    }

    public TerminalSettings withView(View value) {
        return new TerminalSettings(sort, direction, value, search, height);
    }

    public TerminalSettings withSearch(SearchMode value) {
        return new TerminalSettings(sort, direction, view, value, height);
    }

    public TerminalSettings withHeight(Height value) {
        return new TerminalSettings(sort, direction, view, search, value);
    }

    /** The next value of an enum, wrapping around. */
    public static <E extends Enum<E>> E next(E value) {
        E[] values = value.getDeclaringClass().getEnumConstants();
        return values[(value.ordinal() + 1) % values.length];
    }

    public Properties toProperties() {
        Properties properties = new Properties();
        properties.setProperty("sort", sort.name());
        properties.setProperty("direction", direction.name());
        properties.setProperty("view", view.name());
        properties.setProperty("search", search.name());
        properties.setProperty("height", height.name());
        return properties;
    }

    public static TerminalSettings fromProperties(Properties properties) {
        return new TerminalSettings(
                read(properties, "sort", Sort.class, DEFAULT.sort),
                read(properties, "direction", Direction.class, DEFAULT.direction),
                read(properties, "view", View.class, DEFAULT.view),
                read(properties, "search", SearchMode.class, DEFAULT.search),
                read(properties, "height", Height.class, DEFAULT.height));
    }

    private static <E extends Enum<E>> E read(Properties properties, String key, Class<E> type, E fallback) {
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
