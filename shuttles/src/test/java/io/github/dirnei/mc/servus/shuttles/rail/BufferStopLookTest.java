package io.github.dirnei.mc.servus.shuttles.rail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.rail.RailLook.Box;
import io.github.dirnei.mc.servus.shuttles.rail.RailLook.Part;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BufferStopLookTest {

    private static final int[] ROTATIONS = {0, 90, 180, 270};

    @Test
    void boxesStayInsideBlock() {
        for (int rotation : ROTATIONS) {
            for (Box box : BufferStopLook.boxes(rotation)) {
                assertTrue(box.x0() >= 0 && box.y0() >= 0 && box.z0() >= 0, box.toString());
                assertTrue(box.x1() <= 16 && box.y1() <= 16 && box.z1() <= 16, box.toString());
                assertTrue(box.x0() < box.x1() && box.y0() < box.y1() && box.z0() < box.z1(), box.toString());
            }
        }
    }

    @Test
    void noTwoBoxesShareVolume() {
        for (int rotation : ROTATIONS) {
            List<Box> boxes = BufferStopLook.boxes(rotation);
            for (int i = 0; i < boxes.size(); i++) {
                for (int j = i + 1; j < boxes.size(); j++) {
                    Box a = boxes.get(i);
                    Box b = boxes.get(j);
                    boolean overlap = overlaps(a.x0(), a.x1(), b.x0(), b.x1())
                            && overlaps(a.y0(), a.y1(), b.y0(), b.y1())
                            && overlaps(a.z0(), a.z1(), b.z0(), b.z1());
                    assertFalse(overlap, a + " overlaps " + b);
                }
            }
        }
    }

    @Test
    void noTwoSameFacingFacesShareAPlane() {
        for (int rotation : ROTATIONS) {
            List<Face> faces = new ArrayList<>();
            List<Box> boxes = BufferStopLook.boxes(rotation);
            for (int i = 0; i < boxes.size(); i++) {
                faces.addAll(Face.of(i, boxes.get(i)));
            }
            for (int i = 0; i < faces.size(); i++) {
                for (int j = i + 1; j < faces.size(); j++) {
                    Face a = faces.get(i);
                    Face b = faces.get(j);
                    boolean clash = a.box != b.box && a.axis == b.axis && a.positive == b.positive && a.plane == b.plane
                            && overlaps(a.u0, a.u1, b.u0, b.u1) && overlaps(a.v0, a.v1, b.v0, b.v1);
                    assertFalse(clash, "rotation " + rotation + ": " + boxes.get(a.box) + " and " + boxes.get(b.box)
                            + " share a face plane on axis " + a.axis + " at " + a.plane);
                }
            }
        }
    }

    @Test
    void stubHeadsRunOnTheRailHeadsUpToTheSole() {
        List<Box> heads = of(BufferStopLook.boxes(), Part.HEAD);
        assertEquals(2, heads.size());
        for (int i = 0; i < 2; i++) {
            Box head = heads.get(i);
            assertEquals(RailLook.HEADS[i][0], head.x0());
            assertEquals(RailLook.HEADS[i][1], head.x1());
            assertEquals(RailLook.SLEEPER_TOP, head.y0());
            assertEquals(RailLook.HEAD_TOP, head.y1());
            assertEquals(0.0, head.z0());
            assertEquals(BufferStopLook.SOLE_FROM, head.z1());
        }
        Box sole = of(BufferStopLook.boxes(), Part.FRAME).get(0);
        assertEquals(BufferStopLook.SOLE_FROM, sole.z0());
        assertEquals(0.0, sole.y0());
    }

    @Test
    void bufferHeadsSitOnTheBeamFrontInLineWithTheRailHeads() {
        Box beam = of(BufferStopLook.boxes(), Part.DOCK).get(0);
        List<Box> buffers = of(BufferStopLook.boxes(), Part.FRAME).stream()
                .filter(box -> box.z1() == beam.z0()).toList();
        assertEquals(2, buffers.size());
        for (int i = 0; i < 2; i++) {
            Box buffer = buffers.get(i);
            assertTrue(buffer.x0() < RailLook.HEADS[i][0] && buffer.x1() > RailLook.HEADS[i][1], buffer.toString());
            assertTrue(buffer.y0() > beam.y0() && buffer.y1() < beam.y1(), buffer.toString());
        }
    }

    @Test
    void rotationTurnsTheFrontToTheEast() {
        Box head = of(BufferStopLook.boxes(90), Part.HEAD).get(0);
        assertEquals(16.0, head.x1());
        assertEquals(16 - BufferStopLook.SOLE_FROM, head.x0());
    }

    private static boolean overlaps(double a0, double a1, double b0, double b1) {
        return a0 < b1 && b0 < a1;
    }

    private static List<Box> of(List<Box> boxes, Part part) {
        return boxes.stream().filter(box -> box.part() == part).toList();
    }

    /** One face of a box: the axis it faces along, its side, its plane and its extent on the two other axes. */
    private record Face(int box, int axis, boolean positive, double plane, double u0, double u1, double v0, double v1) {

        static List<Face> of(int index, Box b) {
            return List.of(
                    new Face(index, 0, false, b.x0(), b.y0(), b.y1(), b.z0(), b.z1()),
                    new Face(index, 0, true, b.x1(), b.y0(), b.y1(), b.z0(), b.z1()),
                    new Face(index, 1, false, b.y0(), b.x0(), b.x1(), b.z0(), b.z1()),
                    new Face(index, 1, true, b.y1(), b.x0(), b.x1(), b.z0(), b.z1()),
                    new Face(index, 2, false, b.z0(), b.x0(), b.x1(), b.y0(), b.y1()),
                    new Face(index, 2, true, b.z1(), b.x0(), b.x1(), b.y0(), b.y1()));
        }
    }
}
