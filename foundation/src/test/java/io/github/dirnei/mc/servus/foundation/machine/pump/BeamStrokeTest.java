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
