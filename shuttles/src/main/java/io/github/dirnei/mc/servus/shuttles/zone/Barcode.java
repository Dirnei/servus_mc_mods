package io.github.dirnei.mc.servus.shuttles.zone;

import java.util.ArrayList;
import java.util.List;

/**
 * The bars of the barcode printed on a location code sticker, hashed from the
 * code so every code has its own pattern. Only a look, not a real symbology.
 * Pure Java.
 */
public final class Barcode {

    /** One bar from module {@code start} up to, not including, module {@code end}. */
    public record Bar(int start, int end) {
    }

    private Barcode() {
    }

    /**
     * The bars over {@code modules} modules: a guard bar at each end and
     * hashed bars between. Bars are maximal runs, so two bars never touch.
     */
    public static List<Bar> bars(String text, int modules) {
        boolean[] dark = new boolean[modules];
        long state = text.hashCode() * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
        for (int i = 0; i < modules; i++) {
            state ^= state << 13;
            state ^= state >>> 7;
            state ^= state << 17;
            dark[i] = (state & 3) != 0;
        }
        if (modules >= 4) {
            dark[0] = true;
            dark[1] = false;
            dark[modules - 2] = false;
            dark[modules - 1] = true;
        }
        List<Bar> bars = new ArrayList<>();
        int start = -1;
        for (int i = 0; i <= modules; i++) {
            boolean on = i < modules && dark[i];
            if (on && start < 0) {
                start = i;
            } else if (!on && start >= 0) {
                bars.add(new Bar(start, i));
                start = -1;
            }
        }
        return bars;
    }
}
