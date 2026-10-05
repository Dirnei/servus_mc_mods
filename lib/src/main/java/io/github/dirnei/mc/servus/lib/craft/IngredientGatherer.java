package io.github.dirnei.mc.servus.lib.craft;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Decides how to take required item counts out of a list of slots. Pure Java:
 * items are identified by any key type with equals and hashCode.
 */
public final class IngredientGatherer {

    private IngredientGatherer() {
    }

    /**
     * @param slotKeys   item key per slot, null for an empty slot
     * @param slotCounts stack size per slot
     * @param required   item key to total count needed
     * @param <K>        item key type
     * @return how many to take from each slot, or empty when the requirement cannot be met
     */
    public static <K> Optional<int[]> gather(List<K> slotKeys, int[] slotCounts, Map<K, Integer> required) {
        int[] take = new int[slotKeys.size()];
        Map<K, Integer> remaining = new HashMap<>(required);
        remaining.values().removeIf(count -> count <= 0);
        for (int slot = 0; slot < slotKeys.size() && !remaining.isEmpty(); slot++) {
            K key = slotKeys.get(slot);
            if (key == null) {
                continue;
            }
            Integer needed = remaining.get(key);
            if (needed == null) {
                continue;
            }
            int taken = Math.min(needed, slotCounts[slot]);
            if (taken <= 0) {
                continue;
            }
            take[slot] = taken;
            if (taken == needed) {
                remaining.remove(key);
            } else {
                remaining.put(key, needed - taken);
            }
        }
        return remaining.isEmpty() ? Optional.of(take) : Optional.empty();
    }
}
