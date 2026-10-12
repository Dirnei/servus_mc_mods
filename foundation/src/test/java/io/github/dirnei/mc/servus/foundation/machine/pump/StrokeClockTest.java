package io.github.dirnei.mc.servus.foundation.machine.pump;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StrokeClockTest {

    @Test
    void oneSplashPerStrokeWhileWorking() {
        StrokeClock clock = new StrokeClock();
        int bottoms = 0;
        for (int tick = 0; tick < StrokeClock.PERIOD * 3; tick++) {
            bottoms += clock.tick(true);
        }
        assertEquals(3, bottoms);
    }

    @Test
    void standsStillWhileIdle() {
        StrokeClock clock = new StrokeClock();
        clock.tick(true);
        for (int tick = 0; tick < 100; tick++) {
            assertEquals(0, clock.tick(false));
        }
        assertEquals(1, clock.phase(0.5F, false), 1e-9);
        assertEquals(1.5, clock.phase(0.5F, true), 1e-9);
    }
}
