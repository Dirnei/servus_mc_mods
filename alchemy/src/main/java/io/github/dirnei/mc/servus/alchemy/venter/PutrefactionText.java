package io.github.dirnei.mc.servus.alchemy.venter;

import java.util.List;

/** English lines for putrefaction and the Dirt mixing recipes, as the journal lists them. Pure Java, no Minecraft types. */
public final class PutrefactionText {

    private PutrefactionText() {
    }

    /** One putrefaction recipe, like "8 rot: 1 Humus, 60 s in fresh straw, 240 s cold". */
    public static String line(int count, String inputLabel, int resultCount, String resultLabel, int warmSeconds, int coldSeconds) {
        return count + " " + inputLabel + ": " + resultCount + " " + resultLabel + ", " + warmSeconds + " s in fresh straw, "
                + coldSeconds + " s cold";
    }

    /** One mixing recipe, like "4 Sand, 1 Clay Ball, 1 Humus, 250 mB water: 8 Dirt"; {@code water} 0 leaves the water out. */
    public static String mixLine(List<String> parts, int water, int resultCount, String resultLabel) {
        String inputs = String.join(", ", parts);
        if (water > 0) {
            inputs += (parts.isEmpty() ? "" : ", ") + water + " mB water";
        }
        return inputs + ": " + resultCount + " " + resultLabel;
    }
}
