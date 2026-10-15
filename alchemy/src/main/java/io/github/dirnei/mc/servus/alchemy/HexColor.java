package io.github.dirnei.mc.servus.alchemy;

import java.util.OptionalInt;

/** Reads colours written as {@code #rrggbb} in data files. */
public final class HexColor {

    private static final int DIGITS = 6;

    private HexColor() {
    }

    /** The RGB of {@code #rrggbb}, the {@code #} optional; empty for anything else. */
    public static OptionalInt parse(String text) {
        String digits = text.startsWith("#") ? text.substring(1) : text;
        if (digits.length() != DIGITS) {
            return OptionalInt.empty();
        }
        for (int i = 0; i < DIGITS; i++) {
            if (Character.digit(digits.charAt(i), 16) < 0) {
                return OptionalInt.empty();
            }
        }
        return OptionalInt.of(Integer.parseInt(digits, 16));
    }

    /** {@code #rrggbb} for an RGB colour. */
    public static String format(int rgb) {
        return String.format("#%06x", rgb & 0xFFFFFF);
    }
}
