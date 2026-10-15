package io.github.dirnei.mc.servus.storage.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RemoteSiteTest {

    @Test
    void onlineIsOnlineWhateverTheUpgrades() {
        assertEquals(RemoteSite.State.ONLINE, RemoteSite.state(true, SiteCapacity.State.NO_UPGRADE));
        assertEquals(RemoteSite.State.ONLINE, RemoteSite.state(true, SiteCapacity.State.OVER));
    }

    @Test
    void asleepWithCoverCanWake() {
        assertEquals(RemoteSite.State.SLEEPING, RemoteSite.state(false, SiteCapacity.State.COVERED));
        assertTrue(RemoteSite.State.SLEEPING.orderable());
    }

    @Test
    void asleepWithoutCoverIsUnreachable() {
        assertEquals(RemoteSite.State.UNREACHABLE, RemoteSite.state(false, SiteCapacity.State.NO_UPGRADE));
        assertEquals(RemoteSite.State.UNREACHABLE, RemoteSite.state(false, SiteCapacity.State.OVER));
        assertFalse(RemoteSite.State.UNREACHABLE.orderable());
    }
}
