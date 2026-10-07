package io.github.dirnei.mc.servus.storage.rack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Picks the wood a rack remembers from the planks in its recipe grid. The
 * id that appears most often wins; on a tie the middle entry decides. Pure,
 * no Minecraft imports.
 */
public final class RackWood {

    private RackWood() {
    }

    /**
     * @param planks planks ids in grid reading order
     * @return the chosen id, or null when the list is empty
     */
    public static String pick(List<String> planks) {
        if (planks.isEmpty()) {
            return null;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String id : planks) {
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
        return tie ? planks.get(planks.size() / 2) : best;
    }
}
