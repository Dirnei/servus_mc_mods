package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.storage.field.StockLevels.Level;
import io.github.dirnei.mc.servus.storage.field.StockLevels.Levels;
import org.junit.jupiter.api.Test;

class StockLevelsTest {

    @Test
    void zeroMeansOff() {
        assertFalse(StockLevels.harvestPaused(true, 100_000, 0));
        assertTrue(StockLevels.fertilizeAllowed(true, 100_000, 0, 0));
    }

    @Test
    void harvestPausesAtTheLimit() {
        assertTrue(StockLevels.harvestPaused(true, 1000, 1000));
        assertTrue(StockLevels.harvestPaused(true, 1024, 1000));
    }

    @Test
    void harvestGoesOnBelowTheLimit() {
        assertFalse(StockLevels.harvestPaused(true, 999, 1000));
    }

    @Test
    void fertilizeOnlyBelowTheLevel() {
        assertFalse(StockLevels.fertilizeAllowed(true, 640, 500, 0));
        assertFalse(StockLevels.fertilizeAllowed(true, 500, 500, 0));
        assertTrue(StockLevels.fertilizeAllowed(true, 400, 500, 0));
    }

    @Test
    void noFertilizingWhileHarvestIsPaused() {
        assertFalse(StockLevels.fertilizeAllowed(true, 1024, 0, 1000));
        assertFalse(StockLevels.fertilizeAllowed(true, 1000, 1000, 1000));
        assertTrue(StockLevels.fertilizeAllowed(true, 400, 500, 1000));
    }

    @Test
    void unknownProduceDoesNothing() {
        assertFalse(StockLevels.harvestPaused(false, 1024, 1000));
        assertTrue(StockLevels.fertilizeAllowed(false, 1024, 500, 1000));
    }

    @Test
    void valuesAreClamped() {
        assertEquals(0, StockLevels.clamp(-54));
        assertEquals(StockLevels.MAX, StockLevels.clamp(StockLevels.MAX + 639L));
        assertEquals(640, StockLevels.clamp(640));
    }

    @Test
    void raisingTheLevelPastTheLimitRaisesTheLimit() {
        assertEquals(new Levels(640, 640), StockLevels.keepOrder(512, 640, Level.FERTILIZE));
    }

    @Test
    void loweringTheLimitPastTheLevelLowersTheLevel() {
        assertEquals(new Levels(300, 300), StockLevels.keepOrder(300, 500, Level.HARVEST));
    }

    @Test
    void levelsInOrderOrOffStay() {
        assertEquals(new Levels(1000, 500), StockLevels.keepOrder(1000, 500, Level.HARVEST));
        assertEquals(new Levels(0, 500), StockLevels.keepOrder(0, 500, Level.FERTILIZE));
        assertEquals(new Levels(300, 0), StockLevels.keepOrder(300, 0, Level.HARVEST));
    }
}
