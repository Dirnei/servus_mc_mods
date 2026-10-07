package io.github.dirnei.mc.servus.manufacturing.machine.extractor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FuelReserveTest {

    private static final int TARGET = 4;

    @Test
    void emptySlotWantsTheYield() {
        assertTrue(FuelReserve.wantsYield(TARGET, true, true, 0));
    }

    @Test
    void slotBelowTargetWantsTheYield() {
        assertTrue(FuelReserve.wantsYield(TARGET, true, true, 3));
    }

    @Test
    void slotAtTargetDoesNotWantTheYield() {
        assertFalse(FuelReserve.wantsYield(TARGET, true, true, 4));
    }

    @Test
    void fullStackDoesNotWantTheYield() {
        assertFalse(FuelReserve.wantsYield(TARGET, true, true, 64));
    }

    @Test
    void differentItemInTheSlotDoesNotWantTheYield() {
        assertFalse(FuelReserve.wantsYield(TARGET, true, false, 1));
    }

    @Test
    void nonFuelYieldIsNeverWanted() {
        assertFalse(FuelReserve.wantsYield(TARGET, false, true, 0));
    }

    @Test
    void targetZeroDisablesTheReserve() {
        assertFalse(FuelReserve.wantsYield(0, true, true, 0));
    }
}
