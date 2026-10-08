package io.github.dirnei.mc.servus.storage.pattern;

/**
 * How many of the nine lights on a Pattern Storage front are lit. One per
 * pattern for a block of 9; other capacities show the fill level scaled to
 * 9, rounded up while any pattern is stored. Pure, no Minecraft imports.
 */
public final class PatternLights {

    public static final int LIGHTS = 9;

    private PatternLights() {
    }

    public static int lit(int stored, int capacity) {
        if (stored <= 0 || capacity <= 0) {
            return 0;
        }
        if (stored >= capacity) {
            return LIGHTS;
        }
        int scaled = (stored * LIGHTS + capacity - 1) / capacity;
        return Math.max(1, Math.min(LIGHTS, scaled));
    }
}
