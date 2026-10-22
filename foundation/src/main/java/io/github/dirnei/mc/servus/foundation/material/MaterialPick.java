package io.github.dirnei.mc.servus.foundation.material;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Picks the material a block remembers from the material ids in its recipe
 * grid, the same for every kind. The id that appears most often wins; on a
 * tie the middle entry decides. Pure, no Minecraft imports.
 */
public final class MaterialPick {

    private MaterialPick() {
    }

    /**
     * @param ids material ids in grid reading order
     * @return the chosen id, or null when the list is empty
     */
    public static String pick(List<String> ids) {
        if (ids.isEmpty()) {
            return null;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String id : ids) {
            counts.merge(id, 1, Integer::sum);
        }
        String best = null;
        int bestCount = 0;
        boolean tie = false;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                best = entry.getKey();
                bestCount = entry.getValue();
                tie = false;
            } else if (entry.getValue() == bestCount) {
                tie = true;
            }
        }
        return tie ? ids.get(ids.size() / 2) : best;
    }
}
