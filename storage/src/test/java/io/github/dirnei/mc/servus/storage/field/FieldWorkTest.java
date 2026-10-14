package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FieldWorkTest {

    private static FieldWork plant(boolean hydrated, int tillable, int plantable, int planted, FieldWork.Seeds seeds) {
        return FieldWork.of(true, true, true, true, true, false, false, true, hydrated, tillable, plantable, planted, seeds);
    }

    @Test
    void firstMissingPieceWins() {
        assertEquals(FieldWork.NOT_LINKED, FieldWork.of(false, false, false, false, true, false, false, false, false, 0, 0, 0, FieldWork.Seeds.NONE));
        assertEquals(FieldWork.NO_CONTROLLER, FieldWork.of(true, false, false, false, true, false, false, false, false, 0, 0, 0, FieldWork.Seeds.NONE));
        assertEquals(FieldWork.NO_CROP, FieldWork.of(true, true, false, true, true, false, false, false, true, 5, 0, 0, FieldWork.Seeds.BUFFERED));
        assertEquals(FieldWork.NO_HARVESTER, FieldWork.of(true, true, true, false, true, false, false, true, true, 5, 0, 0, FieldWork.Seeds.BUFFERED));
    }

    @Test
    void runUnderWayShowsWorking() {
        assertEquals(FieldWork.WORKING, FieldWork.of(true, true, true, true, true, true, true, true, false, 0, 0, 0, FieldWork.Seeds.NONE));
    }

    @Test
    void harvestDueBeforePlanting() {
        assertEquals(FieldWork.HARVEST_DUE, FieldWork.of(true, true, true, true, true, false, true, true, false, 0, 0, 4, FieldWork.Seeds.NONE));
    }

    @Test
    void harvestOnlyWaits() {
        assertEquals(FieldWork.WAITING_RIPE, FieldWork.of(true, true, true, true, true, false, false, false, false, 3, 3, 2, FieldWork.Seeds.NONE));
    }

    @Test
    void plantingNeedsWater() {
        assertEquals(FieldWork.DRY, plant(false, 3, 0, 0, FieldWork.Seeds.BUFFERED));
    }

    @Test
    void tillingNeedsNoSeeds() {
        assertEquals(FieldWork.READY, plant(true, 3, 0, 0, FieldWork.Seeds.NONE));
    }

    @Test
    void farmlandWithoutSeeds() {
        assertEquals(FieldWork.NO_SEEDS, plant(true, 0, 3, 0, FieldWork.Seeds.NONE));
        assertEquals(FieldWork.READY, plant(true, 0, 3, 0, FieldWork.Seeds.BUFFERED));
    }

    @Test
    void farmlandWaitsForASupplyRun() {
        assertEquals(FieldWork.SEEDS_COMING, plant(true, 0, 3, 0, FieldWork.Seeds.COMING));
        assertEquals(FieldWork.NO_SUPPLY_SHUTTLE, plant(true, 0, 3, 0, FieldWork.Seeds.NO_SHUTTLE));
        assertFalse(FieldWork.SEEDS_COMING.problem());
        assertTrue(FieldWork.NO_SUPPLY_SHUTTLE.problem());
        assertTrue(FieldWork.NO_SEEDS.problem());
    }

    @Test
    void seedsComeFromTheBufferFirst() {
        assertEquals(FieldWork.Seeds.BUFFERED, FieldWork.Seeds.of(true, false, false));
        assertEquals(FieldWork.Seeds.NONE, FieldWork.Seeds.of(false, false, true));
        assertEquals(FieldWork.Seeds.COMING, FieldWork.Seeds.of(false, true, true));
        assertEquals(FieldWork.Seeds.NO_SHUTTLE, FieldWork.Seeds.of(false, true, false));
    }

    @Test
    void nothingLeftToDo() {
        assertEquals(FieldWork.GROWING, plant(true, 0, 0, 6, FieldWork.Seeds.BUFFERED));
        assertEquals(FieldWork.NO_PLOTS, plant(true, 0, 0, 0, FieldWork.Seeds.BUFFERED));
    }

    @Test
    void unknownIdFallsBack() {
        assertEquals(FieldWork.NOT_LINKED, FieldWork.byId(99));
    }

    @Test
    void noWayComesAfterTheHarvester() {
        assertEquals(FieldWork.NO_WAY, FieldWork.of(true, true, true, true, false, true, true, true, true, 5, 0, 0, FieldWork.Seeds.BUFFERED));
        assertEquals(FieldWork.NO_HARVESTER, FieldWork.of(true, true, true, false, false, false, true, true, true, 5, 0, 0, FieldWork.Seeds.BUFFERED));
    }
}
