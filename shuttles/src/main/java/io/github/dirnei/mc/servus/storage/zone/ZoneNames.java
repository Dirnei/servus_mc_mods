package io.github.dirnei.mc.servus.storage.zone;

import java.util.Locale;

/** Zone names as the player typed them versus the key they are matched by. Pure Java. */
public final class ZoneNames {

    public static final int MAX_LENGTH = 32;

    private ZoneNames() {
    }

    /** Trimmed, lower cased key; empty for a blank name. */
    public static String key(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_LENGTH) {
            trimmed = trimmed.substring(0, MAX_LENGTH);
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    /** The display form: trimmed and cut to the maximum length, case kept. */
    public static String display(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        return trimmed.length() > MAX_LENGTH ? trimmed.substring(0, MAX_LENGTH) : trimmed;
    }
}
