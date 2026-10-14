package io.github.dirnei.mc.servus.foundation.logistics.duct;

import static io.github.dirnei.mc.servus.foundation.logistics.duct.DuctShapes.max;
import static io.github.dirnei.mc.servus.foundation.logistics.duct.DuctShapes.min;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctPose.Height;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctPose.Rank;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctShapes.Box;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DuctShapesPoseTest {

    private static final float LIFT = DuctShapes.LIFT;
    private static final int EAST = 1 << DuctShapes.EAST;
    private static final int WEST = 1 << DuctShapes.WEST;
    private static final int FLOOR = DuctPose.of(Height.LOW, Rank.ANCHORED);

    /** Faces of a duct whose every arm side meets the neighbour at {@code pose}. */
    private static int facesLike(int pose) {
        return DuctPose.faces(pose, 63, 0, new int[] {pose, pose, pose, pose, pose, pose});
    }

    /** Arm radii with {@code radius} on the sides of the mask and none elsewhere. */
    private static int[] radii(int radius, int sides) {
        int[] result = new int[6];
        for (int side = 0; side < 6; side++) {
            result[side] = (sides & 1 << side) != 0 ? radius : 0;
        }
        return result;
    }

    @Test
    void middlePoseMatchesOldBoxes() {
        assertEquals(new Box(6, 6, 6, 10, 10, 10), DuctShapes.center(2, DuctPose.CENTRED, 0, radii(2, 63)));
        assertEquals(new Box(6, 6, 0, 10, 10, 6), DuctShapes.arm(DuctShapes.NORTH, 2, DuctShapes.center(2), 0));
        assertEquals(new Box(11, 5, 5, 16, 11, 11), DuctShapes.arm(DuctShapes.EAST, 3, DuctShapes.center(3), 0));
        assertEquals(new Box(6, 0, 6, 10, 6, 10), DuctShapes.arm(DuctShapes.DOWN, 2, DuctShapes.center(2), 0));
        assertEquals(new Box(6, 6, 12, 10, 10, 16), DuctShapes.arm(DuctShapes.SOUTH, 2, DuctShapes.center(4), 0),
                "a thin arm starts at a thick centre's face");
    }

    @Test
    void floorCentreSitsOnLift() {
        assertEquals(new Box(6, LIFT, 6, 10, LIFT + 4, 10), DuctShapes.center(2, FLOOR));
        assertEquals(new Box(4, LIFT, 4, 12, LIFT + 8, 12), DuctShapes.center(4, FLOOR), "in the middle across the block");
    }

    @Test
    void armsFollowTheLowCentre() {
        int faces = facesLike(FLOOR);
        Box centre = DuctShapes.center(2, FLOOR, faces, radii(2, EAST | 1 << DuctShapes.UP));
        assertEquals(new Box(6, LIFT, 6, 10, LIFT + 4, 10), centre, "a duct whose arms all meet low keeps its low centre");
        assertEquals(new Box(10, LIFT, 6, 16, LIFT + 4, 10), DuctShapes.arm(DuctShapes.EAST, 2, centre, faces));
        assertEquals(new Box(6, LIFT + 4, 6, 10, 16, 10), DuctShapes.arm(DuctShapes.UP, 2, centre, faces),
                "the arm up starts at the low centre's top");
    }

    /** A low duct whose east arm meets its face in the middle: the bend is the centre itself, in the middle of the block. */
    @Test
    void bendHappensInTheMiddle() {
        int faces = DuctPose.faces(FLOOR, WEST | EAST, EAST, new int[6]);
        Box centre = DuctShapes.center(2, FLOOR, faces, radii(2, WEST | EAST));
        assertBox(new Box(6, LIFT, 6, 10, 10, 10), centre);
        assertBox(new Box(0, LIFT, 6, 6, LIFT + 4, 10), DuctShapes.arm(DuctShapes.WEST, 2, centre, faces));
        assertBox(new Box(10, 6, 6, 16, 10, 10), DuctShapes.arm(DuctShapes.EAST, 2, centre, faces));
    }

    @Test
    void centreDropsToALowFace() {
        Box centre = DuctShapes.center(4, DuctPose.CENTRED, EAST, radii(2, EAST | WEST));
        assertBox(new Box(4, LIFT, 4, 12, 12, 12), centre);
        assertBox(new Box(12, LIFT, 6, 16, LIFT + 4, 10), DuctShapes.arm(DuctShapes.EAST, 2, centre, EAST));
    }

    @Test
    void verticalArmsStartAtTheCentre() {
        int draped = DuctPose.of(Height.LOW, Rank.DRAPED);
        Box centre = DuctShapes.center(2, draped, 63, radii(2, 1 << DuctShapes.UP | 1 << DuctShapes.DOWN));
        assertEquals(new Box(6, LIFT + 4, 6, 10, 16, 10), DuctShapes.arm(DuctShapes.UP, 2, centre, 63));
        assertEquals(new Box(6, 0, 6, 10, LIFT, 10), DuctShapes.arm(DuctShapes.DOWN, 2, centre, 63));
    }

    /** The low bend's arm up and the centred duct's arm down above it read as one straight vertical arm. */
    @Test
    void lowBendArmUpJoinsCentredArmAbove() {
        for (int centre = 2; centre <= 5; centre++) {
            for (int armRadius = 2; armRadius <= Math.min(4, centre); armRadius++) {
                Box bend = DuctShapes.center(centre, FLOOR, facesLike(FLOOR), radii(armRadius, WEST | 1 << DuctShapes.UP));
                Box up = DuctShapes.arm(DuctShapes.UP, armRadius, bend, facesLike(FLOOR));
                Box down = DuctShapes.arm(DuctShapes.DOWN, armRadius, DuctShapes.center(armRadius), 0);
                String radii = "radii " + centre + "/" + armRadius;
                assertEquals(LIFT + 2 * centre, up.y0(), 1e-4, radii + ": the arm up starts on the low centre's top");
                assertEquals(16, up.y1(), radii + ": the arm up reaches the top face");
                assertEquals(0, down.y0(), radii + ": the arm down above starts on the shared face");
                for (int axis = DuctPose.X; axis <= DuctPose.Z; axis += 2) {
                    assertEquals(min(up, axis), min(down, axis), radii + ": no offset across on axis " + axis);
                    assertEquals(max(up, axis), max(down, axis), radii + ": no offset across on axis " + axis);
                }
            }
        }
    }

    @Test
    void armRadiiCountTheLinesPerSide() {
        int[] radii = DuctShapes.armRadii(new int[] {EAST | WEST, EAST, 0});
        assertEquals(DuctShapes.THICK_RADIUS, radii[DuctShapes.EAST]);
        assertEquals(DuctShapes.THIN_RADIUS, radii[DuctShapes.WEST]);
        assertEquals(0, radii[DuctShapes.UP]);
    }

    @Test
    void segmentsTouchButDoNotOverlap() {
        int[] poses = {DuctPose.CENTRED, FLOOR, DuctPose.of(Height.LOW, Rank.DRAPED), DuctPose.of(Height.MID, Rank.ANCHORED)};
        for (int pose : poses) {
            for (int faces = 0; faces < 64; faces++) {
                for (int centre = 2; centre <= 5; centre++) {
                    for (int armRadius = 2; armRadius <= Math.min(4, centre); armRadius++) {
                        int[] radii = radii(armRadius, 63);
                        Box centreBox = DuctShapes.center(centre, pose, faces, radii);
                        List<Box> boxes = new ArrayList<>();
                        boxes.add(centreBox);
                        for (int side = 0; side < 6; side++) {
                            boxes.add(DuctShapes.arm(side, armRadius, centreBox, faces));
                        }
                        assertNoSharedPlanes(boxes, "pose " + pose + " faces " + faces + " radii " + centre + "/" + armRadius);
                    }
                }
            }
        }
    }

    private static void assertBox(Box expected, Box actual) {
        for (int axis = 0; axis < 3; axis++) {
            assertEquals(min(expected, axis), min(actual, axis), 1e-4, "min on axis " + axis + " of " + actual);
            assertEquals(max(expected, axis), max(actual, axis), 1e-4, "max on axis " + axis + " of " + actual);
        }
    }

    /** No two boxes may show same facing faces in one plane over a common area. */
    private static void assertNoSharedPlanes(List<Box> boxes, String context) {
        for (int i = 0; i < boxes.size(); i++) {
            for (int j = i + 1; j < boxes.size(); j++) {
                Box a = boxes.get(i);
                Box b = boxes.get(j);
                for (int axis = 0; axis < 3; axis++) {
                    boolean shared = min(a, axis) == min(b, axis) || max(a, axis) == max(b, axis);
                    assertFalse(shared && overlap(a, b, axis), context + ": " + a + " and " + b + " share a face plane on axis " + axis);
                }
            }
        }
    }

    /** Whether the boxes overlap with positive extent on both axes other than {@code skip}. */
    private static boolean overlap(Box a, Box b, int skip) {
        for (int axis = 0; axis < 3; axis++) {
            if (axis != skip && Math.min(max(a, axis), max(b, axis)) <= Math.max(min(a, axis), min(b, axis))) {
                return false;
            }
        }
        return true;
    }
}
