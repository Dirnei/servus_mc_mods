package io.github.dirnei.mc.servus.shuttles.handheld;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SweepTrackerTest {

    private final SweepTracker<String, Integer> tracker = new SweepTracker<>(10);

    @Test
    void indexRisesAcrossBays() {
        assertEquals(0, tracker.scan("alex", 1, 0));
        assertEquals(1, tracker.scan("alex", 2, 4));
        assertEquals(2, tracker.scan("alex", 3, 8));
    }

    @Test
    void repeatOnTheSameBayIsSkipped() {
        assertEquals(0, tracker.scan("alex", 1, 0));
        assertEquals(SweepTracker.SKIP, tracker.scan("alex", 1, 4));
        assertEquals(SweepTracker.SKIP, tracker.scan("alex", 1, 8));
        assertEquals(1, tracker.scan("alex", 2, 12));
    }

    @Test
    void quietGapStartsANewSweep() {
        tracker.scan("alex", 1, 0);
        tracker.scan("alex", 2, 4);
        assertEquals(0, tracker.scan("alex", 2, 30));
        assertEquals(0, tracker.scan("alex", 3, 50));
    }

    @Test
    void twoBaysOfOneShelfShareTheKeyAndAreSkipped() {
        assertEquals(0, tracker.scan("alex", 10, 0));
        assertEquals(SweepTracker.SKIP, tracker.scan("alex", 10, 4));
        assertEquals(1, tracker.scan("alex", 20, 8));
        assertEquals(2, tracker.scan("alex", 30, 12));
    }

    @Test
    void playersSweepApart() {
        tracker.scan("alex", 1, 0);
        assertEquals(0, tracker.scan("sam", 1, 1));
        assertEquals(1, tracker.scan("alex", 2, 2));
    }
}
