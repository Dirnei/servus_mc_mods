package io.github.dirnei.mc.servus.alchemy.calciner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EternalFireSpeedTest {

    @Test
    void halvesTheTime() {
        assertEquals(40, EternalFireSpeed.cycleTicks(80, 2));
    }

    @Test
    void roundsDown() {
        assertEquals(40, EternalFireSpeed.cycleTicks(81, 2));
    }

    @Test
    void neverBelowOneTick() {
        assertEquals(1, EternalFireSpeed.cycleTicks(1, 4));
    }

    @Test
    void speedOneKeepsTheTime() {
        assertEquals(80, EternalFireSpeed.cycleTicks(80, 1));
    }

    @Test
    void speedBelowOneCountsAsOne() {
        assertEquals(80, EternalFireSpeed.cycleTicks(80, 0));
    }
}
