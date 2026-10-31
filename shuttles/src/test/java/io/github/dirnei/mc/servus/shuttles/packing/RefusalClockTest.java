package io.github.dirnei.mc.servus.shuttles.packing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.packing.RefusalClock.TargetState;
import io.github.dirnei.mc.servus.shuttles.packing.RefusalClock.Verdict;
import org.junit.jupiter.api.Test;

class RefusalClockTest {

    private static final int TIMEOUT = 1200;

    @Test
    void unloadedWaitsAndClearsClock() {
        Verdict verdict = RefusalClock.judge(TargetState.UNLOADED, false, 100, 5000, TIMEOUT);
        assertFalse(verdict.end());
        assertEquals(-1, verdict.refusedSince());
    }

    @Test
    void removedEndsAtOnce() {
        assertTrue(RefusalClock.judge(TargetState.REMOVED, false, -1, 10, TIMEOUT).end());
        assertTrue(RefusalClock.judge(TargetState.REMOVED, true, -1, 10, TIMEOUT).end());
    }

    @Test
    void firstRefusalStartsClock() {
        Verdict verdict = RefusalClock.judge(TargetState.REFUSING, false, -1, 300, TIMEOUT);
        assertFalse(verdict.end());
        assertEquals(300, verdict.refusedSince());
    }

    @Test
    void machineRefusingEndsAfterTimeout() {
        assertFalse(RefusalClock.judge(TargetState.REFUSING, false, 300, 1500, TIMEOUT).end());
        assertTrue(RefusalClock.judge(TargetState.REFUSING, false, 300, 1501, TIMEOUT).end());
    }

    @Test
    void refusingWithinTimeoutWaits() {
        Verdict verdict = RefusalClock.judge(TargetState.REFUSING, false, 300, 900, TIMEOUT);
        assertFalse(verdict.end());
        assertEquals(300, verdict.refusedSince());
    }

    @Test
    void playerRefusingNeverEnds() {
        Verdict verdict = RefusalClock.judge(TargetState.REFUSING, true, 300, 100_000, TIMEOUT);
        assertFalse(verdict.end());
        assertEquals(-1, verdict.refusedSince());
    }

    @Test
    void cutEndsAfterTimeoutForPlayerToo() {
        assertTrue(RefusalClock.judge(TargetState.CUT, true, 0, 1201, TIMEOUT).end());
        assertTrue(RefusalClock.judge(TargetState.CUT, false, 0, 1201, TIMEOUT).end());
    }

    @Test
    void cutWithinTimeoutWaits() {
        Verdict first = RefusalClock.judge(TargetState.CUT, false, -1, 50, TIMEOUT);
        assertFalse(first.end());
        assertEquals(50, first.refusedSince());
        assertFalse(RefusalClock.judge(TargetState.CUT, false, first.refusedSince(), 250, TIMEOUT).end());
    }

    @Test
    void cutAndRefusingShareClock() {
        Verdict refused = RefusalClock.judge(TargetState.REFUSING, false, -1, 100, TIMEOUT);
        Verdict cut = RefusalClock.judge(TargetState.CUT, false, refused.refusedSince(), 700, TIMEOUT);
        assertEquals(100, cut.refusedSince());
        assertTrue(RefusalClock.judge(TargetState.REFUSING, false, cut.refusedSince(), 1301, TIMEOUT).end());
    }

    @Test
    void takingClearsClock() {
        Verdict verdict = RefusalClock.judge(TargetState.TAKING, false, 100, 1000, TIMEOUT);
        assertFalse(verdict.end());
        assertEquals(-1, verdict.refusedSince());
        assertFalse(RefusalClock.judge(TargetState.REFUSING, false, verdict.refusedSince(), 2000, TIMEOUT).end());
    }
}
