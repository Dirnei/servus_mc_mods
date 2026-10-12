package io.github.dirnei.mc.servus.storage.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WirelessReachTest {

    @Test
    void upgradeAndPowerIsCapable() {
        assertTrue(WirelessReach.capable(true, 500, 4));
    }

    @Test
    void upgradeWithoutEnoughPowerIsNotCapable() {
        assertFalse(WirelessReach.capable(true, 2, 4));
        assertEquals(4, WirelessReach.needs(4));
    }

    @Test
    void noUpgradeIsNeverCapable() {
        assertFalse(WirelessReach.capable(false, 50_000, 4));
        assertFalse(WirelessReach.capable(false, 50_000, 0));
    }

    @Test
    void fractionsRoundUp() {
        assertEquals(5, WirelessReach.needs(4.2));
        assertFalse(WirelessReach.capable(true, 4, 4.2));
        assertTrue(WirelessReach.capable(true, 5, 4.2));
    }

    @Test
    void atLeastOneFeAtCostZero() {
        assertEquals(1, WirelessReach.needs(0));
        assertFalse(WirelessReach.capable(true, 0, 0));
        assertTrue(WirelessReach.capable(true, 1, 0));
    }
}
