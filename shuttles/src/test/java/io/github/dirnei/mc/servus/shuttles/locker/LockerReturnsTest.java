package io.github.dirnei.mc.servus.shuttles.locker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LockerReturnsTest {

    @Test
    void expiresAfterReturnTime() {
        assertFalse(LockerReturns.expired(LockerReturns.State.READY, 1000, 24999, 24000));
        assertTrue(LockerReturns.expired(LockerReturns.State.READY, 1000, 25000, 24000));
        assertEquals(1, LockerReturns.ticksLeft(1000, 24999, 24000));
        assertEquals(0, LockerReturns.ticksLeft(1000, 30000, 24000));
    }

    @Test
    void reservedNeverExpires() {
        assertFalse(LockerReturns.expired(LockerReturns.State.RESERVED, 0, 1_000_000, 24000));
        assertFalse(LockerReturns.expired(LockerReturns.State.RETURNING, 0, 1_000_000, 24000));
    }
}
