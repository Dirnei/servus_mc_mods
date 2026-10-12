package io.github.dirnei.mc.servus.foundation.machine.pump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BeamStrokeTest {

    private final BeamStroke stroke = new BeamStroke(12, 40);

    @Test
    void levelAtPhaseZero() {
        assertEquals(0, stroke.angle(0), 1e-6);
    }

    @Test
    void neverPastTheAmplitude() {
        for (double phase = 0; phase < 200; phase += 0.37) {
            assertTrue(Math.abs(stroke.angle(phase)) <= 12 + 1e-4, "phase " + phase);
        }
        assertEquals(12, stroke.angle(10), 1e-4);
    }

    @Test
    void repeatsAfterOnePeriod() {
        for (double phase = 0; phase < 40; phase += 3.1) {
            assertEquals(stroke.angle(phase), stroke.angle(phase + 40), 1e-4);
        }
    }

    @Test
    void bottomsCountsOncePerPeriod() {
        assertEquals(1, stroke.bottoms(0, 40));
        assertEquals(1, stroke.bottoms(5, 45));
        int total = 0;
        for (double phase = 0; phase < 40; phase += 1) {
            total += stroke.bottoms(phase, phase + 1);
        }
        assertEquals(1, total, "one bottom per stroke, counted tick by tick");
    }

    @Test
    void noBottomWhileStill() {
        assertEquals(0, stroke.bottoms(10, 10));
        assertEquals(0, stroke.bottoms(12, 12));
    }

    @Test
    void twoBottomsAcrossTwoPeriods() {
        assertEquals(2, stroke.bottoms(0, 80));
        assertEquals(2, stroke.bottoms(3, 83));
    }

    /** The pump rod hangs on the negative arm: its bottom is where the beam angle peaks. */
    @Test
    void bottomIsAtTheLargestAngle() {
        assertEquals(1, stroke.bottoms(9.9, 10));
        assertEquals(0, stroke.bottoms(10, 10.1));
        assertTrue(BeamStroke.rodOffset(stroke.angle(10), BeamPumpParts.ROD_X - BeamPumpParts.PIVOT_X) < 0);
    }

    @Test
    void rodRestsAtLevelBeam() {
        assertEquals(0, BeamStroke.rodOffset(0, 6), 1e-6);
    }

    @Test
    void rodFollowsTheBeamEnd() {
        assertEquals(6 * Math.sin(Math.toRadians(10)), BeamStroke.rodOffset(10, 6), 1e-5);
        assertTrue(BeamStroke.rodOffset(10, 6) > 0);
        assertTrue(BeamStroke.rodOffset(-10, 6) < 0);
        assertTrue(BeamStroke.rodOffset(10, -4) < 0, "the other end of the beam goes the other way");
    }
}
