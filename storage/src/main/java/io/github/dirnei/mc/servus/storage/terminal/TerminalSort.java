package io.github.dirnei.mc.servus.storage.terminal;

import java.util.Comparator;
import java.util.Locale;

/**
 * Orders the stock entries of the Counter Terminal by the player's sort and
 * direction. Ties fall back to the name, then the mod. Pure, no Minecraft imports.
 */
public final class TerminalSort {

    /** What an entry is sorted by. Craft only entries have a count of 0. */
    public record Key(String name, long count, String mod) {
    }

    private TerminalSort() {
    }

    public static Comparator<Key> comparator(TerminalSettings.Sort sort, TerminalSettings.Direction direction) {
        Comparator<Key> byName = Comparator.comparing(key -> key.name().toLowerCase(Locale.ROOT));
        Comparator<Key> byMod = Comparator.comparing(key -> key.mod().toLowerCase(Locale.ROOT));
        Comparator<Key> primary = switch (sort) {
            case NAME -> byName;
            case AMOUNT -> Comparator.comparingLong(Key::count);
            case MOD -> byMod;
        };
        if (direction == TerminalSettings.Direction.DESCENDING) {
            primary = primary.reversed();
        }
        return primary.thenComparing(byName).thenComparing(byMod);
    }
}
