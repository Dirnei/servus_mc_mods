package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FertilizeStatusTest {

    @Test
    void reasonsInOrder() {
        assertEquals(FertilizeStatus.OFF, FertilizeStatus.of(false, false, false, false, false, 0, 0));
        assertEquals(FertilizeStatus.NOT_LINKED, FertilizeStatus.of(true, false, false, false, false, 0, 0));
        assertEquals(FertilizeStatus.NO_SPREADER, FertilizeStatus.of(true, true, false, false, false, 0, 0));
        assertEquals(FertilizeStatus.NO_WAY, FertilizeStatus.of(true, true, true, false, false, 0, 0));
        assertEquals(FertilizeStatus.WORKING, FertilizeStatus.of(true, true, true, true, true, 0, 0));
        assertEquals(FertilizeStatus.NO_STOCK, FertilizeStatus.of(true, true, true, true, false, 0, 64));
        assertEquals(FertilizeStatus.BELOW_RESERVE, FertilizeStatus.of(true, true, true, true, false, 64, 64));
        assertEquals(FertilizeStatus.WAITING, FertilizeStatus.of(true, true, true, true, false, 65, 64));
        assertEquals(FertilizeStatus.WAITING, FertilizeStatus.of(true, true, true, true, false, 1, 0));
    }

    @Test
    void problems() {
        assertTrue(FertilizeStatus.NO_SPREADER.problem());
        assertTrue(FertilizeStatus.NO_WAY.problem());
        assertTrue(FertilizeStatus.NO_STOCK.problem());
        assertTrue(FertilizeStatus.BELOW_RESERVE.problem());
        assertFalse(FertilizeStatus.OFF.problem());
        assertFalse(FertilizeStatus.WORKING.problem());
        assertFalse(FertilizeStatus.WAITING.problem());
        assertFalse(FertilizeStatus.NOT_LINKED.problem());
    }
}
