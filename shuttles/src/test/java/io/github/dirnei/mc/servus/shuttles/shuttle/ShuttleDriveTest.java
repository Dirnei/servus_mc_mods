package io.github.dirnei.mc.servus.shuttles.shuttle;

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

    @Test
    void glideStopsAtAStandingLead() {
        double[] at = ShuttleDrive.glide(21, 0, 0, TOP, TOP, ACCEL, 300, i -> false, i -> false, 0, 6.5, 0);
        assertEquals(6.5, at[0] + at[1], 1e-6);
        assertEquals(0, at[2], 1e-9);
    }

    @Test
    void glideBehindAMovingLeadTakesItsSpeedAndNeverPassesIt() {
        double leadSpeed = TOP / 2;
        double v = TOP;
        double[] at = {0, 0};
        for (int t = 1; t <= 150; t++) {
            double[] next = ShuttleDrive.glide(80, (int) at[0], at[1], v, TOP, ACCEL, 1, i -> false, i -> false, 0, 2 + leadSpeed * (t - 1), leadSpeed);
            at = next;
            v = next[2];
            assertTrue(at[0] + at[1] <= 2 + leadSpeed * t + 1e-9, "never past the lead at tick " + t);
        }
        assertEquals(leadSpeed, v, 1e-3);
        double[] whole = ShuttleDrive.glide(80, 0, 0, TOP, TOP, ACCEL, 150, i -> false, i -> false, 0, 2, leadSpeed);
        assertEquals(at[0] + at[1], whole[0] + whole[1], 1e-9);
    }

    /** An L path of 21 nodes turning at node 10. */
    private static final int NODES = 21;
    private static final int TURN = 10;
    private static final double CORNER = 0.1;

    @Test
    void noCornerNoLimit() {
        assertEquals(Double.POSITIVE_INFINITY, ShuttleDrive.cornerLimit(NODES, 0, 0, i -> false, CORNER, ACCEL));
        assertEquals(Double.POSITIVE_INFINITY, ShuttleDrive.cornerLimit(NODES, 0, 0, i -> i == TURN, 0, ACCEL));
    }

    @Test
    void insideCornerZoneIsCornerSpeed() {
        assertEquals(CORNER, ShuttleDrive.cornerLimit(NODES, TURN - 1, 0.6, i -> i == TURN, CORNER, ACCEL), 1e-9);
        assertEquals(CORNER, ShuttleDrive.cornerLimit(NODES, TURN, 0.4, i -> i == TURN, CORNER, ACCEL), 1e-9);
        assertEquals(Double.POSITIVE_INFINITY, ShuttleDrive.cornerLimit(NODES, TURN, 0.6, i -> i == TURN, CORNER, ACCEL));
    }

    @Test
    void limitRisesWithDistanceBeforeCorner() {
        double near = ShuttleDrive.cornerLimit(NODES, TURN - 2, 0, i -> i == TURN, CORNER, ACCEL);
        double far = ShuttleDrive.cornerLimit(NODES, TURN - 6, 0, i -> i == TURN, CORNER, ACCEL);
        assertEquals(-ACCEL + Math.sqrt(ACCEL * ACCEL + CORNER * CORNER + 2 * ACCEL * 1.5), near, 1e-9);
        assertTrue(far > near, "farther from the corner allows more speed");
    }

    @Test
    void nearestCornerWins() {
        double both = ShuttleDrive.cornerLimit(NODES, 0, 0, i -> i == 4 || i == TURN, CORNER, ACCEL);
        double first = ShuttleDrive.cornerLimit(NODES, 0, 0, i -> i == 4, CORNER, ACCEL);
        assertEquals(first, both, 1e-9);
    }

    @Test
    void fastShuttleReachesCornerAtCornerSpeed() {
        double[] at = {0, 0};
        double v = TOP;
        double highestInZone = 0;
        for (int t = 0; t < 400 && at[0] < NODES - 1; t++) {
            double[] next = ShuttleDrive.glide(NODES, (int) at[0], at[1], v, TOP, ACCEL, 1, i -> false, i -> i == TURN, CORNER);
            at = new double[] {next[0], next[1]};
            v = next[2];
            double position = at[0] + at[1];
            if (Math.abs(position - TURN) <= 0.5) {
                highestInZone = Math.max(highestInZone, v);
            }
        }
        assertTrue(highestInZone > 0, "the shuttle passed the corner");
        assertTrue(highestInZone <= CORNER + 1e-9, "no faster than the corner speed in the corner, was " + highestInZone);
    }

    @Test
    void speedsUpAfterCorner() {
        double[] after = ShuttleDrive.glide(NODES, TURN, 0.5, CORNER, TOP, ACCEL, 6, i -> false, i -> i == TURN, CORNER);
        assertTrue(after[2] > CORNER + 0.1, "it speeds up again after the corner, was " + after[2]);
    }

    @Test
    void cornerIsATurnOnOneLevel() {
        assertTrue(ShuttleDrive.corner(0, 0, 0, 1, 0, 0, 1, 0, 1), "east then south turns");
        assertTrue(!ShuttleDrive.corner(0, 0, 0, 1, 0, 0, 2, 0, 0), "straight on is no corner");
        assertTrue(!ShuttleDrive.corner(0, 0, 0, 1, 0, 0, 1, 1, 0), "a step up is the lift, not a corner");
    }

    @Test
    void glideThroughCornerMatchesTickByTick() {
        double v = 0;
        double[] at = {TURN - 4, 0};
        for (int t = 0; t < 30; t++) {
            double[] next = ShuttleDrive.glide(NODES, (int) at[0], at[1], v, TOP, ACCEL, 1, i -> false, i -> i == TURN, CORNER);
            at = new double[] {next[0], next[1]};
            v = next[2];
        }
        double[] whole = ShuttleDrive.glide(NODES, TURN - 4, 0, 0, TOP, ACCEL, 30, i -> false, i -> i == TURN, CORNER);
        assertEquals(at[0] + at[1], whole[0] + whole[1], 1e-9);
        assertEquals(v, whole[2], 1e-9);
    }
}
