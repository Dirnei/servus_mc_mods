package io.github.dirnei.mc.servus.foundation.machine.sink;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SinkFillTest {

    private final SinkFill fill = new SinkFill(14, 7, 2);

    @Test
    void startsEmpty() {
        assertFalse(fill.any());
        assertTrue(fill.rowEmpty(0) && fill.rowEmpty(1));
        assertTrue(fill.tankEmpty(0) && fill.tankEmpty(1));
    }

    @Test
    void aFilledSlotCountsForItsRowOnly() {
        fill.slot(8, true);
        assertTrue(fill.any());
        assertTrue(fill.rowEmpty(0));
        assertFalse(fill.rowEmpty(1));
    }

    @Test
    void reportingTheSameStateTwiceCountsOnce() {
        fill.slot(3, true);
        fill.slot(3, true);
        fill.slot(3, false);
        assertFalse(fill.any());
        assertTrue(fill.rowEmpty(0));
    }

    @Test
    void aRowStaysFilledUntilItsLastSlotEmpties() {
        fill.slot(0, true);
        fill.slot(6, true);
        fill.slot(0, false);
        assertFalse(fill.rowEmpty(0));
        fill.slot(6, false);
        assertTrue(fill.rowEmpty(0));
    }

    @Test
    void aFilledTankCounts() {
        fill.tank(1, true);
        assertTrue(fill.any());
        assertFalse(fill.tankEmpty(1));
        assertTrue(fill.tankEmpty(0));
        fill.tank(1, false);
        assertFalse(fill.any());
    }
}
