package io.github.dirnei.mc.servus.storage.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.storage.site.SiteCapacity.State;
import io.github.dirnei.mc.servus.storage.site.WakeAllowance.Reason;
import org.junit.jupiter.api.Test;

class WakeAllowanceTest {

    @Test
    void capacityComesFirst() {
        assertEquals(Reason.NO_UPGRADE, WakeAllowance.check(false, true, State.NO_UPGRADE, false, false));
        assertEquals(Reason.OVER_CAPACITY, WakeAllowance.check(false, true, State.OVER, false, false));
    }

    @Test
    void thenOwnerThenFtbThenChunkCap() {
        assertEquals(Reason.NO_OWNER, WakeAllowance.check(false, true, State.COVERED, false, false));
        assertEquals(Reason.FTB_CHUNKS, WakeAllowance.check(true, true, State.COVERED, false, false));
        assertEquals(Reason.TEAM_LIMIT, WakeAllowance.check(true, false, State.COVERED, false, false));
    }

    @Test
    void registeredSiteNeverHitsTheCap() {
        assertEquals(Reason.ALLOWED, WakeAllowance.check(true, false, State.COVERED, true, false));
    }

    @Test
    void fittingSpanIsAllowed() {
        assertEquals(Reason.ALLOWED, WakeAllowance.check(true, false, State.COVERED, false, true));
    }

    @Test
    void onlyCapacityIsTheHardLimit() {
        assertTrue(Reason.NO_UPGRADE.hardLimit());
        assertTrue(Reason.OVER_CAPACITY.hardLimit());
        assertFalse(Reason.TEAM_LIMIT.hardLimit());
        assertFalse(Reason.FTB_CHUNKS.hardLimit());
    }

    @Test
    void noPowerAfterCapacityBeforeOwner() {
        assertEquals(Reason.NO_UPGRADE, WakeAllowance.check(true, false, State.NO_UPGRADE, false, true, 100, 360));
        assertEquals(Reason.OVER_CAPACITY, WakeAllowance.check(true, false, State.OVER, false, true, 100, 360));
        assertEquals(Reason.NO_POWER, WakeAllowance.check(true, false, State.COVERED, false, true, 100, 360));
        assertEquals(Reason.NO_POWER, WakeAllowance.check(false, true, State.COVERED, false, false, 100, 360));
    }

    @Test
    void enoughStoredWakes() {
        assertEquals(Reason.ALLOWED, WakeAllowance.check(true, false, State.COVERED, false, true, 5000, 360));
        assertEquals(Reason.ALLOWED, WakeAllowance.check(true, false, State.COVERED, false, true, 360, 360));
    }

    @Test
    void noPowerIsNoHardLimit() {
        assertFalse(Reason.NO_POWER.hardLimit());
    }
}
