package io.github.dirnei.mc.servus.foundation.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RestartGateTest {

    private static final long CAPACITY = 16000;

    @Test
    void runsUntilFullThenStopsAndWaits() {
        RestartGate gate = new RestartGate();

        assertTrue(gate.allows(false, 0, CAPACITY, 0.5));
        assertTrue(gate.allows(false, 15960, CAPACITY, 0.5));
        assertFalse(gate.allows(true, CAPACITY, CAPACITY, 0.5));
        assertTrue(gate.waiting());
    }

    @Test
    void waitsWhileAtOrAboveTheThreshold() {
        RestartGate gate = new RestartGate();
        gate.allows(true, CAPACITY, CAPACITY, 0.5);

        assertFalse(gate.allows(false, 15995, CAPACITY, 0.5));
        assertFalse(gate.allows(false, 9600, CAPACITY, 0.5));
        assertFalse(gate.allows(false, 8000, CAPACITY, 0.5));
        assertTrue(gate.waiting());
    }

    @Test
    void restartsBelowTheThresholdAndRunsUntilFull() {
        RestartGate gate = new RestartGate();
        gate.allows(true, CAPACITY, CAPACITY, 0.5);

        assertTrue(gate.allows(false, 7999, CAPACITY, 0.5));
        assertFalse(gate.waiting());
        assertTrue(gate.allows(false, 12000, CAPACITY, 0.5));
        assertTrue(gate.allows(false, 15999, CAPACITY, 0.5));
        assertFalse(gate.allows(true, CAPACITY, CAPACITY, 0.5));
    }

    @Test
    void thresholdOneRunsWheneverNotFull() {
        RestartGate gate = new RestartGate();
        gate.allows(true, CAPACITY, CAPACITY, 1.0);

        assertTrue(gate.allows(false, 15995, CAPACITY, 1.0));
    }

    @Test
    void levelIsTheShareOfTheCapacityRoundedUp() {
        assertEquals(8000, RestartGate.level(CAPACITY, 0.5));
        assertEquals(2000, RestartGate.level(4000, 0.5));
        assertEquals(1334, RestartGate.level(4000, 1.0 / 3.0));
        assertEquals(CAPACITY, RestartGate.level(CAPACITY, 1.0));
        assertEquals(0, RestartGate.level(CAPACITY, 0.0));
    }

    @Test
    void restartsExactlyBelowTheLevel() {
        RestartGate gate = new RestartGate();
        gate.allows(true, 4000, 4000, 1.0 / 3.0);

        assertFalse(gate.allows(false, 1334, 4000, 1.0 / 3.0));
        assertTrue(gate.allows(false, 1333, 4000, 1.0 / 3.0));
    }

    @Test
    void waitingStateCanBeRestored() {
        RestartGate gate = new RestartGate();
        gate.setWaiting(true);

        assertTrue(gate.waiting());
        assertFalse(gate.allows(false, 9000, CAPACITY, 0.5));
        gate.setWaiting(false);
        assertTrue(gate.allows(false, 9000, CAPACITY, 0.5));
    }
}
