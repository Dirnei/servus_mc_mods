package io.github.dirnei.mc.servus.alchemy.venter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PutrefactionWorkTest {

    @Test
    void fullBedTakesTheWarmTime() {
        assertEquals(1200, PutrefactionWork.ticksAt(1200, 4800, 1000));
    }

    @Test
    void emptyBedTakesTheColdTime() {
        assertEquals(4800, PutrefactionWork.ticksAt(1200, 4800, 0));
    }

    @Test
    void halfBedRunsAtTheMeanSpeed() {
        assertEquals(1920, PutrefactionWork.ticksAt(1200, 4800, 500));
    }

    @Test
    void pointsPerTickAreNeverZero() {
        assertTrue(PutrefactionWork.perTick(0, 0, 0) > 0);
        assertTrue(PutrefactionWork.perTick(1, 1, 0) > 0);
        assertTrue(PutrefactionWork.perTick(1200, 4800, -5) > 0);
    }

    @Test
    void coldTimeBelowWarmTimeCountsAsWarm() {
        assertEquals(100, PutrefactionWork.ticksAt(100, 10, 0));
    }

    @Test
    void noOverflowAtLongTimes() {
        assertEquals(72_000L * 72_000L * 1000L, PutrefactionWork.total(72_000, 72_000));
        assertEquals(72_000, PutrefactionWork.ticksAt(72_000, 72_000, 0));
        assertEquals(72_000, PutrefactionWork.ticksAt(72_000, 72_000, 1000));
    }
}
