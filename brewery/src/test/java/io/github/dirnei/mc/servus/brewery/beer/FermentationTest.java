package io.github.dirnei.mc.servus.brewery.beer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FermentationTest {

    private static final long START = 5000;
    private static final long TIME = 36000;

    @Test
    void notDoneBeforeItsTime() {
        Fermentation batch = Fermentation.start(START);
        assertFalse(batch.isDone(START + TIME - 1, TIME));
        assertEquals(1, batch.remaining(START + TIME - 1, TIME));
    }

    @Test
    void doneAtAndAfterItsTime() {
        Fermentation batch = Fermentation.start(START);
        assertTrue(batch.isDone(START + TIME, TIME));
        assertTrue(batch.isDone(START + TIME * 5, TIME));
    }

    @Test
    void remainingCountsDownAndStopsAtZero() {
        Fermentation batch = Fermentation.start(START);
        assertEquals(TIME, batch.remaining(START, TIME));
        assertEquals(TIME - 100, batch.remaining(START + 100, TIME));
        assertEquals(0, batch.remaining(START + TIME * 2, TIME));
    }

    @Test
    void percentAtStartHalfAndEnd() {
        Fermentation batch = Fermentation.start(START);
        assertEquals(0, batch.percent(START, TIME));
        assertEquals(50, batch.percent(START + TIME / 2, TIME));
        assertEquals(100, batch.percent(START + TIME, TIME));
        assertEquals(100, batch.percent(START + TIME * 3, TIME));
    }

    @Test
    void zeroTimeIsDoneAtOnce() {
        Fermentation batch = Fermentation.start(START);
        assertTrue(batch.isDone(START, 0));
        assertEquals(100, batch.percent(START, 0));
    }

    @Test
    void gameTimeBeforeTheStartCountsAsNothing() {
        Fermentation batch = Fermentation.start(START);
        assertEquals(0, batch.elapsed(START - 1000));
        assertEquals(TIME, batch.remaining(START - 1000, TIME));
        assertEquals(0, batch.percent(START - 1000, TIME));
    }
}
