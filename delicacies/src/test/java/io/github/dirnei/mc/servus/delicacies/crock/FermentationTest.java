package io.github.dirnei.mc.servus.delicacies.crock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FermentationTest {

    @Test
    void inactiveIsNeverDone() {
        Fermentation fermentation = new Fermentation();
        assertFalse(fermentation.active());
        assertFalse(fermentation.done(1_000_000));
        assertEquals(0, fermentation.remaining(5));
    }

    @Test
    void doneExactlyAtTheDuration() {
        Fermentation fermentation = new Fermentation();
        fermentation.start(100, 72000);
        assertFalse(fermentation.done(72099));
        assertTrue(fermentation.done(72100));
    }

    @Test
    void remainingNeverBelowZero() {
        Fermentation fermentation = new Fermentation();
        fermentation.start(0, 50);
        assertEquals(20, fermentation.remaining(30));
        assertEquals(0, fermentation.remaining(500));
    }

    @Test
    void hoursLeftRoundUp() {
        Fermentation fermentation = new Fermentation();
        fermentation.start(0, 2500);
        assertEquals(3, fermentation.hoursLeft(0));
        assertEquals(1, fermentation.hoursLeft(1500));
        assertEquals(1, fermentation.hoursLeft(2499));
        assertEquals(0, fermentation.hoursLeft(2500));
    }

    @Test
    void nextCheckIsCappedAndAtLeastOne() {
        Fermentation fermentation = new Fermentation();
        fermentation.start(0, 1000);
        assertEquals(100, fermentation.nextCheck(0, 100));
        assertEquals(30, fermentation.nextCheck(970, 100));
        assertEquals(1, fermentation.nextCheck(2000, 100));
    }

    @Test
    void timeThatPassedUnseenCounts() {
        Fermentation fermentation = new Fermentation();
        fermentation.start(10, 48000);
        assertTrue(fermentation.done(10 + 48000 + 123456), "a crock found again long after is done at once");
    }

    @Test
    void backdateMovesTheStart() {
        Fermentation fermentation = new Fermentation();
        fermentation.start(1000, 600);
        fermentation.backdate(300);
        assertEquals(700, fermentation.startedAt());
        assertEquals(300, fermentation.remaining(1000));
        fermentation.backdate(300);
        assertTrue(fermentation.done(1000));
    }

    @Test
    void backdateLeavesAnInactiveOneAlone() {
        Fermentation fermentation = new Fermentation();
        fermentation.backdate(300);
        assertFalse(fermentation.active());
    }

    @Test
    void restoreFromSave() {
        Fermentation fermentation = new Fermentation();
        fermentation.restore(400, 200);
        assertTrue(fermentation.active());
        assertEquals(200, fermentation.duration());
        assertTrue(fermentation.done(600));
        fermentation.restore(-1, 0);
        assertFalse(fermentation.active());
    }

    @Test
    void stopEndsIt() {
        Fermentation fermentation = new Fermentation();
        fermentation.start(0, 10);
        fermentation.stop();
        assertFalse(fermentation.active());
        assertFalse(fermentation.done(100));
    }

    @Test
    void daysFromTicks() {
        assertEquals(3.0, Fermentation.days(72000));
        assertEquals(0.5, Fermentation.days(12000));
    }
}
