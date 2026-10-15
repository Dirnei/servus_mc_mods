package io.github.dirnei.mc.servus.shuttles.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StepPowerTest {

    @Test
    void oneBlockCostsPerBlock() {
        assertEquals(20, StepPower.segmentCost(false, 1, 20));
        assertEquals(20, StepPower.segmentCost(false, 7, 20));
    }

    @Test
    void rideOfFourCostsFourBlocks() {
        assertEquals(80, StepPower.segmentCost(true, 4, 20));
        assertEquals(0, StepPower.segmentCost(true, 0, 20));
    }

    @Test
    void freeWhenNoCost() {
        assertEquals(0, StepPower.segmentCost(false, 1, 0));
    }

    @Test
    void unpaidGateRefuses() {
        assertFalse(StepPower.mayStart(false));
        assertTrue(StepPower.mayStart(true));
    }

    @Test
    void unpaidTickKeepsProgress() {
        assertEquals(30, StepPower.advance(30, false));
        assertEquals(31, StepPower.advance(30, true));
    }
}
