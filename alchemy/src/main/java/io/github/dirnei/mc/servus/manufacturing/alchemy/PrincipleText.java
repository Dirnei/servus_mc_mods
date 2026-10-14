package io.github.dirnei.mc.servus.manufacturing.alchemy;

import java.util.ArrayList;
import java.util.List;

/** English text for amounts of principles, as the journal lists recipes. Pure Java, no Minecraft types. */
public final class PrincipleText {

    private PrincipleText() {
    }

    /** "Salt", "Sulfur" or "Mercury". */
    public static String name(Principle principle) {
        String id = principle.id();
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    /** The principles that are not zero, in principle order, like "100 Salt, 10 Mercury"; "nothing" when all are zero. */
    public static String amounts(Principles principles) {
        List<String> parts = new ArrayList<>();
        for (Principle principle : Principle.values()) {
            int amount = principles.get(principle);
            if (amount > 0) {
                parts.add(amount + " " + name(principle));
            }
        }
        return parts.isEmpty() ? "nothing" : String.join(", ", parts);
    }

    /** One recipe line, like "Cobblestone: 100 Salt, 10 Mercury". */
    public static String line(String label, Principles principles) {
        return label + ": " + amounts(principles);
    }
}
