package io.github.dirnei.mc.servus.storage.site;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SiteCapacityTest {

    @Test
    void capacityIsUpgradesTimesChunks() {
        assertEquals(18, SiteCapacity.capacity(2, 9));
        assertEquals(0, SiteCapacity.capacity(0, 9));
    }

    @Test
    void noUpgradeIsNoError() {
        assertEquals(SiteCapacity.State.NO_UPGRADE, SiteCapacity.state(30, 0, 9));
    }

    @Test
    void equalSpanIsCovered() {
        assertEquals(SiteCapacity.State.COVERED, SiteCapacity.state(9, 1, 9));
    }

    @Test
    void oneChunkMoreIsOver() {
        assertEquals(SiteCapacity.State.OVER, SiteCapacity.state(10, 1, 9));
        assertEquals(SiteCapacity.State.COVERED, SiteCapacity.state(10, 2, 9));
    }
}
