package io.github.dirnei.mc.servus.shuttles.terminal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** Picks the stock entry with the highest count among the accepted ones. */
public final class MostStocked {

    private MostStocked() {
    }

    /**
     * The accepted key with the highest count above zero; on a tie the one
     * that comes first in {@code stock}. Empty when no accepted key is in stock.
     */
    public static <K> Optional<K> pick(List<Map.Entry<K, Long>> stock, Predicate<? super K> accepted) {
        K best = null;
        long bestCount = 0;
        for (Map.Entry<K, Long> entry : stock) {
            long count = entry.getValue();
            if (count > bestCount && accepted.test(entry.getKey())) {
                best = entry.getKey();
                bestCount = count;
            }
        }
        return Optional.ofNullable(best);
    }
}
