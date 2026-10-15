package io.github.dirnei.mc.servus.alchemy.homunculus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WhisperClockTest {

    @Test
    void delayStaysInsideTheBounds() {
        assertEquals(WhisperClock.MIN_DELAY, WhisperClock.delay(0));
        assertEquals(WhisperClock.MAX_DELAY, WhisperClock.delay(0.99999999));
        assertEquals(WhisperClock.MAX_DELAY, WhisperClock.delay(5));
        int middle = WhisperClock.delay(0.5);
        assertTrue(middle > WhisperClock.MIN_DELAY && middle < WhisperClock.MAX_DELAY);
    }

    @Test
    void firesOncePerDelay() {
        WhisperClock clock = new WhisperClock();
        int fired = 0;
        int ticks = 0;
        for (int i = 0; i < 3 * (WhisperClock.MIN_DELAY + 1); i++) {
            ticks++;
            if (clock.tick(0)) {
                fired++;
            }
        }
        assertEquals(3, fired, "three whispers in " + ticks + " ticks at the shortest delay");
    }
}
