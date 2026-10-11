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
        assertEquals(Reason.NO_UPGRADE, WakeAllowance.check(false, true, State.NO_UPGRADE, false, 9, 4));
        assertEquals(Reason.OVER_CAPACITY, WakeAllowance.check(false, true, State.OVER, false, 9, 4));
    }

    @Test
    void thenOwnerThenFtbThenCap() {
        assertEquals(Reason.NO_OWNER, WakeAllowance.check(false, true, State.COVERED, false, 9, 4));
        assertEquals(Reason.FTB_CHUNKS, WakeAllowance.check(true, true, State.COVERED, false, 9, 4));
        assertEquals(Reason.TEAM_LIMIT, WakeAllowance.check(true, false, State.COVERED, false, 4, 4));
    }

    @Test
    void registeredSiteNeverHitsTheCap() {
        assertEquals(Reason.ALLOWED, WakeAllowance.check(true, false, State.COVERED, true, 4, 4));
    }

    @Test
    void belowTheCapIsAllowed() {
        assertEquals(Reason.ALLOWED, WakeAllowance.check(true, false, State.COVERED, false, 3, 4));
    }

    @Test
    void onlyCapacityIsTheHardLimit() {
        assertTrue(Reason.NO_UPGRADE.hardLimit());
        assertTrue(Reason.OVER_CAPACITY.hardLimit());
        assertFalse(Reason.TEAM_LIMIT.hardLimit());
        assertFalse(Reason.FTB_CHUNKS.hardLimit());
    }
}
