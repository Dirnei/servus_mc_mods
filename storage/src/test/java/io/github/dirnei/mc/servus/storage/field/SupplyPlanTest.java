package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SupplyPlanTest {

    @Test
    void lowBelowHalfTheLevel() {
        assertTrue(SupplyPlan.low(31, 64));
        assertTrue(SupplyPlan.low(0, 8));
        assertTrue(SupplyPlan.low(3, 8));
    }

    @Test
    void halfTheLevelIsEnough() {
        assertFalse(SupplyPlan.low(32, 64));
        assertFalse(SupplyPlan.low(4, 8));
        assertFalse(SupplyPlan.low(64, 64));
    }

    @Test
    void bringsAFillToTheLevel() {
        assertEquals(44, SupplyPlan.toBring(20, 64, 500, 0, 1000));
        assertEquals(6, SupplyPlan.toBring(2, 8, 50, 0, 1000));
        assertEquals(0, SupplyPlan.toBring(64, 64, 500, 0, 1000));
    }

    @Test
    void keepsTheReserve() {
        assertEquals(20, SupplyPlan.toBring(0, 64, 30, 10, 1000));
        assertEquals(0, SupplyPlan.toBring(0, 64, 10, 10, 1000));
        assertEquals(3, SupplyPlan.toBring(0, 64, 8, 5, 1000));
    }

    @Test
    void fitsTheCargo() {
        assertEquals(16, SupplyPlan.toBring(0, 64, 500, 0, 16));
        assertEquals(0, SupplyPlan.toBring(0, 64, 500, 0, 0));
    }

    @Test
    void nothingInStock() {
        assertEquals(0, SupplyPlan.toBring(0, 64, 0, 0, 1000));
        assertFalse(SupplyPlan.due(new SupplyPlan.Part(0, 64, 0, 0)));
    }

    @Test
    void levelZeroTurnsAPartOff() {
        assertFalse(SupplyPlan.low(0, 0));
        assertEquals(0, SupplyPlan.toBring(0, 0, 500, 0, 1000));
        assertFalse(SupplyPlan.due(new SupplyPlan.Part(0, 0, 500, 0)));
    }

    @Test
    void dueWhenAnyPartIsLowWithStock() {
        SupplyPlan.Part fullSeeds = new SupplyPlan.Part(40, 64, 100, 0);
        SupplyPlan.Part lowHoney = new SupplyPlan.Part(2, 8, 10, 0);
        SupplyPlan.Part lowFertilizerAtReserve = new SupplyPlan.Part(0, 64, 10, 10);
        assertTrue(SupplyPlan.due(fullSeeds, lowHoney));
        assertFalse(SupplyPlan.due(fullSeeds, lowFertilizerAtReserve));
        assertTrue(SupplyPlan.due(new SupplyPlan.Part(0, 64, 11, 10)));
    }
}
