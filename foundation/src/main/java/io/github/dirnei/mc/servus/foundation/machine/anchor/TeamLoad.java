package io.github.dirnei.mc.servus.foundation.machine.anchor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A team's chunk load: the union of the chunks held by all its entries
 * (anchors and woken sites), each chunk counted once. Pure; a chunk is any
 * value with equals, in practice the dimension plus the packed chunk.
 */
public final class TeamLoad {

    private TeamLoad() {
    }

    /** How many distinct chunks the entries hold together. */
    public static <C> int load(Map<String, ? extends Collection<C>> entries) {
        return union(entries, null).size();
    }

    /**
     * Whether the entry {@code key} may hold {@code chunks}: the union of every
     * other entry plus these chunks stays within {@code cap}. The entry's own
     * old chunks are ignored, so an entry that changes its area is checked fairly.
     */
    public static <C> boolean fits(Map<String, ? extends Collection<C>> entries, String key, Collection<C> chunks, int cap) {
        return loadWith(entries, key, chunks) <= cap;
    }

    /** The load if the entry {@code key} held {@code chunks} instead of what it holds now. */
    public static <C> int loadWith(Map<String, ? extends Collection<C>> entries, String key, Collection<C> chunks) {
        Set<C> union = union(entries, key);
        union.addAll(chunks);
        return union.size();
    }

    private static <C> Set<C> union(Map<String, ? extends Collection<C>> entries, String skip) {
        Set<C> union = new HashSet<>();
        for (Map.Entry<String, ? extends Collection<C>> entry : entries.entrySet()) {
            if (!entry.getKey().equals(skip)) {
                union.addAll(entry.getValue());
            }
        }
        return union;
    }
}
