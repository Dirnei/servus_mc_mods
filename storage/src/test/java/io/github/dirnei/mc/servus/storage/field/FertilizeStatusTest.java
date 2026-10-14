package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FertilizeStatusTest {

    @Test
    void reasonsInOrder() {
        assertEquals(FertilizeStatus.OFF, FertilizeStatus.of(false, false, false, false, false, 0, 0, 0, false));
        assertEquals(FertilizeStatus.NOT_LINKED, FertilizeStatus.of(true, false, false, false, false, 0, 0, 0, false));
        assertEquals(FertilizeStatus.NO_SPREADER, FertilizeStatus.of(true, true, false, false, false, 0, 0, 0, false));
        assertEquals(FertilizeStatus.NO_WAY, FertilizeStatus.of(true, true, true, false, false, 0, 0, 0, false));
        assertEquals(FertilizeStatus.WORKING, FertilizeStatus.of(true, true, true, true, true, 0, 0, 0, false));
        assertEquals(FertilizeStatus.WAITING, FertilizeStatus.of(true, true, true, true, false, 5, 0, 64, false));
        assertEquals(FertilizeStatus.NO_STOCK, FertilizeStatus.of(true, true, true, true, false, 0, 0, 64, true));
        assertEquals(FertilizeStatus.BELOW_RESERVE, FertilizeStatus.of(true, true, true, true, false, 0, 64, 64, true));
        assertEquals(FertilizeStatus.NO_SUPPLY_SHUTTLE, FertilizeStatus.of(true, true, true, true, false, 0, 65, 64, false));
        assertEquals(FertilizeStatus.SUPPLY_COMING, FertilizeStatus.of(true, true, true, true, false, 0, 65, 64, true));
        assertEquals(FertilizeStatus.SUPPLY_COMING, FertilizeStatus.of(true, true, true, true, false, 0, 1, 0, true));
    }

    @Test
    void bufferedFertilizerWaitsWhateverTheStock() {
        assertEquals(FertilizeStatus.WAITING, FertilizeStatus.of(true, true, true, true, false, 1, 0, 0, false));
    }

    @Test
    void problems() {
        assertTrue(FertilizeStatus.NO_SPREADER.problem());
        assertTrue(FertilizeStatus.NO_WAY.problem());
        assertTrue(FertilizeStatus.NO_SUPPLY_SHUTTLE.problem());
        assertFalse(FertilizeStatus.NO_STOCK.problem());
        assertFalse(FertilizeStatus.BELOW_RESERVE.problem());
        assertFalse(FertilizeStatus.SUPPLY_COMING.problem());
        assertFalse(FertilizeStatus.OFF.problem());
        assertFalse(FertilizeStatus.WORKING.problem());
        assertFalse(FertilizeStatus.WAITING.problem());
        assertFalse(FertilizeStatus.NOT_LINKED.problem());
    }

    @Test
    void notNeededReplacesWaitingButNotProblems() {
        assertEquals(FertilizeStatus.NOT_NEEDED, FertilizeStatus.WAITING.whileNeeded(false));
        assertEquals(FertilizeStatus.NOT_NEEDED, FertilizeStatus.SUPPLY_COMING.whileNeeded(false));
        assertEquals(FertilizeStatus.WAITING, FertilizeStatus.WAITING.whileNeeded(true));
        assertEquals(FertilizeStatus.NO_SPREADER, FertilizeStatus.NO_SPREADER.whileNeeded(false));
        assertEquals(FertilizeStatus.WORKING, FertilizeStatus.WORKING.whileNeeded(false));
        assertEquals(FertilizeStatus.OFF, FertilizeStatus.OFF.whileNeeded(false));
        assertFalse(FertilizeStatus.NOT_NEEDED.problem());
    }
}
