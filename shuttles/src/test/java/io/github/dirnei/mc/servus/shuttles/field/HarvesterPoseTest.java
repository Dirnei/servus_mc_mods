package io.github.dirnei.mc.servus.shuttles.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.field.HarvesterParts.Box;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterParts.Part;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterPose.Placed;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterTimeline.Moment;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterTimeline.Stage;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HarvesterPoseTest {

    /** The plot floor under a shuttle on its rail, in model pixels. */
    private static final double FLOOR = -2.5;
    private static final Set<Part> HOLD = EnumSet.of(Part.BODY, Part.SIDE, Part.END, Part.DOOR);

    private static double[] log(int plotsOut, int sign) {
        return new double[] {8 + sign * plotsOut * HarvesterPose.BLOCK, FLOOR, 8};
    }

    @Test
    void gripCentreOnTrunkAxisForOneToFourPlots() {
        for (int sign : new int[] {1, -1}) {
            for (int plots = 1; plots <= 4; plots++) {
                double[] log = log(plots, sign);
                HarvesterPose pose = HarvesterPose.of(log, new Moment(Stage.GRIP, 1));
                double[] centre = pose.head().apply(0, HarvesterParts.BAR_PIVOT[1], HarvesterParts.LOG_NEAR + HarvesterParts.LOG_HALF);
                String at = plots + " plots, side " + sign;
                assertEquals(log[0], centre[0], 1e-9, at);
                assertEquals(log[2], centre[2], 1e-9, at);
                assertEquals(FLOOR + HarvesterPose.GRIP_HEIGHT + HarvesterParts.BAR_PIVOT[1], centre[1], 1e-9, at);
                assertTrue(centre[1] > FLOOR && centre[1] < FLOOR + 2, "the cut is at the base, " + at);
                assertEquals(0, pose.a1(), 1e-9);
                assertEquals(0, pose.a2(), 1e-9);
                assertEquals(HarvesterParts.BOOM_LENGTH, distance(pose.pillarTop(), pose.elbow()), 1e-6, "the main boom keeps its length, " + at);
            }
        }
        assertTrue(HarvesterPose.of(log(4, 1), new Moment(Stage.GRIP, 1)).reach() > HarvesterParts.STICK_LENGTH + 30,
                "the telescope runs out to a tree four plots out");
    }

    @Test
    void headFacesAForwardLogAlongZ() {
        double[] log = {8, FLOOR, 8 + 2 * HarvesterPose.BLOCK};
        HarvesterPose pose = HarvesterPose.grip(log);
        double[] centre = pose.head().apply(0, 0, HarvesterParts.LOG_NEAR + HarvesterParts.LOG_HALF);
        assertEquals(8, centre[0], 1e-9);
        assertEquals(log[2], centre[2], 1e-9);
    }

    @Test
    void stowedPartsInsideHold() {
        HarvesterPose pose = HarvesterPose.of(log(2, 1), new Moment(Stage.DOORS, 0));
        for (Placed placed : pose.parts()) {
            if (HOLD.contains(placed.part())) {
                continue;
            }
            for (Box box : HarvesterParts.boxes(placed.part(), 2)) {
                for (double[] corner : corners(box)) {
                    double[] p = placed.frame().apply(corner);
                    String at = placed.part() + " " + p[0] + " " + p[1] + " " + p[2];
                    assertTrue(p[0] > HarvesterParts.INNER - 10.5 - 1e-9 && p[0] < HarvesterParts.INNER + 1e-9, at);
                    assertTrue(p[2] > HarvesterParts.INNER - 10.5 - 1e-9 && p[2] < HarvesterParts.INNER + 1e-9, at);
                    assertTrue(p[1] > 4.75 - 1e-9 && p[1] < HarvesterParts.DOOR_TOP, at);
                }
            }
        }
    }

    @Test
    void restHasDoorsShutAndWallsUp() {
        for (HarvesterPose pose : List.of(HarvesterPose.of(log(1, 1), HarvesterTimeline.of(HarvesterTimeline.Part.UNFOLD, 0)),
                HarvesterPose.of(log(1, 1), HarvesterTimeline.of(HarvesterTimeline.Part.FOLD, 1)))) {
            assertEquals(0, pose.door(), 1e-9);
            assertEquals(0, pose.side(), 1e-9);
            assertEquals(0, pose.lift(), 1e-9);
            assertEquals(0, pose.unfold(), 1e-9);
            for (Placed placed : pose.parts()) {
                if (HOLD.contains(placed.part())) {
                    for (Box box : HarvesterParts.boxes(placed.part(), 3)) {
                        double[] p = placed.frame().apply(box.x1(), box.y1(), box.z1());
                        boolean inPlace = near(p, box.x1(), box.y1(), box.z1()) || near(p, 16 - box.x1(), box.y1(), 16 - box.z1());
                        assertTrue(inPlace, placed.part() + " stands where the closed hold has it");
                    }
                }
            }
        }
    }

    @Test
    void closedHoldStaysUnderTheDoorway() {
        for (int tier = 1; tier <= 3; tier++) {
            for (Box box : HarvesterParts.hold(tier)) {
                assertTrue(box.y2() <= HarvesterParts.TOP + 1e-9, "Mk" + tier + " " + box);
            }
        }
    }

    @Test
    void twoByTwoGripsNearestBaseLog() {
        double b = HarvesterPose.BLOCK;
        List<double[]> right = List.of(new double[] {8 + 3 * b, FLOOR, 8 + b}, new double[] {8 + 2 * b, FLOOR, 8 + b},
                new double[] {8 + 3 * b, FLOOR, 8}, new double[] {8 + 2 * b, FLOOR, 8});
        assertEquals(3, HarvesterPose.nearestLog(right, 8, 8));
        List<double[]> left = List.of(new double[] {8 - b, FLOOR, 8}, new double[] {8 - 2 * b, FLOOR, 8},
                new double[] {8 - b, FLOOR, 8 - b}, new double[] {8 - 2 * b, FLOOR, 8 - b});
        assertEquals(0, HarvesterPose.nearestLog(left, 8, 8));
    }

    @Test
    void noNaNOverSweep() {
        double b = HarvesterPose.BLOCK;
        List<double[]> logs = List.of(log(1, 1), log(4, -1), new double[] {8 + b, FLOOR + b, 8 - b}, new double[] {8, FLOOR, 8 + 3 * b},
                new double[] {8 + 2 * b, FLOOR - b, 8 + 2 * b});
        for (double[] log : logs) {
            for (HarvesterTimeline.Part part : HarvesterTimeline.Part.values()) {
                for (double p = 0; p <= 1.0001; p += 0.02) {
                    for (Placed placed : HarvesterPose.of(log, HarvesterTimeline.of(part, p)).parts()) {
                        for (double[] row : placed.frame().m()) {
                            for (double v : row) {
                                assertTrue(Double.isFinite(v), part + " " + p + " " + placed.part());
                            }
                        }
                        for (double v : placed.frame().t()) {
                            assertTrue(Double.isFinite(v), part + " " + p + " " + placed.part());
                        }
                    }
                }
            }
        }
    }

    static double[][] corners(Box box) {
        double[][] corners = new double[8][];
        for (int i = 0; i < 8; i++) {
            corners[i] = new double[] {(i & 1) == 0 ? box.x1() : box.x2(), (i & 2) == 0 ? box.y1() : box.y2(),
                    (i & 4) == 0 ? box.z1() : box.z2()};
        }
        return corners;
    }

    private static boolean near(double[] p, double x, double y, double z) {
        return Math.abs(p[0] - x) < 1e-6 && Math.abs(p[1] - y) < 1e-6 && Math.abs(p[2] - z) < 1e-6;
    }

    private static double distance(double[] a, double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
    }
}
