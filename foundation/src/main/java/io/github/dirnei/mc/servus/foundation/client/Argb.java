package io.github.dirnei.mc.servus.foundation.client;

/** ARGB colour helpers. */
final class Argb {

    private Argb() {
    }

    /** The colour itself, or opaque when it carries no alpha. */
    static int opaque(int colour) {
        return (colour >>> 24) == 0 ? colour | 0xFF000000 : colour;
    }
}
