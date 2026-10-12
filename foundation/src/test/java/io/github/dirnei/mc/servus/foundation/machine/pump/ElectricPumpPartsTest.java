package io.github.dirnei.mc.servus.foundation.machine.pump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ElectricPumpPartsTest {

    private static boolean inside(float[] box) {
        for (int i = 0; i < 3; i++) {
            if (box[i] < 0 || box[i + 3] > 16 || box[i] >= box[i + 3]) {
                return false;
            }
        }
        return true;
    }

    @Test
    void lowerPartsInsideTheirHalf() {
        for (float[] part : ElectricPumpParts.lowerParts()) {
            assertTrue(inside(part), "lower part " + java.util.Arrays.toString(part));
        }
    }

    @Test
    void upperPartsInsideTheirHalf() {
        for (float[] part : ElectricPumpParts.upperParts()) {
            assertTrue(inside(part), "upper part " + java.util.Arrays.toString(part));
        }
        for (float[] part : new float[][] {ElectricPumpParts.PIN, ElectricPumpParts.CON_ROD, ElectricPumpParts.FAN_BLADE_Y,
                ElectricPumpParts.FAN_BLADE_Z}) {
            assertTrue(inside(part), "moving part " + java.util.Arrays.toString(part));
        }
    }

    /** At rest the con rod stands on the pin at its top and reaches the level beam where the drive rod hangs. */
    @Test
    void conRodFromCrankPinToTheBeam() {
        float[] rod = ElectricPumpParts.CON_ROD;
        assertEquals(BeamPumpParts.PISTON_X, (rod[0] + rod[3]) / 2, 1e-5);
        assertEquals(ElectricPumpParts.CRANK_X, (rod[0] + rod[3]) / 2, 1e-5, "the crank turns under the drive end");
        assertEquals(ElectricPumpParts.CRANK_Y + ElectricPumpParts.PIN_R, rod[1], 1e-5);
        assertEquals(BeamPumpParts.PIVOT_Y, rod[4], 1e-5);
        float[] pin = ElectricPumpParts.PIN;
        assertEquals(ElectricPumpParts.CRANK_X + ElectricPumpParts.PIN_R, (pin[0] + pin[3]) / 2, 1e-5, "the pin sits at crank angle 0");
        assertTrue(pin[2] <= rod[2] && rod[5] <= pin[5] + 1, "the pin reaches the con rod's plane");
    }

    @Test
    void rotatedPartsStayInsideTheBlock() {
        for (int quarters = 0; quarters < 4; quarters++) {
            for (float[] part : ElectricPumpParts.lowerParts()) {
                assertTrue(inside(BeamPumpParts.rotated(part, quarters)), "lower part turned " + quarters);
            }
            for (float[] part : ElectricPumpParts.upperParts()) {
                assertTrue(inside(BeamPumpParts.rotated(part, quarters)), "upper part turned " + quarters);
            }
        }
    }

    /** The terminal sits on the front, which never has a stub, and the windings leave room for the pump rod. */
    @Test
    void clearOfRodAndBackStub() {
        float[] backStub = {5, 5, 13, 11, 11, 16};
        assertFalse(BeamPumpPartsTest.overlaps(backStub, ElectricPumpParts.TERMINAL), "terminal");
        for (float[] winding : ElectricPumpParts.WINDINGS) {
            assertFalse(BeamPumpPartsTest.overlaps(winding, BeamPumpParts.ROD), "winding and rod");
        }
        assertFalse(BeamPumpPartsTest.overlaps(ElectricPumpParts.WHEEL, BeamPumpParts.POST), "wheel and post");
        assertFalse(BeamPumpPartsTest.overlaps(ElectricPumpParts.GRILLE_BOX, BeamPumpParts.BEAM), "grille and beam");
    }
}
