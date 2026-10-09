package io.github.dirnei.mc.servus.foundation.machine.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.machine.sink.SinkLayout.Vec;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SinkLayoutTest {

    /** North, east, south, west as front unit vectors. */
    private static final int[][] FACINGS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    @Test
    void offsetsFacingNorth() {
        // Facing north: right is west (-x), back is south (+z).
        assertEquals(new Vec(0, 0, 0), SinkLayout.offset(0, 0, -1));
        assertEquals(new Vec(-1, 0, 0), SinkLayout.offset(1, 0, -1));
        assertEquals(new Vec(0, 1, 0), SinkLayout.offset(2, 0, -1));
        assertEquals(new Vec(-1, 1, 0), SinkLayout.offset(3, 0, -1));
        assertEquals(new Vec(0, 0, 1), SinkLayout.offset(4, 0, -1));
        assertEquals(new Vec(-1, 1, 1), SinkLayout.offset(7, 0, -1));
    }

    @Test
    void offsetsFacingEastSouthWest() {
        // East: right is north (-z), back is west (-x).
        assertEquals(new Vec(0, 0, -1), SinkLayout.offset(1, 1, 0));
        assertEquals(new Vec(-1, 0, 0), SinkLayout.offset(4, 1, 0));
        // South: right is east (+x), back is north (-z).
        assertEquals(new Vec(1, 0, 0), SinkLayout.offset(1, 0, 1));
        assertEquals(new Vec(0, 0, -1), SinkLayout.offset(4, 0, 1));
        // West: right is south (+z), back is east (+x).
        assertEquals(new Vec(0, 0, 1), SinkLayout.offset(1, -1, 0));
        assertEquals(new Vec(1, 0, 0), SinkLayout.offset(4, -1, 0));
        assertEquals(new Vec(1, 1, 1), SinkLayout.offset(7, -1, 0));
    }

    @Test
    void partsFillACubeForEveryFacing() {
        for (int[] f : FACINGS) {
            Set<Vec> seen = new HashSet<>();
            for (int part = 0; part < SinkLayout.PARTS; part++) {
                Vec offset = SinkLayout.offset(part, f[0], f[1]);
                assertTrue(Math.abs(offset.x()) <= 1 && Math.abs(offset.z()) <= 1 && offset.y() >= 0 && offset.y() <= 1);
                seen.add(offset);
            }
            assertEquals(8, seen.size(), "eight distinct blocks");
        }
    }

    @Test
    void controllerOfInvertsOffset() {
        for (int[] f : FACINGS) {
            for (int part = 0; part < SinkLayout.PARTS; part++) {
                Vec offset = SinkLayout.offset(part, f[0], f[1]);
                Vec at = new Vec(10, 64, -7).plus(offset);
                assertEquals(new Vec(10, 64, -7), SinkLayout.controllerOf(at.x(), at.y(), at.z(), f[0], f[1], part));
            }
        }
    }

    @Test
    void candidatesPutTheClickedBlockAtEachFrontPart() {
        for (int[] f : FACINGS) {
            List<Vec> candidates = SinkLayout.candidates(3, 5, 7, f[0], f[1]);
            assertEquals(4, candidates.size());
            for (int i = 0; i < 4; i++) {
                int part = SinkLayout.frontParts().get(i);
                assertEquals(new Vec(3, 5, 7), candidates.get(i).plus(SinkLayout.offset(part, f[0], f[1])));
            }
            assertEquals(4, new HashSet<>(candidates).size(), "four different cubes");
        }
    }

    @Test
    void cubeLiesBehindTheClickedFace() {
        for (int[] f : FACINGS) {
            for (Vec controller : SinkLayout.candidates(0, 0, 0, f[0], f[1])) {
                for (int part = 0; part < SinkLayout.PARTS; part++) {
                    Vec block = controller.plus(SinkLayout.offset(part, f[0], f[1]));
                    int towardsFront = block.x() * f[0] + block.z() * f[1];
                    assertTrue(towardsFront <= 0, "no block in front of the clicked face");
                    if (SinkLayout.isFront(part)) {
                        assertEquals(0, towardsFront, "front parts share the clicked block's front plane");
                    } else {
                        assertEquals(-1, towardsFront, "back parts lie one block behind");
                    }
                }
            }
        }
    }

    @Test
    void topAndFrontParts() {
        assertEquals(List.of(2, 3, 6, 7), SinkLayout.topParts());
        assertEquals(List.of(0, 1, 2, 3), SinkLayout.frontParts());
        for (int part = 0; part < SinkLayout.PARTS; part++) {
            assertEquals(SinkLayout.topParts().contains(part), SinkLayout.isTop(part));
            assertEquals(SinkLayout.frontParts().contains(part), SinkLayout.isFront(part));
        }
        assertTrue(SinkLayout.isFront(SinkLayout.CONTROLLER));
        assertFalse(SinkLayout.isTop(SinkLayout.CONTROLLER));
    }

    @Test
    void rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> SinkLayout.offset(8, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> SinkLayout.offset(0, 1, 1));
    }
}
