package io.github.dirnei.mc.servus.storage.rail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.storage.rail.RailLook.Box;
import io.github.dirnei.mc.servus.storage.rail.RailLook.Part;
import io.github.dirnei.mc.servus.storage.rail.RailLook.Piece;
import io.github.dirnei.mc.servus.storage.rail.RailLook.Placed;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RailLookTest {

    @Test
    void pieceForEveryMask() {
        assertEquals(new Placed(Piece.SINGLE, 0), RailLook.of(false, false, false, false));
        assertEquals(new Placed(Piece.END, 0), RailLook.of(true, false, false, false));
        assertEquals(new Placed(Piece.END, 90), RailLook.of(false, true, false, false));
        assertEquals(new Placed(Piece.END, 180), RailLook.of(false, false, true, false));
        assertEquals(new Placed(Piece.END, 270), RailLook.of(false, false, false, true));
        assertEquals(new Placed(Piece.STRAIGHT, 0), RailLook.of(true, false, true, false));
        assertEquals(new Placed(Piece.STRAIGHT, 90), RailLook.of(false, true, false, true));
        assertEquals(new Placed(Piece.CURVE, 0), RailLook.of(true, true, false, false));
        assertEquals(new Placed(Piece.CURVE, 90), RailLook.of(false, true, true, false));
        assertEquals(new Placed(Piece.CURVE, 180), RailLook.of(false, false, true, true));
        assertEquals(new Placed(Piece.CURVE, 270), RailLook.of(true, false, false, true));
        for (int mask = 0; mask < 16; mask++) {
            int sides = Integer.bitCount(mask);
            Placed placed = RailLook.of((mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0, (mask & 8) != 0);
            if (sides >= 3) {
                assertEquals(Piece.CROSS, placed.piece(), "mask " + mask);
            }
            assertEquals(0, placed.rotation() % 90, "mask " + mask);
        }
    }

    @Test
    void rotationRoundTrip() {
        Box box = new Box(Part.HEAD, 2, 1, 0, 4, 2, 5);
        assertEquals(box, box.rotated(90).rotated(270));
        assertEquals(box, box.rotated(360));
        // A quarter turn takes the north edge to the east edge.
        Box east = box.rotated(90);
        assertEquals(new Box(Part.HEAD, 11, 1, 2, 16, 2, 4), east);
        assertEquals(new Box(Part.HEAD, 12, 1, 11, 14, 2, 16), box.rotated(180));
    }

    @Test
    void straightGaugeAndSleepers() {
        List<Box> boxes = RailLook.boxes(Piece.STRAIGHT);
        List<Box> heads = of(boxes, Part.HEAD);
        assertEquals(List.of(new Box(Part.HEAD, 2, 1, 0, 4, 2, 16), new Box(Part.HEAD, 12, 1, 0, 14, 2, 16)), heads);
        List<Box> sleepers = of(boxes, Part.SLEEPER);
        assertEquals(4, sleepers.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(new Box(Part.SLEEPER, 1, 0, 1 + 4 * i, 15, 1, 3 + 4 * i), sleepers.get(i));
        }
        assertEquals(3, of(RailLook.boxes(Piece.END), Part.SLEEPER).size());
        assertEquals(2, of(RailLook.boxes(Piece.SINGLE), Part.SLEEPER).size());
    }

    @Test
    void curveMeetsEdgesAtGauge() {
        List<Integer> north = new ArrayList<>();
        List<Integer> east = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            if (RailLook.curveHead(i, 0)) {
                north.add(i);
            }
            if (RailLook.curveHead(15, i)) {
                east.add(i);
            }
        }
        assertEquals(List.of(2, 3, 12, 13), north);
        assertEquals(List.of(2, 3, 12, 13), east);
        // Nothing reaches the south and west edges.
        for (int i = 0; i < 16; i++) {
            assertFalse(RailLook.curveHead(i, 15) || RailLook.curveSleeper(i, 15), "south edge " + i);
            assertFalse(RailLook.curveHead(0, i) || RailLook.curveSleeper(0, i), "west edge " + i);
        }
    }

    @Test
    void curveIsSymmetric() {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                assertEquals(RailLook.curveHead(x, z), RailLook.curveHead(15 - z, 15 - x), "head " + x + "," + z);
                assertEquals(RailLook.curveSleeper(x, z), RailLook.curveSleeper(15 - z, 15 - x), "sleeper " + x + "," + z);
            }
        }
        long sleepers = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                sleepers += RailLook.curveSleeper(x, z) ? 1 : 0;
            }
        }
        assertTrue(sleepers > 20, "the curve has sleepers, got " + sleepers);
    }

    @Test
    void crossHeadsDoNotOverlap() {
        List<Box> heads = of(RailLook.boxes(Piece.CROSS), Part.HEAD);
        for (int i = 0; i < heads.size(); i++) {
            for (int j = i + 1; j < heads.size(); j++) {
                Box a = heads.get(i);
                Box b = heads.get(j);
                boolean overlap = a.x0() < b.x1() && b.x0() < a.x1() && a.z0() < b.z1() && b.z0() < a.z1();
                assertFalse(overlap, a + " overlaps " + b);
            }
        }
        // Both directions reach every edge.
        assertTrue(heads.stream().anyMatch(box -> box.x0() == 0 && box.z0() == 2));
        assertTrue(heads.stream().anyMatch(box -> box.x1() == 16 && box.z0() == 12));
        assertTrue(heads.stream().anyMatch(box -> box.z0() == 0 && box.x0() == 2));
    }

    @Test
    void boxesStayInsideBlock() {
        List<Box> all = new ArrayList<>();
        for (Piece piece : Piece.values()) {
            for (int rotation = 0; rotation < 360; rotation += 90) {
                all.addAll(RailLook.boxes(new Placed(piece, rotation)));
            }
        }
        all.addAll(RailLook.dock());
        all.addAll(RailLook.dockInline());
        for (Box box : all) {
            assertTrue(box.x0() >= 0 && box.z0() >= 0 && box.x1() <= 16 && box.z1() <= 16, box.toString());
            assertTrue(box.x0() < box.x1() && box.y0() < box.y1() && box.z0() < box.z1(), box.toString());
            double top = box.part() == Part.DOCK ? RailLook.DOCK_TOP : RailLook.HEAD_TOP;
            assertTrue(box.y0() >= 0 && box.y1() <= top, box.toString());
        }
    }

    @Test
    void dockInlineReachesBeam() {
        double beam = RailLook.dock().get(0).z1();
        List<Box> heads = of(RailLook.dockInline(), Part.HEAD);
        assertEquals(2, heads.size());
        for (Box head : heads) {
            assertEquals(beam, head.z0());
            assertEquals(4.0, head.z1());
        }
        // The end piece turned away from the dock starts where the inline track stops.
        Box endHead = of(RailLook.boxes(new Placed(Piece.END, 180)), Part.HEAD).get(0);
        assertEquals(4.0, endHead.z0());
    }

    private static List<Box> of(List<Box> boxes, Part part) {
        return boxes.stream().filter(box -> box.part() == part).toList();
    }
}
