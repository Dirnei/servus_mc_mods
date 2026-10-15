package io.github.dirnei.mc.servus.storage.field;

import java.util.Map;
import java.util.Optional;

/**
 * Picks a field's produce from what one harvest brought home: the item with
 * the highest count, leaving out the crop item whenever another item came too.
 * Items are named by registry id. Pure Java.
 */
public final class Produce {

    private Produce() {
    }

    /**
     * @param tally items brought home by id and count
     * @param crop  the id of the field's crop item, null in harvest only mode
     * @return the produce, empty when nothing came home; ties go to the smaller id
     */
    public static Optional<String> pick(Map<String, Long> tally, String crop) {
        boolean othersCame = tally.entrySet().stream().anyMatch(entry -> entry.getValue() > 0 && !entry.getKey().equals(crop));
        String best = null;
        long bestCount = 0;
        for (Map.Entry<String, Long> entry : tally.entrySet()) {
            String id = entry.getKey();
            long count = entry.getValue();
            if (count <= 0 || othersCame && id.equals(crop)) {
                continue;
            }
            if (best == null || count > bestCount || count == bestCount && id.compareTo(best) < 0) {
                best = id;
                bestCount = count;
            }
        }
        return Optional.ofNullable(best);
    }
}
