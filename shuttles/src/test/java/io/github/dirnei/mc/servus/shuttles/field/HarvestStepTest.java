package io.github.dirnei.mc.servus.shuttles.field;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HarvestStepTest {

    @Test
    void headGone() {
        assertEquals(HarvestStep.PUT_AWAY, HarvestStep.next(false, false, 0, true, true, true));
        assertEquals(HarvestStep.HOME, HarvestStep.next(false, false, 5, true, true, false));
    }

    @Test
    void fullCargoGoesToStorage() {
        assertEquals(HarvestStep.PUT_AWAY, HarvestStep.next(true, true, 0, true, true, true));
    }

    @Test
    void seedsBeforeWork() {
        assertEquals(HarvestStep.FETCH_SEEDS, HarvestStep.next(true, false, 10, true, true, false));
        assertEquals(HarvestStep.PUT_AWAY, HarvestStep.next(true, false, 10, false, true, true));
        assertEquals(HarvestStep.WORK, HarvestStep.next(true, false, 10, false, true, false));
    }

    @Test
    void workWhileWorkIsLeft() {
        assertEquals(HarvestStep.WORK, HarvestStep.next(true, false, 0, true, true, true));
    }

    @Test
    void doneGoesToStorageThenHome() {
        assertEquals(HarvestStep.PUT_AWAY, HarvestStep.next(true, false, 0, true, false, true));
        assertEquals(HarvestStep.HOME, HarvestStep.next(true, false, 0, true, false, false));
    }
}
