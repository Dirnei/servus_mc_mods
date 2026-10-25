package io.github.dirnei.mc.servus.shuttles.field;

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

    @Test
    void pausedHarvestShowsInsteadOfWaiting() {
        assertEquals(FieldWork.HARVEST_PAUSED, FieldWork.GROWING.whilePaused(true));
        assertEquals(FieldWork.HARVEST_PAUSED, FieldWork.WAITING_RIPE.whilePaused(true));
        assertEquals(FieldWork.GROWING, FieldWork.GROWING.whilePaused(false));
        assertEquals(FieldWork.READY, FieldWork.READY.whilePaused(true));
        assertEquals(FieldWork.DRY, FieldWork.DRY.whilePaused(true));
        assertFalse(FieldWork.HARVEST_PAUSED.problem());
    }

    @Test
    void collectNeedsItems() {
        assertEquals(FieldWork.Collect.FINE, FieldWork.Collect.of(false, false, true, false));
        assertEquals(FieldWork.Collect.NO_SHUTTLE, FieldWork.Collect.of(true, false, false, true));
        assertEquals(FieldWork.Collect.NO_ROOM, FieldWork.Collect.of(true, true, true, true));
        assertEquals(FieldWork.Collect.FULL, FieldWork.Collect.of(true, true, false, false));
        assertEquals(FieldWork.Collect.FINE, FieldWork.Collect.of(true, true, false, true));
    }

    @Test
    void waitingBufferOverridesOnlyAWorkableField() {
        assertEquals(FieldWork.COLLECT_NO_SHUTTLE, FieldWork.WORKING.whileCollecting(FieldWork.Collect.NO_SHUTTLE));
        assertEquals(FieldWork.TO_STORAGE_FULL, FieldWork.HARVEST_DUE.whileCollecting(FieldWork.Collect.FULL));
        assertEquals(FieldWork.COLLECT_NO_ROOM, FieldWork.GROWING.whileCollecting(FieldWork.Collect.NO_ROOM));
        assertEquals(FieldWork.NO_HARVESTER, FieldWork.NO_HARVESTER.whileCollecting(FieldWork.Collect.FULL));
        assertEquals(FieldWork.READY, FieldWork.READY.whileCollecting(FieldWork.Collect.FINE));
        assertTrue(FieldWork.TO_STORAGE_FULL.problem());
    }

    private static FieldWork trees(boolean lumber, boolean reachable, boolean needsRange, boolean tooLarge, boolean hydrated, int plantable) {
        return FieldWork.of(FieldKind.TREES, true, true, true, lumber, reachable, needsRange, tooLarge, false, false, true, hydrated, 0,
                plantable, 0, FieldWork.Seeds.BUFFERED);
    }

    @Test
    void treesAskForALumberShuttle() {
        assertEquals(FieldWork.NO_LUMBER, trees(false, false, true, true, true, 1));
        assertEquals(FieldWork.NO_LUMBER_WAY, trees(true, false, true, true, true, 1));
        assertTrue(FieldWork.NO_LUMBER.problem());
        assertTrue(FieldWork.NO_LUMBER_WAY.problem());
    }

    @Test
    void rangeAndSizeComeAfterTheWay() {
        assertEquals(FieldWork.NEEDS_RANGE, trees(true, true, true, true, true, 1));
        assertEquals(FieldWork.TREE_TOO_LARGE, trees(true, true, false, true, true, 1));
        assertTrue(FieldWork.NEEDS_RANGE.problem());
        assertFalse(FieldWork.TREE_TOO_LARGE.problem());
    }

    @Test
    void treesAreNeverDry() {
        assertEquals(FieldWork.READY, trees(true, true, false, false, false, 2));
        assertEquals(FieldWork.DRY, FieldWork.of(FieldKind.CROPS, true, true, true, true, true, true, true, false, false, true, false, 0, 2, 0,
                FieldWork.Seeds.BUFFERED), "crops ignore the tree checks and still need water");
    }
}
