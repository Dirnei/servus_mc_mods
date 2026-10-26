package io.github.dirnei.mc.servus.shuttles.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TopUpTest {

    @Test
    void surplusJoinsAJobWithRoom() {
        assertEquals(8, TopUp.join(8, 8, 64));
    }

    @Test
    void fullJobTakesNothing() {
        assertEquals(0, TopUp.join(64, 8, 64));
    }

    @Test
    void surplusBiggerThanTheRoom() {
        assertEquals(4, TopUp.join(60, 8, 64));
    }

    @Test
    void smallStackSize() {
        assertEquals(6, TopUp.join(10, 20, 16));
        assertEquals(0, TopUp.join(16, 1, 16));
    }

    @Test
    void noSurplusJoinsNothing() {
        assertEquals(0, TopUp.join(0, 0, 64));
    }
}
