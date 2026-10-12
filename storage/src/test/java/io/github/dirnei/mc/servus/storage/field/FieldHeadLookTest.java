package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FieldHeadLookTest {

    @Test
    void ripeStepEmpty() {
        assertEquals(FieldHeadLook.EMPTY, FieldHeadLook.ripeStep(0, 0, 50, false));
        assertEquals(FieldHeadLook.EMPTY, FieldHeadLook.ripeStep(10, 0, 50, false));
        assertEquals(0, FieldHeadLook.goldColumns(FieldHeadLook.EMPTY));
    }

    @Test
    void ripeStepProgress() {
        // Threshold 50 percent over 8 planted: 1 ripe is a quarter of the way.
        assertEquals(1, FieldHeadLook.ripeStep(100, 1, 50, false));
        assertEquals(2, FieldHeadLook.ripeStep(8, 1, 50, false));
        assertEquals(3, FieldHeadLook.ripeStep(8, 2, 50, false));
        assertEquals(4, FieldHeadLook.ripeStep(8, 3, 50, false));
        assertEquals(5, FieldHeadLook.goldColumns(3));
    }

    @Test
    void ripeStepBelowDueStaysGold() {
        // Share at or beyond the threshold but no crop set: not due, the bar stays gold.
        assertEquals(FieldHeadLook.GOLD_STEPS, FieldHeadLook.ripeStep(4, 2, 50, false));
        assertEquals(FieldHeadLook.GOLD_STEPS, FieldHeadLook.ripeStep(4, 4, 50, false));
        assertEquals(7, FieldHeadLook.goldColumns(FieldHeadLook.GOLD_STEPS));
    }

    @Test
    void ripeStepDue() {
        assertEquals(FieldHeadLook.READY, FieldHeadLook.ripeStep(4, 2, 50, true));
        assertEquals(0, FieldHeadLook.goldColumns(FieldHeadLook.READY));
    }

    @Test
    void noProblemWhileServingWell() {
        assertFalse(FieldHeadLook.problem(true, FieldWork.GROWING, false, true, FertilizeStatus.OFF));
        assertFalse(FieldHeadLook.problem(true, FieldWork.WORKING, false, true, null));
        assertFalse(FieldHeadLook.problem(true, FieldWork.HARVEST_DUE, false, true, FertilizeStatus.OFF));
    }

    @Test
    void problemsOfEveryTab() {
        assertTrue(FieldHeadLook.problem(false, null, false, false, null), "a head serving no field");
        assertTrue(FieldHeadLook.problem(true, FieldWork.NO_HARVESTER, false, true, FertilizeStatus.OFF), "no harvester");
        assertTrue(FieldHeadLook.problem(true, FieldWork.NO_WAY, false, true, FertilizeStatus.OFF), "unreachable");
        assertTrue(FieldHeadLook.problem(true, FieldWork.GROWING, false, false, FertilizeStatus.OFF), "dry field");
        assertTrue(FieldHeadLook.problem(true, FieldWork.GROWING, true, true, FertilizeStatus.OFF), "corroded rails");
        assertTrue(FieldHeadLook.problem(true, FieldWork.GROWING, false, true, FertilizeStatus.NO_SPREADER), "no spreader");
        assertFalse(FieldHeadLook.problem(true, FieldWork.GROWING, false, true, FertilizeStatus.NO_STOCK), "no fertilizer is no problem");
    }

    @Test
    void workProblemsMatchTheGui() {
        for (FieldWork work : FieldWork.values()) {
            boolean fine = work == FieldWork.WORKING || work == FieldWork.HARVEST_DUE || work == FieldWork.READY
                    || work == FieldWork.GROWING || work == FieldWork.WAITING_RIPE;
            assertEquals(!fine, work.problem(), work.name());
        }
    }

    @Test
    void waterLevelEmptyAndFull() {
        assertEquals(0, FieldHeadLook.waterLevel(0, 8000));
        assertEquals(8, FieldHeadLook.waterLevel(8000, 8000));
        assertEquals(8, FieldHeadLook.waterLevel(9000, 8000));
    }

    @Test
    void waterLevelSteps() {
        assertEquals(1, FieldHeadLook.waterLevel(1, 8000));
        assertEquals(4, FieldHeadLook.waterLevel(4000, 8000));
        assertEquals(4, FieldHeadLook.waterLevel(4999, 8000));
        assertEquals(7, FieldHeadLook.waterLevel(7999, 8000));
    }

    @Test
    void waterLevelZeroCapacity() {
        assertEquals(0, FieldHeadLook.waterLevel(100, 0));
    }
}
