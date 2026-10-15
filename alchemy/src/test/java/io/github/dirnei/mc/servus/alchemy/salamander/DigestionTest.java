package io.github.dirnei.mc.servus.alchemy.salamander;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DigestionTest {

    @Test
    void idleIsNotBusy() {
        Digestion digestion = new Digestion();
        assertFalse(digestion.busy());
        assertFalse(digestion.tick());
        assertEquals(0, digestion.remaining());
    }

    @Test
    void finishesAfterTicks() {
        Digestion digestion = new Digestion();
        digestion.start(3);
        assertTrue(digestion.busy());
        assertFalse(digestion.tick());
        assertFalse(digestion.tick());
        assertTrue(digestion.tick());
        assertFalse(digestion.busy());
    }

    @Test
    void finishesOnlyOnce() {
        Digestion digestion = new Digestion();
        digestion.start(1);
        assertTrue(digestion.tick());
        assertFalse(digestion.tick());
        assertFalse(digestion.tick());
    }

    @Test
    void restoredRemainingCounts() {
        Digestion digestion = new Digestion();
        digestion.restore(2);
        assertTrue(digestion.busy());
        assertFalse(digestion.tick());
        assertTrue(digestion.tick());
        digestion.restore(-5);
        assertFalse(digestion.busy());
    }

    @Test
    void rejectsStartBelowOneTick() {
        assertThrows(IllegalArgumentException.class, () -> new Digestion().start(0));
    }
}
