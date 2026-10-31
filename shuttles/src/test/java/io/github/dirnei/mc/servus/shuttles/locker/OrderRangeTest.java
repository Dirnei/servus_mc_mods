package io.github.dirnei.mc.servus.shuttles.locker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.site.SiteChunks;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderRangeTest {

    /** Two chunks side by side: x 0 to 31, z 0 to 15. */
    private static final List<Long> SITE = List.of(SiteChunks.key(0, 0), SiteChunks.key(1, 0));

    @Test
    void insideSite() {
        assertTrue(OrderRange.inRange(SITE, true, 20, 5, 0));
        assertTrue(OrderRange.inRange(SITE, true, 0, 15, 0));
    }

    @Test
    void withinMargin() {
        assertTrue(OrderRange.inRange(SITE, true, -64, 5, 64));
        assertTrue(OrderRange.inRange(SITE, true, 31 + 64, 15 + 64, 64));
    }

    @Test
    void beyondMargin() {
        assertFalse(OrderRange.inRange(SITE, true, -65, 5, 64));
        assertFalse(OrderRange.inRange(SITE, true, 20, 15 + 300, 64));
        assertFalse(OrderRange.inRange(List.of(), true, 0, 0, 64));
    }

    @Test
    void otherDimension() {
        assertFalse(OrderRange.inRange(SITE, false, 20, 5, 64));
    }
}
