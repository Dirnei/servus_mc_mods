package io.github.dirnei.mc.servus.shuttles.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExportPlanTest {

    @Test
    void belowTheThreshold() {
        assertEquals(0, ExportPlan.surplus(1800, 2000, 0));
    }

    @Test
    void atTheThreshold() {
        assertEquals(0, ExportPlan.surplus(2000, 2000, 0));
    }

    @Test
    void aboveTheThreshold() {
        assertEquals(300, ExportPlan.surplus(2300, 2000, 0));
        assertEquals(5, ExportPlan.surplus(5, 0, 0));
    }

    @Test
    void queuedAmountsAreSubtracted() {
        assertEquals(0, ExportPlan.surplus(2300, 2000, 300));
        assertEquals(100, ExportPlan.surplus(2300, 2000, 200));
        assertEquals(0, ExportPlan.surplus(2300, 2000, 500));
    }

    @Test
    void oneStackPerFreeSlot() {
        assertEquals(128, ExportPlan.bounded(300, 2, 64));
        assertEquals(300, ExportPlan.bounded(300, 9, 64));
        assertEquals(0, ExportPlan.bounded(300, 0, 64));
        assertEquals(3, ExportPlan.bounded(10, 3, 1));
    }

    @Test
    void absentFillUpToPasses() {
        assertTrue(ExportPlan.validExport(-1, 0));
        assertTrue(ExportPlan.validExport(-1, 2000));
    }

    @Test
    void exportNotOverFillUpToFails() {
        assertFalse(ExportPlan.validExport(500, 400));
        assertFalse(ExportPlan.validExport(500, 500));
        assertTrue(ExportPlan.validExport(500, 501));
    }

    @Test
    void incomingBelowTheThresholdIsStored() {
        assertEquals(0, ExportPlan.direct(1800, 2000, 64));
        assertEquals(0, ExportPlan.direct(1936, 2000, 64));
    }

    @Test
    void incomingAboveTheThresholdGoesDirect() {
        assertEquals(14, ExportPlan.direct(1950, 2000, 64));
        assertEquals(64, ExportPlan.direct(2000, 2000, 64));
        assertEquals(64, ExportPlan.direct(2500, 2000, 64));
        assertEquals(10, ExportPlan.direct(0, 0, 10));
    }

    @Test
    void nothingIncomingNothingDirect() {
        assertEquals(0, ExportPlan.direct(2500, 2000, 0));
    }
}
