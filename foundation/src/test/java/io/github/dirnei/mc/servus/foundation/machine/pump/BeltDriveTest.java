package io.github.dirnei.mc.servus.foundation.machine.pump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BeltDriveTest {

    private static final BeltDrive DRIVE = new BeltDrive(13, 3.2F, 1.2F, 12, 21, 1.6F);

    /** Each strand ends on both pulleys and meets them at a right angle, so it lies tangent to both. */
    @Test
    void strandsTouchBothPulleys() {
        for (int side = 0; side < 2; side++) {
            float[] s = DRIVE.strand(side);
            double dx = s[2] - s[0];
            double dy = s[3] - s[1];
            assertEquals(1.2, Math.hypot(s[0] - 13, s[1] - 3.2), 1e-4, "on the motor pulley");
            assertEquals(1.6, Math.hypot(s[2] - 12, s[3] - 21), 1e-4, "on the driven pulley");
            assertEquals(0, ((s[0] - 13) * dx + (s[1] - 3.2) * dy) / Math.hypot(dx, dy), 1e-4, "tangent at the motor pulley");
            assertEquals(0, ((s[2] - 12) * dx + (s[3] - 21) * dy) / Math.hypot(dx, dy), 1e-4, "tangent at the driven pulley");
            assertEquals(Math.hypot(dx, dy), DRIVE.length(side), 1e-4);
        }
        float[] a = DRIVE.strand(0);
        float[] b = DRIVE.strand(1);
        assertTrue(Math.abs(a[0] - b[0]) > 2, "the strands run on opposite sides");
        assertNotEquals(DRIVE.runsUp(0), DRIVE.runsUp(1), "one strand runs up, the other down");
    }

    @Test
    void motorPulleyTurnsFasterByTheRatio() {
        assertEquals(1.6F / 1.2F, DRIVE.ratio(), 1e-6);
        assertEquals(0, DRIVE.driverAngle(0), 1e-4);
        assertEquals(360 * 0.25 * DRIVE.ratio(), DRIVE.driverAngle(0.25), 1e-3);
        assertEquals(360 * (3 * DRIVE.ratio() % 1), DRIVE.driverAngle(3), 1e-2, "wraps below 360");
        assertEquals(2 * Math.PI * 1.6 * 0.5, DRIVE.travel(0.5), 1e-6, "the belt runs as far as the driven rim");
    }

    @Test
    void markWrapsAlongTheStrand() {
        for (int side = 0; side < 2; side++) {
            float length = DRIVE.length(side);
            double turnsPerPass = length / (2 * Math.PI * 1.6);
            float previous = DRIVE.markAlong(0.1 * turnsPerPass, side);
            int wraps = 0;
            int steps = 400;
            for (int i = 1; i <= steps; i++) {
                double turns = turnsPerPass * (0.1 + 2.5 * i / steps);
                float at = DRIVE.markAlong(turns, side);
                assertTrue(at >= 0 && at < length, "inside the strand");
                float step = at - previous;
                if (Math.abs(step) > length / 2) {
                    wraps++;
                } else {
                    assertTrue(DRIVE.runsUp(side) ? step > 0 : step < 0, "runs with the belt");
                }
                previous = at;
            }
            assertEquals(2, wraps, "wraps once per pass, side " + side);
        }
    }

    /** An idle pump's clock stands, so the pulley and the marks stand too. */
    @Test
    void standsStillWithTheCrank() {
        StrokeClock clock = new StrokeClock();
        for (int i = 0; i < 7; i++) {
            clock.tick(true);
        }
        double before = clock.phase(0.1F, false) / StrokeClock.PERIOD;
        double later = clock.phase(0.9F, false) / StrokeClock.PERIOD;
        assertEquals(DRIVE.driverAngle(before), DRIVE.driverAngle(later), 0);
        assertEquals(DRIVE.markAlong(before, 0), DRIVE.markAlong(later, 0), 0);
        assertEquals(DRIVE.markAlong(before, 1), DRIVE.markAlong(later, 1), 0);
        double running = clock.phase(0.9F, true) / StrokeClock.PERIOD;
        assertNotEquals(DRIVE.driverAngle(before), DRIVE.driverAngle(running), "turns while working");
    }
}
