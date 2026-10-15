package io.github.dirnei.mc.servus.shuttles.shuttle.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShuttleLookTest {

    @Test
    void speedSteps() {
        int[] counts = {0, 1, 2, 3, 5, 6, 8};
        int[] steps = {0, 1, 1, 2, 2, 3, 3};
        for (int i = 0; i < counts.length; i++) {
            assertEquals(steps[i], ShuttleLook.speedStep(counts[i]), "Speed " + counts[i]);
        }
    }

    @Test
    void handlingSteps() {
        int[] counts = {0, 1, 3, 4, 8};
        int[] steps = {0, 1, 1, 2, 2};
        for (int i = 0; i < counts.length; i++) {
            assertEquals(steps[i], ShuttleLook.handlingStep(counts[i]), "Handling " + counts[i]);
        }
    }

    @Test
    void negativeCountsShowNothing() {
        assertEquals(0, ShuttleLook.speedStep(-2));
        assertEquals(0, ShuttleLook.handlingStep(-1));
    }

    @Test
    void onlyTheMk1HoldIsOpen() {
        assertTrue(ShuttleLook.openHold(1));
        assertFalse(ShuttleLook.openHold(2));
        assertFalse(ShuttleLook.openHold(3));
    }

    @Test
    void keysInOneStepAreEqual() {
        assertEquals(ShuttleLook.Key.of(3, 4, 1, ""), ShuttleLook.Key.of(3, 5, 3, ""));
        assertNotEquals(ShuttleLook.Key.of(3, 5, 0, ""), ShuttleLook.Key.of(3, 6, 0, ""));
        assertNotEquals(ShuttleLook.Key.of(1, 0, 0, ""), ShuttleLook.Key.of(1, 0, 0, "harvester"));
    }
}
