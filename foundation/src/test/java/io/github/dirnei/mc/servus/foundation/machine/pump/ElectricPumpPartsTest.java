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
        for (float[] part : new float[][] {ElectricPumpParts.PIN, ElectricPumpParts.CON_ROD, ElectricPumpParts.FAN}) {
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

    /** The wheel turns on the axle, which runs back through the bearing to the axle pulley. */
    @Test
    void wheelSitsOnItsAxle() {
        float[] axle = ElectricPumpParts.AXLE;
        assertEquals(ElectricPumpParts.CRANK_X, (axle[0] + axle[3]) / 2, 1e-5);
        assertEquals(ElectricPumpParts.CRANK_Y, (axle[1] + axle[4]) / 2, 1e-5);
        assertEquals(ElectricPumpParts.WHEEL[5], axle[2], 1e-5, "the axle starts at the wheel's back face");
        float[] bearing = ElectricPumpParts.BEARING;
        assertTrue(bearing[0] < axle[0] && axle[3] < bearing[3] && bearing[1] < axle[1] && axle[4] < bearing[4], "the bearing holds the axle");
        assertTrue(axle[2] < bearing[2] && bearing[5] < axle[5], "the axle passes through the bearing");
        assertTrue(axle[5] >= ElectricPumpParts.AXLE_PULLEY[5], "the axle reaches through its pulley");
        assertEquals(2, ElectricPumpParts.WHEEL[5] - ElectricPumpParts.WHEEL[2], 1e-5, "the wheel is two pixels thick");
        float[] rim = ElectricPumpParts.RIM_SEGMENT;
        assertEquals(ElectricPumpParts.CRANK_X + ElectricPumpParts.CRANK_R, rim[3], 1e-5, "the rim is the wheel's edge");
        assertEquals(ElectricPumpParts.CRANK_Y, (rim[1] + rim[4]) / 2, 1e-5);
        double segment = 2 * Math.PI * ElectricPumpParts.CRANK_R / ElectricPumpParts.RIM_SEGMENTS;
        assertTrue(rim[4] - rim[1] >= segment * 0.95, "the segments close the rim");
    }

    /** Modelled spokes run from inside the hub into the rim, the hub holds the axle and the crank pin sits on a spoke. */
    @Test
    void spokesFromHubToRim() {
        float cx = ElectricPumpParts.CRANK_X;
        float cy = ElectricPumpParts.CRANK_Y;
        float[] hub = ElectricPumpParts.HUB;
        float[] spoke = ElectricPumpParts.SPOKE;
        float[] rim = ElectricPumpParts.RIM_SEGMENT;
        float[] axle = ElectricPumpParts.AXLE;
        assertTrue(hub[0] < axle[0] && axle[3] < hub[3] && hub[1] < axle[1] && axle[4] < hub[4], "the hub is round the axle");
        assertTrue(hub[5] > axle[2], "the axle reaches into the hub");
        assertTrue(spoke[0] < hub[3], "the spoke starts inside the hub");
        assertTrue(spoke[3] > rim[0] && spoke[3] <= rim[3], "the spoke ends inside the rim");
        assertEquals(cy, (spoke[1] + spoke[4]) / 2, 1e-5, "the spoke points at the centre");
        assertTrue(spoke[2] >= rim[2] && spoke[5] <= rim[5], "no thicker than the wheel");
        assertTrue(ElectricPumpParts.SPOKES >= 4, "spokes all round");
        float[] pin = ElectricPumpParts.PIN;
        assertEquals(spoke[2], pin[5], 1e-5, "the pin sits on the front of the spoke at angle 0");
        float pinX = (pin[0] + pin[3]) / 2;
        float pinY = (pin[1] + pin[4]) / 2;
        assertTrue(spoke[0] < pinX && pinX < spoke[3] && spoke[1] < pinY && pinY < spoke[4], "the pin is on the spoke");
        assertTrue(hub[0] < cx && cx < hub[3], "the hub is centred");
    }

    /** The column stands on the cap and carries the bearing, nothing floats. */
    @Test
    void bearingStandsOnTheCap() {
        float[] cap = ElectricPumpParts.CAP;
        float[] column = ElectricPumpParts.BEARING_COLUMN;
        float[] bearing = ElectricPumpParts.BEARING;
        assertEquals(cap[4], column[1], 1e-5, "the column stands on the cap");
        assertEquals(bearing[1], column[4], 1e-5, "the bearing sits on the column");
        assertTrue(cap[0] <= column[0] && column[3] <= cap[3] && cap[2] <= column[2] && column[5] <= cap[5], "the column is on the cap");
        assertTrue(bearing[0] <= column[0] && column[3] <= bearing[3] && bearing[2] <= column[2] && column[5] <= bearing[5],
                "the column is under the bearing");
        assertEquals(cap[4], ElectricPumpParts.WHEEL[1], 1e-5, "the wheel just clears the cap");
        assertTrue(cap[3] >= ElectricPumpParts.CRANK_X + 2, "the cap reaches under the wheel");
    }

    /** Distance from a point to the box x 5..11, y 5..11 of a two line stub on the back. */
    private static double fromBackStub(float x, float y) {
        double dx = Math.max(0, Math.max(5 - x, x - 11));
        double dy = Math.max(0, Math.max(5 - y, y - 11));
        return Math.hypot(dx, dy);
    }

    /** The belt and the motor pulley stay clear of a pipe or cable on the back, and behind the windings and bearing. */
    @Test
    void beltClearsTheBackStub() {
        BeltDrive drive = ElectricPumpParts.DRIVE;
        for (int i = 0; i <= 400; i++) {
            float[] point = drive.pointOnLoop(drive.loopLength() * i / 400);
            assertTrue(fromBackStub(point[0], point[1]) > ElectricPumpParts.BELT_T / 2,
                    "belt at " + point[0] + ", " + point[1]);
        }
        float[] pulley = ElectricPumpParts.MOTOR_PULLEY;
        assertTrue(pulley[0] > 11 || pulley[4] < 5, "motor pulley");
        assertFalse(BeamPumpPartsTest.overlaps(new float[] {5, 5, 13, 11, 11, 16}, ElectricPumpParts.MOTOR_SHAFT), "motor shaft");
        for (float[] winding : ElectricPumpParts.WINDINGS) {
            assertTrue(winding[5] < ElectricPumpParts.BELT_Z1, "the belt runs behind the windings");
        }
        assertTrue(ElectricPumpParts.BEARING[5] < ElectricPumpParts.BELT_Z1, "the belt runs behind the bearing");
    }

    /** Both strands and pulleys stay inside the two blocks for every facing. */
    @Test
    void beltInsideTheBlock() {
        BeltDrive drive = ElectricPumpParts.DRIVE;
        float half = ElectricPumpParts.BELT_T / 2;
        for (int i = 0; i <= 400; i++) {
            float[] point = drive.pointOnLoop(drive.loopLength() * i / 400);
            assertTrue(point[0] - half >= 0 && point[0] + half <= 16, "belt x at " + point[0] + ", " + point[1]);
            assertTrue(point[1] - half >= 0 && point[1] + half <= 32, "belt y at " + point[0] + ", " + point[1]);
        }
        for (int quarters = 0; quarters < 4; quarters++) {
            assertTrue(inside(BeamPumpParts.rotated(ElectricPumpParts.MOTOR_PULLEY, quarters)), "motor pulley turned " + quarters);
            assertTrue(inside(BeamPumpParts.rotated(ElectricPumpParts.AXLE_PULLEY, quarters)), "axle pulley turned " + quarters);
            assertTrue(inside(BeamPumpParts.rotated(ElectricPumpParts.AXLE, quarters)), "axle turned " + quarters);
        }
        assertTrue(ElectricPumpParts.BELT_Z2 <= 16 && ElectricPumpParts.BELT_MARK[5] <= 16, "belt plane");
    }
}
