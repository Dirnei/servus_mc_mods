package io.github.dirnei.mc.servus.alchemy;

import java.util.ArrayList;
import java.util.List;

/** English lines for purification recipes, as the journal lists them. Pure Java, no Minecraft types. */
public final class PurificationText {

    private PurificationText() {
    }

    /**
     * One recipe, like "8 Sugar, 500 mB water: 100 Salt, 1 Kandis". The Salt is left out when it is 0 and the residue
     * when {@code residueCount} is 0.
     */
    public static String line(int count, String label, int water, int salt, int residueCount, String residueLabel) {
        List<String> gives = new ArrayList<>();
        if (salt > 0) {
            gives.add(salt + " Salt");
        }
        if (residueCount > 0) {
            gives.add(residueCount + " " + residueLabel);
        }
        return count + " " + label + ", " + water + " mB water: " + (gives.isEmpty() ? "nothing" : String.join(", ", gives));
    }

    /** One kind of colour wash standing for a recipe per colour, like "8 Concrete Powder of any colour, 250 mB water: its Concrete". */
    public static String kindLine(int count, String label, int water, String residueLabel) {
        return count + " " + label + ", " + water + " mB water: " + residueLabel;
    }
}
