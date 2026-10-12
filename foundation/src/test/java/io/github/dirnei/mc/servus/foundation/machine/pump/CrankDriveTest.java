package io.github.dirnei.mc.servus.foundation.machine.pump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CrankDriveTest {

    private static final float PERIOD = 40;
    private final BeamStroke stroke = new BeamStroke(12, PERIOD);
    private final CrankDrive crank = new CrankDrive(stroke, PERIOD, 2);

    @Test
    void pinHighestAtAQuarterPeriod() {
        assertEquals(2, crank.pinY(PERIOD / 4), 1e-4);
        assertEquals(0, crank.pinX(PERIOD / 4), 1e-4);
        for (double phase = 0; phase < PERIOD; phase += 0.5) {
            assertTrue(crank.pinY(phase) <= crank.pinY(PERIOD / 4) + 1e-5, "phase " + phase);
        }
    }

    @Test
    void beamAngleMatchesTheStroke() {
        for (int i = 0; i < 8; i++) {
            double phase = i * PERIOD / 8 + 1.3;
            assertEquals(stroke.angle(phase), crank.beamAngle(phase), 1e-5, "phase " + phase);
        }
    }

    /** The beam end above the crank (positive arm) is highest exactly when the pin is. */
    @Test
    void beamEndAboveTheCrankFollowsThePin() {
        float arm = BeamPumpParts.PISTON_X - BeamPumpParts.PIVOT_X;
        double top = PERIOD / 4;
        assertEquals(12, crank.beamAngle(top), 1e-4);
        assertTrue(BeamStroke.rodOffset(crank.beamAngle(top), arm) > 0);
        double bottom = 3 * PERIOD / 4;
        assertEquals(-2, crank.pinY(bottom), 1e-4);
        assertTrue(BeamStroke.rodOffset(crank.beamAngle(bottom), arm) < 0);
    }

    @Test
    void crankAngleWrapsAt360() {
        assertEquals(0, crank.crankAngle(0), 1e-5);
        assertEquals(90, crank.crankAngle(PERIOD / 4), 1e-4);
        assertEquals(0, crank.crankAngle(PERIOD), 1e-4);
        assertEquals(90, crank.crankAngle(PERIOD * 5 + PERIOD / 4), 1e-3);
        for (double phase = 0; phase < 400; phase += 0.77) {
            float angle = crank.crankAngle(phase);
            assertTrue(angle >= 0 && angle < 360, "phase " + phase);
        }
    }

    @Test
    void fanTurnsFasterAndWraps() {
        assertEquals(90 * CrankDrive.FAN_GEAR % 360, crank.fanAngle(PERIOD / 4), 1e-3);
        for (double phase = 0; phase < 400; phase += 0.77) {
            float angle = crank.fanAngle(phase);
            assertTrue(angle >= 0 && angle < 360, "phase " + phase);
        }
    }
}
