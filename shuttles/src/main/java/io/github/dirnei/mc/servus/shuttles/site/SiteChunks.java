package io.github.dirnei.mc.servus.shuttles.site;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.LongPredicate;

/**
 * The chunks a site spans, as chunk keys packed like {@code ChunkPos.asLong}:
 * the chunk x in the low 32 bits, the chunk z in the high 32 bits.
 */
public final class SiteChunks {

    private SiteChunks() {
    }

    /** The key of the chunk with these chunk coordinates. */
    public static long key(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((chunkZ & 0xFFFFFFFFL) << 32);
    }

    /** The key of the chunk holding this block column. */
    public static long ofBlock(int blockX, int blockZ) {
        return key(blockX >> 4, blockZ >> 4);
    }

    public static int x(long key) {
        return (int) key;
    }

    public static int z(long key) {
        return (int) (key >>> 32);
    }

    /** The chunks of block columns given as {x, z} pairs, each chunk once. */
    public static Set<Long> ofBlocks(Collection<int[]> columns) {
        Set<Long> keys = new LinkedHashSet<>();
        for (int[] column : columns) {
            keys.add(ofBlock(column[0], column[1]));
        }
        return keys;
    }

    /**
     * The new span. While every chunk of the previous span is loaded, the
     * network sees the whole site and the span is exactly what it holds now.
     * While a part is not loaded, members beyond it may sit in a network of
     * their own, so the span only grows: the previous chunks plus the new ones.
     */
    public static Set<Long> merge(Set<Long> previous, Set<Long> current, LongPredicate loaded) {
        Set<Long> merged = new LinkedHashSet<>(current);
        boolean whole = true;
        for (long key : previous) {
            whole &= loaded.test(key);
        }
        if (!whole) {
            merged.addAll(previous);
        }
        return merged;
    }
}
