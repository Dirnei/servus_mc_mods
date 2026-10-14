package io.github.dirnei.mc.servus.foundation.logistics.duct;

import static io.github.dirnei.mc.servus.foundation.logistics.duct.DuctPose.X;
import static io.github.dirnei.mc.servus.foundation.logistics.duct.DuctPose.Y;
import static io.github.dirnei.mc.servus.foundation.logistics.duct.DuctPose.Z;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctPose.Rank;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctPose.Side;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctShapes.Box;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DuctShapesPoseTest {

    private static final float LIFT = DuctShapes.LIFT;
    /** Inset of a step on an arm along X. */
    private static final float I = DuctShapes.STEP_INSET;
    private static final int EAST = 1 << DuctShapes.EAST;
    private static final int FLOOR = DuctPose.with(DuctPose.CENTRED, Y, Side.NEG, Rank.ANCHORED);

    /** Faces of a duct whose every arm side meets the neighbour at {@code pose}. */
    private static int facesLike(int pose) {
        return DuctPose.faces(pose, 63, 0, new int[] {pose, pose, pose, pose, pose, pose});
    }

    @Test
    void middlePoseMatchesOldBoxes() {
        assertEquals(new Box(6, 6, 6, 10, 10, 10), DuctShapes.center(2, DuctPose.CENTRED));
        assertEquals(List.of(new Box(6, 6, 0, 10, 10, 6)), DuctShapes.arm(DuctShapes.NORTH, 2, 2, DuctPose.CENTRED, 0));
        assertEquals(List.of(new Box(11, 5, 5, 16, 11, 11)), DuctShapes.arm(DuctShapes.EAST, 3, 3, DuctPose.CENTRED, 0));
        assertEquals(List.of(new Box(6, 0, 6, 10, 6, 10)), DuctShapes.arm(DuctShapes.DOWN, 2, 2, DuctPose.CENTRED, 0));
        assertEquals(List.of(new Box(6, 6, 12, 10, 10, 16)), DuctShapes.arm(DuctShapes.SOUTH, 2, 4, DuctPose.CENTRED, 0),
                "a thin arm starts at a thick centre's face");
    }

    @Test
    void floorCentreSitsOnLift() {
        assertEquals(new Box(6, LIFT, 6, 10, LIFT + 4, 10), DuctShapes.center(2, FLOOR));
        int corner = DuctPose.with(DuctPose.with(DuctPose.CENTRED, X, Side.POS, Rank.ANCHORED), Z, Side.NEG, Rank.ANCHORED);
        assertEquals(new Box(16 - LIFT - 8, 4, LIFT, 16 - LIFT, 12, LIFT + 8), DuctShapes.center(4, corner));
    }

    @Test
    void armStretchesToMovedCentre() {
        int faces = facesLike(FLOOR);
        assertEquals(List.of(new Box(10, LIFT, 6, 16, LIFT + 4, 10)), DuctShapes.arm(DuctShapes.EAST, 2, 2, FLOOR, faces));
        assertEquals(List.of(new Box(6, LIFT + 4, 6, 10, 16, 10)), DuctShapes.arm(DuctShapes.UP, 2, 2, FLOOR, faces),
                "the arm up starts at the low centre's top");
    }

    @Test
    void endpointArmStepsToMiddle() {
        int faces = DuctPose.faces(FLOOR, EAST, EAST, new int[6]);
        DuctShapes.ArmBoxes arm = DuctShapes.armBoxes(DuctShapes.EAST, 2, 2, FLOOR, faces);
        assertNotNull(arm.step());
        assertBox(new Box(14, 6 + I, 6 + I, 16, 10 - I, 10 - I), arm.segment());
    }

    @Test
    void stepBoxSpansBothCrossSections() {
        int faces = DuctPose.faces(FLOOR, EAST, EAST, new int[6]);
        Box step = DuctShapes.armBoxes(DuctShapes.EAST, 2, 2, FLOOR, faces).step();
        assertBox(new Box(10, LIFT + I, 6 + I, 14, 10 - I, 10 - I), step);
    }

    @Test
    void twoAxisStepIsOneBox() {
        int pose = DuctPose.with(FLOOR, Z, Side.POS, Rank.ANCHORED);
        int faces = DuctPose.faces(pose, EAST, EAST, new int[6]);
        List<Box> boxes = DuctShapes.arm(DuctShapes.EAST, 2, 2, pose, faces);
        assertEquals(2, boxes.size());
        assertBox(new Box(10, LIFT + I, 6 + I, 14, 10 - I, 16 - LIFT - I), boxes.get(0));
    }

    @Test
    void segmentsTouchButDoNotOverlap() {
        int[] poses = {
                DuctPose.CENTRED,
                FLOOR,
                DuctPose.with(FLOOR, X, Side.POS, Rank.ANCHORED),
                DuctPose.with(DuctPose.with(DuctPose.CENTRED, X, Side.NEG, Rank.ANCHORED), Z, Side.POS, Rank.ANCHORED)};
        for (int pose : poses) {
            for (int faces : new int[] {facesLike(DuctPose.CENTRED), facesLike(FLOOR), facesLike(pose), 0}) {
                for (int centre = 2; centre <= 5; centre++) {
                    for (int armRadius = 2; armRadius <= Math.min(4, centre); armRadius++) {
                        List<Box> boxes = new ArrayList<>();
                        boxes.add(DuctShapes.center(centre, pose));
                        for (int side = 0; side < 6; side++) {
                            boxes.addAll(DuctShapes.arm(side, armRadius, centre, pose, faces));
                        }
                        assertNoSharedPlanes(boxes, "pose " + pose + " faces " + faces + " radii " + centre + "/" + armRadius);
                    }
                }
            }
        }
    }

    @Test
    void thickCentreArmStepFitsRoom() {
        int faces = facesLike(FLOOR);
        DuctShapes.ArmBoxes arm = DuctShapes.armBoxes(DuctShapes.EAST, 2, 4, DuctPose.CENTRED, faces);
        assertEquals(12, arm.step().x0());
        assertEquals(16 - DuctShapes.RING_DEPTH, arm.step().x1(), "the step leaves the ring depth free");
        assertBox(new Box(16 - DuctShapes.RING_DEPTH, LIFT + I, 6 + I, 16, LIFT + 4 - I, 10 - I), arm.segment());
    }

    @Test
    void flushCentreStepFillsTheGap() {
        int pose = DuctPose.with(FLOOR, X, Side.POS, Rank.DRAPED);
        int faces = DuctPose.faces(pose, EAST, EAST, new int[6]);
        DuctShapes.ArmBoxes arm = DuctShapes.armBoxes(DuctShapes.EAST, 2, 2, pose, faces);
        assertBox(new Box(16 - LIFT, LIFT + I, 6 + I, 16, 10 - I, 10 - I), arm.step());
        assertNull(arm.segment());
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

    private static float min(Box box, int axis) {
        return switch (axis) {
            case 0 -> box.x0();
            case 1 -> box.y0();
            default -> box.z0();
        };
    }

    private static float max(Box box, int axis) {
        return switch (axis) {
            case 0 -> box.x1();
            case 1 -> box.y1();
            default -> box.z1();
        };
    }
}
