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

    /** The loop is both strands plus the arcs over the driven pulley's top and under the motor pulley, each about half way round. */
    @Test
    void loopLengthIsStrandsAndArcs() {
        double over = DRIVE.arcSpan(1);
        double under = DRIVE.arcSpan(0);
        assertEquals(2 * Math.PI, over + under, 1e-5, "the two arcs make one full turn");
        assertTrue(over > Math.PI && over < Math.PI * 1.2, "the larger driven pulley is wrapped a little more than half way");
        assertEquals(1.6 * over, DRIVE.arcLength(1), 1e-4);
        assertEquals(1.2 * under, DRIVE.arcLength(0), 1e-4);
        assertEquals(DRIVE.length(0) + DRIVE.length(1) + DRIVE.arcLength(0) + DRIVE.arcLength(1), DRIVE.loopLength(), 1e-4);
        assertTrue(DRIVE.arcPoint(1, 0.5)[1] > 21 + 1.5, "the driven arc runs over the top");
        assertTrue(DRIVE.arcPoint(0, 0.5)[1] < 3.2F - 1.1F, "the motor arc runs under the bottom");
        float[] s0 = DRIVE.strand(0);
        float[] s1 = DRIVE.strand(1);
        assertPoint(new float[] {s0[2], s0[3]}, DRIVE.arcPoint(1, 0), "the driven arc starts at strand 0's end");
        assertPoint(new float[] {s1[2], s1[3]}, DRIVE.arcPoint(1, 1), "and ends at strand 1's end");
        assertPoint(new float[] {s1[0], s1[1]}, DRIVE.arcPoint(0, 0), "the motor arc starts at strand 1's start");
        assertPoint(new float[] {s0[0], s0[1]}, DRIVE.arcPoint(0, 1), "and ends at strand 0's start");
    }

    /** Walking the loop in small steps never jumps: strands and arcs join up, and the heading follows the step. */
    @Test
    void loopIsClosedAndRunsForward() {
        assertTrue(DRIVE.runsUp(0), "the loop runs up strand 0");
        float loop = DRIVE.loopLength();
        int steps = 2000;
        float step = loop / steps;
        float[] previous = DRIVE.pointOnLoop(0);
        for (int i = 1; i <= steps; i++) {
            float[] point = DRIVE.pointOnLoop(step * i);
            double dx = point[0] - previous[0];
            double dy = point[1] - previous[1];
            assertEquals(step, Math.hypot(dx, dy), step * 0.05, "step " + i);
            double off = Math.abs(Math.IEEEremainder(Math.atan2(dy, dx) - point[2], 2 * Math.PI));
            assertTrue(off < 0.1, "heading at step " + i);
            previous = point;
        }
        assertPoint(DRIVE.pointOnLoop(0), DRIVE.pointOnLoop(loop), "the loop closes");
    }

    /** The mark runs round the whole loop: up a strand, over the driven pulley, down, under the motor pulley and back. */
    @Test
    void markRunsRoundTheLoop() {
        float loop = DRIVE.loopLength();
        double turnsPerLap = loop / (2 * Math.PI * 1.6);
        boolean overTop = false;
        boolean underBottom = false;
        int wraps = 0;
        float previous = DRIVE.markOnLoop(0, 0);
        int steps = 800;
        for (int i = 1; i <= steps; i++) {
            float at = DRIVE.markOnLoop(turnsPerLap * 2.5 * i / steps, 0);
            assertTrue(at >= 0 && at < loop, "on the loop");
            if (at < previous) {
                wraps++;
            }
            float[] point = DRIVE.pointOnLoop(at);
            overTop = overTop || point[1] > 21 + 1.5;
            underBottom = underBottom || point[1] < 3.2F - 1.1F;
            previous = at;
        }
        assertEquals(2, wraps, "wraps once per lap");
        assertTrue(overTop, "the mark passes over the driven pulley");
        assertTrue(underBottom, "the mark passes under the motor pulley");
        float lead = DRIVE.markOnLoop(0.3, loop / 2);
        assertEquals((DRIVE.markOnLoop(0.3, 0) + loop / 2) % loop, lead, 1e-3, "an offset mark runs half a loop ahead");
        float halfOver = DRIVE.length(0) + DRIVE.arcLength(1) / 2;
        assertPoint(DRIVE.arcPoint(1, 0.5), DRIVE.pointOnLoop(DRIVE.markOnLoop(halfOver / (2 * Math.PI * 1.6), 0)),
                "half way over the driven arc");
    }

    private static void assertPoint(float[] expected, float[] actual, String message) {
        assertEquals(expected[0], actual[0], 1e-3, message + " x");
        assertEquals(expected[1], actual[1], 1e-3, message + " y");
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
        assertEquals(DRIVE.markOnLoop(before, 0), DRIVE.markOnLoop(later, 0), 0);
        assertEquals(DRIVE.markOnLoop(before, 5), DRIVE.markOnLoop(later, 5), 0);
        double running = clock.phase(0.9F, true) / StrokeClock.PERIOD;
        assertNotEquals(DRIVE.driverAngle(before), DRIVE.driverAngle(running), "turns while working");
    }
}
