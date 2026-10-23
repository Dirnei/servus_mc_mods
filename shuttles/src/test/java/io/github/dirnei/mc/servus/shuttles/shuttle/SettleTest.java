package io.github.dirnei.mc.servus.shuttles.shuttle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SettleTest {

    @Test
    void countsOnlyWhileReady() {
        Settle settle = new Settle(3);
        assertFalse(settle.tick(true));
        assertFalse(settle.tick(true));
        assertTrue(settle.tick(true));
    }

    @Test
    void startsOverWhenNotReady() {
        Settle settle = new Settle(2);
        assertFalse(settle.tick(true));
        assertFalse(settle.tick(false));
        assertFalse(settle.tick(true));
        assertTrue(settle.tick(true));
    }

    @Test
    void staysDone() {
        Settle settle = new Settle(1);
        assertTrue(settle.tick(true));
        assertTrue(settle.tick(true));
    }
}
