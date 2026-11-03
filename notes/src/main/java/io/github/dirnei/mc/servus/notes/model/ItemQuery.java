package io.github.dirnei.mc.servus.notes.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A search over item names: every plain word must be part of the name, every {@code @word} part of
 * the item's namespace or mod name, all ignoring case.
 */
public final class ItemQuery {

    private final List<String> words = new ArrayList<>();
    private final List<String> mods = new ArrayList<>();

    private ItemQuery(String text) {
        for (String word : text.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            if (word.startsWith("@")) {
                if (word.length() > 1) {
                    mods.add(word.substring(1));
                }
            } else if (!word.isEmpty()) {
                words.add(word);
            }
        }
    }

    public static ItemQuery of(String text) {
        return new ItemQuery(text);
    }

    /** Whether an item with this display name, from this namespace with this mod name, matches. */
    public boolean matches(String name, String namespace, String modName) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        String lowerMod = modName.toLowerCase(Locale.ROOT);
        for (String word : words) {
            if (!lowerName.contains(word)) {
                return false;
            }
        }
        for (String mod : mods) {
            if (!namespace.contains(mod) && !lowerMod.contains(mod)) {
                return false;
            }
        }
        return true;
    }
}
