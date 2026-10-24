package io.github.dirnei.mc.servus.alchemy.coagulator;

/** English lines for the Coagulator's seeds, as the journal lists them. Pure Java, no Minecraft types. */
public final class SeedText {

    private SeedText() {
    }

    /** One seed and what it grows, like "Kandis grows Salt Crystal". */
    public static String line(String seedLabel, String resultLabel) {
        return seedLabel + " grows " + resultLabel;
    }
}
