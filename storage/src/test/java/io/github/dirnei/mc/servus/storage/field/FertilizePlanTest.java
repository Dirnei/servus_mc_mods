package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FertilizePlanTest {

    @Test
    void fetchIsWhatThePlotsNeed() {
        assertEquals(4, FertilizePlan.toFetch(2, 2, 20, 0, 128));
    }

    @Test
    void fetchKeepsTheReserve() {
        assertEquals(3, FertilizePlan.toFetch(4, 2, 8, 5, 128));
        assertEquals(0, FertilizePlan.toFetch(4, 2, 10, 10, 128));
        assertEquals(0, FertilizePlan.toFetch(4, 2, 3, 10, 128));
    }

    @Test
    void reserveZeroMayTakeAll() {
        assertEquals(6, FertilizePlan.toFetch(10, 2, 6, 0, 128));
    }

    @Test
    void fetchFitsTheCargo() {
        assertEquals(64, FertilizePlan.toFetch(100, 2, 1000, 0, 64));
        assertEquals(0, FertilizePlan.toFetch(2, 2, 20, 0, 0));
    }

    @Test
    void targetsPerMode() {
        assertEquals(0, FertilizePlan.targets(FertilizeMode.OFF, 3, 5, 0, 5, 50));
        assertEquals(3, FertilizePlan.targets(FertilizeMode.AFTER_PLANTING, 3, 5, 0, 5, 50));
        assertEquals(5, FertilizePlan.targets(FertilizeMode.UNTIL_RIPE, 3, 5, 1, 6, 50));
    }

    @Test
    void untilRipeStopsAtTheThreshold() {
        assertEquals(0, FertilizePlan.targets(FertilizeMode.UNTIL_RIPE, 0, 2, 2, 4, 50));
        assertEquals(0, FertilizePlan.targets(FertilizeMode.UNTIL_RIPE, 0, 1, 3, 4, 50));
        assertEquals(3, FertilizePlan.targets(FertilizeMode.UNTIL_RIPE, 0, 3, 1, 4, 50));
        assertTrue(FertilizePlan.belowShare(0, 0, 50), "nothing planted counts as below");
    }

    @Test
    void interval() {
        assertTrue(FertilizePlan.intervalPassed(5, -1, 600), "a field that never called may call");
        assertFalse(FertilizePlan.intervalPassed(1000, 500, 600));
        assertTrue(FertilizePlan.intervalPassed(1100, 500, 600));
    }

    @Test
    void dueNeedsTargetsStockAndInterval() {
        assertTrue(FertilizePlan.due(2, 20, 0, 100, -1, 600));
        assertFalse(FertilizePlan.due(0, 20, 0, 100, -1, 600), "no plot needs a dose");
        assertFalse(FertilizePlan.due(2, 10, 10, 100, -1, 600), "stock equal to the reserve");
        assertFalse(FertilizePlan.due(2, 20, 0, 100, 50, 600), "the interval has not passed");
    }

    @Test
    void reserveSteps() {
        assertEquals(0, FertilizePlan.stepReserve(5, -10));
        assertEquals(74, FertilizePlan.stepReserve(64, 10));
        assertEquals(FertilizePlan.MAX_RESERVE, FertilizePlan.stepReserve(FertilizePlan.MAX_RESERVE, 1));
    }
}
