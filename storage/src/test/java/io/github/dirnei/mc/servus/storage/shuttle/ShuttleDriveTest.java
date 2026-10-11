package io.github.dirnei.mc.servus.storage.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShuttleDriveTest {

    private static final double TOP = 0.3;
    private static final double ACCEL = 0.03;

    @Test
    void speedsUpToTopSpeedInTheGivenTicks() {
        double v = 0;
        for (int t = 0; t < 10; t++) {
            v = ShuttleDrive.next(v, TOP, ACCEL, 100);
        }
        assertEquals(TOP, v, 1e-9);
        assertEquals(TOP, ShuttleDrive.next(v, TOP, ACCEL, 100), 1e-9);
    }

    @Test
    void firstTickIsOneStep() {
        assertEquals(0.03, ShuttleDrive.next(0, TOP, ACCEL, 100), 1e-9);
    }

    @Test
    void brakesAndStopsExactlyAtTheStop() {
        double left = 10;
        double v = TOP;
        int ticks = 0;
        while (left > 0 && ticks < 1000) {
            v = ShuttleDrive.next(v, TOP, ACCEL, left);
            assertTrue(v <= left + 1e-12, "never past the stop");
            left -= v;
            ticks++;
        }
        assertEquals(0, left, 1e-9);
        assertTrue(v <= ACCEL + 1e-9, "slow when it gets there, was " + v);
    }

    @Test
    void noTopSpeedMeansStandingStill() {
        assertEquals(0, ShuttleDrive.next(0.2, 0, ACCEL, 10));
        assertEquals(0, ShuttleDrive.next(0.2, TOP, ACCEL, 0));
    }

    @Test
    void stopIsTheEndOrTheFirstVerticalStep() {
        assertEquals(4.5, ShuttleDrive.toStop(6, 0, 0.5, i -> false), 1e-9);
        assertEquals(2.75, ShuttleDrive.toStop(8, 0, 0.25, i -> i == 3), 1e-9);
        assertEquals(0, ShuttleDrive.toStop(8, 3, 0, i -> i == 3), 1e-9);
    }

    @Test
    void glideNeverEntersAVerticalStep() {
        double[] at = ShuttleDrive.glide(8, 0, 0, TOP, TOP, ACCEL, 200, i -> i == 3);
        assertEquals(3, at[0], 1e-9);
        assertEquals(0, at[1], 1e-9);
    }

    @Test
    void glideMatchesTickByTickDriving() {
        double v = 0;
        double travelled = 0;
        for (int t = 0; t < 7; t++) {
            v = ShuttleDrive.next(v, TOP, ACCEL, 20 - travelled);
            travelled += v;
        }
        double[] at = ShuttleDrive.glide(21, 0, 0, 0, TOP, ACCEL, 7, i -> false);
        assertEquals(travelled, at[0] + at[1], 1e-9);
        assertEquals(v, at[2], 1e-9);
    }
}
