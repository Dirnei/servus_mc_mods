package io.github.dirnei.mc.servus.shuttles.shuttle.module.build;

/** The rule for a name plate: plain text, trimmed, at most {@link #MAX} characters. Pure Java. */
public final class NamePlate {

    /** The longest name a plate takes. */
    public static final int MAX = 20;
    private static final char FORMATTING = '§';

    private NamePlate() {
    }

    /** The name as it goes on a plate: formatting codes and control characters removed, trimmed, cut to {@link #MAX}. */
    public static String clean(String name) {
        if (name == null) {
            return "";
        }
        StringBuilder plain = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == FORMATTING) {
                i++;
            } else if (!Character.isISOControl(c)) {
                plain.append(c);
            }
        }
        String trimmed = plain.toString().strip();
        return trimmed.length() > MAX ? trimmed.substring(0, MAX).strip() : trimmed;
    }
}
