package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FieldWorkTest {

    private static FieldWork plant(boolean hydrated, int tillable, int plantable, int planted, boolean seeds) {
        return FieldWork.of(true, true, true, true, false, false, true, hydrated, tillable, plantable, planted, seeds);
    }

    @Test
    void firstMissingPieceWins() {
        assertEquals(FieldWork.NOT_LINKED, FieldWork.of(false, false, false, false, false, false, false, false, 0, 0, 0, false));
        assertEquals(FieldWork.NO_CONTROLLER, FieldWork.of(true, false, false, false, false, false, false, false, 0, 0, 0, false));
        assertEquals(FieldWork.NO_CROP, FieldWork.of(true, true, false, true, false, false, false, true, 5, 0, 0, true));
        assertEquals(FieldWork.NO_HARVESTER, FieldWork.of(true, true, true, false, false, false, true, true, 5, 0, 0, true));
    }

    @Test
    void runUnderWayShowsWorking() {
        assertEquals(FieldWork.WORKING, FieldWork.of(true, true, true, true, true, true, true, false, 0, 0, 0, false));
    }

    @Test
    void harvestDueBeforePlanting() {
        assertEquals(FieldWork.HARVEST_DUE, FieldWork.of(true, true, true, true, false, true, true, false, 0, 0, 4, false));
    }

    @Test
    void harvestOnlyWaits() {
        assertEquals(FieldWork.WAITING_RIPE, FieldWork.of(true, true, true, true, false, false, false, false, 3, 3, 2, false));
    }

    @Test
    void plantingNeedsWater() {
        assertEquals(FieldWork.DRY, plant(false, 3, 0, 0, true));
    }

    @Test
    void tillingNeedsNoSeeds() {
        assertEquals(FieldWork.READY, plant(true, 3, 0, 0, false));
    }

    @Test
    void farmlandWithoutSeeds() {
        assertEquals(FieldWork.NO_SEEDS, plant(true, 0, 3, 0, false));
        assertEquals(FieldWork.READY, plant(true, 0, 3, 0, true));
    }

    @Test
    void nothingLeftToDo() {
        assertEquals(FieldWork.GROWING, plant(true, 0, 0, 6, true));
        assertEquals(FieldWork.NO_PLOTS, plant(true, 0, 0, 0, true));
    }

    @Test
    void unknownIdFallsBack() {
        assertEquals(FieldWork.NOT_LINKED, FieldWork.byId(99));
    }
}
