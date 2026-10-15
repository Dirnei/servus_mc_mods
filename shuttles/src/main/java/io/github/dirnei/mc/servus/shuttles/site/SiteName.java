package io.github.dirnei.mc.servus.shuttles.site;

/** The name of a site as typed by the player and as shown everywhere. */
public final class SiteName {

    public static final int MAX_LENGTH = 32;

    private SiteName() {
    }

    /** What the server keeps of a typed name: no control characters, trimmed, at most 32 characters. */
    public static String clean(String typed) {
        if (typed == null) {
            return "";
        }
        StringBuilder kept = new StringBuilder();
        typed.codePoints().filter(c -> !Character.isISOControl(c) && c != '§').forEach(kept::appendCodePoint);
        String trimmed = kept.toString().trim();
        if (trimmed.length() > MAX_LENGTH) {
            trimmed = trimmed.substring(0, MAX_LENGTH).trim();
        }
        return trimmed;
    }

    /** The name to show: the player's name, or "Warehouse at x, z" for an unnamed site. */
    public static String display(String name, int x, int z) {
        String cleaned = clean(name);
        return cleaned.isEmpty() ? "Warehouse at " + x + ", " + z : cleaned;
    }
}
