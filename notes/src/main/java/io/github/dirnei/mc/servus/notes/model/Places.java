package io.github.dirnei.mc.servus.notes.model;

import java.util.Locale;

/** How spots read in the notebook. */
public final class Places {

    private Places() {
    }

    /** The short name of a dimension: Overworld, Nether and End by name, others by their path in title case. */
    public static String label(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> titleCase(dimension.substring(dimension.indexOf(':') + 1));
        };
    }

    /** The coordinates of a spot, for example "X -120 Y 64 Z 340 Nether". */
    public static String coordinates(Spot spot) {
        return "X " + spot.x() + " Y " + spot.y() + " Z " + spot.z() + " " + label(spot.dimension());
    }

    /** The chip text of a spot: its name before the coordinates when it has one, for example "Portal room · X 120 Y 64 Z -30 Nether". */
    public static String chip(Spot spot) {
        String coordinates = coordinates(spot);
        return spot.name().isBlank() ? coordinates : spot.name() + " · " + coordinates;
    }

    private static String titleCase(String path) {
        StringBuilder out = new StringBuilder();
        for (String word : path.split("[_/\\-.]+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return out.toString();
    }
}
