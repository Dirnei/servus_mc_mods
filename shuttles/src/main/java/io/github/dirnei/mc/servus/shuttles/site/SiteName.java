package io.github.dirnei.mc.servus.shuttles.site;

import io.github.dirnei.mc.servus.foundation.naming.NameText;

/** The name of a site as typed by the player and as shown everywhere. */
public final class SiteName {

    public static final int MAX_LENGTH = NameText.MAX_LENGTH;

    private SiteName() {
    }

    /** What the server keeps of a typed name, cleaned like every name. */
    public static String clean(String typed) {
        return NameText.clean(typed);
    }

    /** The name to show: the player's name, or "Warehouse at x, z" for an unnamed site. */
    public static String display(String name, int x, int z) {
        String cleaned = clean(name);
        return cleaned.isEmpty() ? "Warehouse at " + x + ", " + z : cleaned;
    }
}
