package io.github.dirnei.mc.servus.foundation.naming;

/** How a name typed by a player is kept: site names and machine names follow the same rules. */
public final class NameText {

    public static final int MAX_LENGTH = 32;

    private NameText() {
    }

    /** What the server keeps of a typed name: no control or formatting characters, trimmed, at most {@link #MAX_LENGTH} characters. */
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
}
