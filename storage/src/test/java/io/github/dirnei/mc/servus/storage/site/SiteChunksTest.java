package io.github.dirnei.mc.servus.storage.site;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SiteChunksTest {

    @Test
    void keyRoundTripsNegativeCoordinates() {
        long key = SiteChunks.key(-3, 7);
        assertEquals(-3, SiteChunks.x(key));
        assertEquals(7, SiteChunks.z(key));
        long other = SiteChunks.key(5, -12);
        assertEquals(5, SiteChunks.x(other));
        assertEquals(-12, SiteChunks.z(other));
    }

    @Test
    void blockToChunkFloors() {
        assertEquals(SiteChunks.key(-1, 0), SiteChunks.ofBlock(-1, 15));
        assertEquals(SiteChunks.key(1, -1), SiteChunks.ofBlock(16, -16));
    }

    @Test
    void positionsInOneChunkCountOnce() {
        Set<Long> span = SiteChunks.ofBlocks(List.of(new int[]{0, 0}, new int[]{15, 15}, new int[]{16, 0}));
        assertEquals(Set.of(SiteChunks.key(0, 0), SiteChunks.key(1, 0)), span);
    }

    @Test
    void mergeKeepsUnloadedChunks() {
        long home = SiteChunks.key(0, 0);
        long far = SiteChunks.key(1, 0);
        Set<Long> merged = SiteChunks.merge(Set.of(home, far), Set.of(home), key -> key == home);
        assertEquals(Set.of(home, far), merged);
    }

    @Test
    void mergeDropsLoadedChunksThatHoldNothing() {
        long home = SiteChunks.key(0, 0);
        long far = SiteChunks.key(1, 0);
        Set<Long> merged = SiteChunks.merge(Set.of(home, far), Set.of(home), key -> true);
        assertEquals(Set.of(home), merged);
    }

    @Test
    void mergeAddsNewChunks() {
        long home = SiteChunks.key(0, 0);
        long next = SiteChunks.key(0, 1);
        assertEquals(Set.of(home, next), SiteChunks.merge(Set.of(home), Set.of(home, next), key -> true));
    }
}
