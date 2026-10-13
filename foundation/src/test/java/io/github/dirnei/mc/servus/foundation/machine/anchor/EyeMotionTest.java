package io.github.dirnei.mc.servus.foundation.machine.anchor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EyeMotionTest {

    private static final float FRAME = 0.05F;

    @Test
    void opensInAboutASecond() {
        float open = 0;
        int frames = 0;
        while (open < 1 && frames < 1000) {
            open = EyeMotion.openness(open, true, FRAME);
            frames++;
        }
        assertEquals(1, open, 1e-6);
        assertTrue(frames * FRAME >= 0.8 && frames * FRAME <= 1.2, "opened after " + frames * FRAME + " s");
        assertEquals(1, EyeMotion.openness(1, true, FRAME), 1e-6, "stays open");
    }

    @Test
    void closesTheSameWay() {
        float open = 1;
        int frames = 0;
        while (open > 0 && frames < 1000) {
            open = EyeMotion.openness(open, false, FRAME);
            frames++;
        }
        assertEquals(0, open, 1e-6);
        assertTrue(frames * FRAME >= 0.8 && frames * FRAME <= 1.2, "closed after " + frames * FRAME + " s");
        assertEquals(1, EyeMotion.lid(0, 0), 1e-6, "a closed eye has its lid shut");
        assertEquals(0, EyeMotion.lid(1, 0), 1e-6, "an open eye between blinks has its lid up");
    }

    /** The moments the lid is shut furthest, sampled finely over a stretch of time. */
    private static List<Double> blinkPeaks(long seed, double from, double to) {
        List<Double> peaks = new ArrayList<>();
        double step = 0.005;
        float before = 0;
        float now = EyeMotion.blink(seed, from);
        for (double t = from + step; t < to; t += step) {
            float next = EyeMotion.blink(seed, t);
            if (now > 0.5F && now >= before && now >= next) {
                if (peaks.isEmpty() || t - peaks.get(peaks.size() - 1) > 0.5) {
                    peaks.add(t);
                }
            }
            before = now;
            now = next;
        }
        return peaks;
    }

    @Test
    void blinkSpacingThreeToNineSeconds() {
        long seed = 1234567L;
        double previous = EyeMotion.blinkStart(seed, 1000);
        for (long w = 1001; w <= 1100; w++) {
            double start = EyeMotion.blinkStart(seed, w);
            double gap = start - previous;
            assertTrue(gap >= 3 && gap <= 9, "gap " + gap + " at window " + w);
            previous = start;
        }
        List<Double> peaks = blinkPeaks(seed, 0, 30);
        assertTrue(peaks.size() >= 3 && peaks.size() <= 10, "half a minute shows several blinks, got " + peaks.size());
    }

    @Test
    void blinksAreShort() {
        long seed = 99L;
        for (long w = 0; w < 100; w++) {
            double start = EyeMotion.blinkStart(seed, w);
            assertTrue(EyeMotion.blink(seed, start + EyeMotion.BLINK / 2) > 0.9F, "the lid shuts mid blink");
            assertEquals(0, EyeMotion.blink(seed, start + 0.2), 1e-6, "the blink is over after 0.2 s");
            assertEquals(0, EyeMotion.blink(seed, start - 0.01), 1e-6, "no closing before it starts");
        }
    }

    @Test
    void seedsBlinkApart() {
        List<Double> a = blinkPeaks(1L, 0, 60);
        List<Double> b = blinkPeaks(2L, 0, 60);
        int together = 0;
        for (double ta : a) {
            for (double tb : b) {
                if (Math.abs(ta - tb) < 0.1) {
                    together++;
                }
            }
        }
        assertTrue(together <= 1, "two loaders blink together " + together + " times in a minute");
    }

    @Test
    void irisPointsTowardPlayer() {
        // Facing north the eye looks toward -z; +x is the viewer's left.
        float[] left = EyeMotion.iris(3, 0, -4, 0);
        assertTrue(left[0] > 0.5F && Math.abs(left[1]) < 1e-6, "left " + left[0]);
        float[] right = EyeMotion.iris(-3, 0, -4, 0);
        assertTrue(right[0] < -0.5F, "right " + right[0]);
        float[] up = EyeMotion.iris(0, 3, -4, 0);
        assertTrue(up[1] > 0.5F, "up " + up[1]);
        // Facing east (quarter 1) the eye looks toward +x; the player turned a quarter with it stands at (4, 3).
        float[] east = EyeMotion.iris(4, 0, 3, 1);
        float[] north = EyeMotion.iris(3, 0, -4, 0);
        assertTrue(Math.abs(east[0] - north[0]) < 1e-5, "turning the eye and the player together gives the same iris");
    }

    @Test
    void irisCentredBehindOrFar() {
        float[] behind = EyeMotion.iris(1, 0, 3, 0);
        assertEquals(0, behind[0], 1e-6);
        assertEquals(0, behind[1], 1e-6);
        float[] far = EyeMotion.iris(5, 0, -20, 0);
        assertEquals(0, far[0], 1e-6);
        float[] behindEast = EyeMotion.iris(-3, 0, 0, 1);
        assertEquals(0, behindEast[0], 1e-6, "behind an eye facing east");
    }

    @Test
    void irisStaysInLens() {
        for (int q = 0; q < 4; q++) {
            for (double dx = -8; dx <= 8; dx += 0.5) {
                for (double dz = -8; dz <= 8; dz += 0.5) {
                    float[] iris = EyeMotion.iris(dx, 1.5, dz, q);
                    double r = Math.sqrt(iris[0] * iris[0] + iris[1] * iris[1]);
                    assertTrue(r <= EyeMotion.IRIS_RADIUS + 1e-5, "radius " + r);
                }
            }
        }
    }

    @Test
    void smoothingConverges() {
        float value = 0;
        for (int i = 0; i < 40; i++) {
            float next = EyeMotion.smooth(value, 1, FRAME);
            assertTrue(next >= value && next <= 1, "moves toward the target without passing it");
            value = next;
        }
        assertEquals(1, value, 0.01);
    }
}
