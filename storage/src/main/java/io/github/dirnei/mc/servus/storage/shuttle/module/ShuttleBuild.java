package io.github.dirnei.mc.servus.storage.shuttle.module;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Checks a build sheet against the supplies in a workshop, counted by item id. Pure, no Minecraft imports. */
public final class ShuttleBuild {

    private ShuttleBuild() {
    }

    /** How many of each supply are still missing, in sheet order; empty when the sheet is complete. */
    public static Map<String, Integer> missing(List<ShuttleTiers.Supply> sheet, Map<String, Integer> have) {
        Map<String, Integer> missing = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> need : needs(sheet).entrySet()) {
            int lacking = need.getValue() - have.getOrDefault(need.getKey(), 0);
            if (lacking > 0) {
                missing.put(need.getKey(), lacking);
            }
        }
        return missing;
    }

    /** Whether there is a sheet and every supply of it is there. */
    public static boolean ready(List<ShuttleTiers.Supply> sheet, Map<String, Integer> have) {
        return !sheet.isEmpty() && missing(sheet, have).isEmpty();
    }

    /** The sheet as item id to count, with repeated ids added up. */
    public static Map<String, Integer> needs(List<ShuttleTiers.Supply> sheet) {
        Map<String, Integer> needs = new LinkedHashMap<>();
        for (ShuttleTiers.Supply supply : sheet) {
            if (supply.count() > 0) {
                needs.merge(supply.id(), supply.count(), Integer::sum);
            }
        }
        return needs;
    }
}
