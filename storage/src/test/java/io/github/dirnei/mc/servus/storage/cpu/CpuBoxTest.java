package io.github.dirnei.mc.servus.storage.cpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CpuBoxTest {

    private static Set<CpuShape.Vec> box(int sx, int sy, int sz) {
        Set<CpuShape.Vec> set = new HashSet<>();
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    set.add(new CpuShape.Vec(10 + x, 64 + y, -5 + z));
                }
            }
        }
        return set;
    }

    private static CpuShape.Vec at(int x, int y, int z) {
        return new CpuShape.Vec(10 + x, 64 + y, -5 + z);
    }

    @Test
    void smallestBoxForms() {
        Set<CpuShape.Vec> joined = box(2, 2, 2);
        CpuBox.Result result = CpuBox.check(joined, joined, 512);
        assertTrue(result.formed());
        assertEquals(at(0, 0, 0), result.box().corner());
        assertEquals(new CpuShape.Vec(2, 2, 2), result.box().size());
        assertEquals(8, result.box().cpuBlocks());
        assertEquals(0, result.box().bufferBlocks());
    }

    @Test
    void boxWithBuffersCounts() {
        Set<CpuShape.Vec> joined = box(3, 2, 2);
        Set<CpuShape.Vec> cpus = new HashSet<>();
        cpus.add(at(0, 0, 0));
        cpus.add(at(1, 1, 1));
        cpus.add(at(2, 0, 1));
        cpus.add(at(1, 0, 0));
        CpuBox.Result result = CpuBox.check(joined, cpus, 512);
        assertTrue(result.formed());
        assertEquals(4, result.box().cpuBlocks());
        assertEquals(8, result.box().bufferBlocks());
        assertEquals(12, result.box().blocks());
    }

    @Test
    void cubeForms() {
        Set<CpuShape.Vec> joined = box(3, 3, 3);
        CpuBox.Result result = CpuBox.check(joined, joined, 512);
        assertTrue(result.formed());
        assertEquals(27, result.box().cpuBlocks());
    }

    @Test
    void flatIsTooSmall() {
        Set<CpuShape.Vec> joined = box(2, 1, 2);
        assertEquals(CpuBox.Reason.TOO_SMALL, CpuBox.check(joined, joined, 512).reason());
    }

    @Test
    void lineIsTooSmall() {
        Set<CpuShape.Vec> joined = box(4, 1, 1);
        assertEquals(CpuBox.Reason.TOO_SMALL, CpuBox.check(joined, joined, 512).reason());
    }

    @Test
    void lShapeIsNotABox() {
        Set<CpuShape.Vec> joined = box(3, 2, 2);
        joined.remove(at(2, 1, 0));
        joined.remove(at(2, 1, 1));
        CpuBox.Result result = CpuBox.check(joined, joined, 512);
        assertFalse(result.formed());
        assertEquals(CpuBox.Reason.NOT_A_BOX, result.reason());
    }

    @Test
    void missingCornerIsNotABox() {
        Set<CpuShape.Vec> joined = box(3, 3, 3);
        joined.remove(at(2, 2, 2));
        assertEquals(CpuBox.Reason.NOT_A_BOX, CpuBox.check(joined, joined, 512).reason());
    }

    @Test
    void missingMiddleIsAGap() {
        Set<CpuShape.Vec> joined = box(3, 3, 3);
        joined.remove(at(1, 1, 1));
        CpuBox.Result result = CpuBox.check(joined, joined, 512);
        assertEquals(CpuBox.Reason.GAP, result.reason());
        assertEquals(at(1, 1, 1), result.gap());
    }

    @Test
    void twoGapsReportTheLowest() {
        Set<CpuShape.Vec> joined = box(3, 3, 3);
        joined.remove(at(1, 2, 1));
        joined.remove(at(1, 1, 1));
        joined.remove(at(1, 1, 0));
        CpuBox.Result result = CpuBox.check(joined, joined, 512);
        assertEquals(CpuBox.Reason.GAP, result.reason());
        assertEquals(at(1, 1, 0), result.gap());
    }

    @Test
    void onlyBuffersHaveNoCpuBlock() {
        Set<CpuShape.Vec> joined = box(2, 2, 2);
        assertEquals(CpuBox.Reason.NO_CPU_BLOCK, CpuBox.check(joined, Set.of(), 512).reason());
    }

    @Test
    void tooBig() {
        Set<CpuShape.Vec> joined = box(8, 8, 8);
        joined.add(at(8, 0, 0));
        assertEquals(513, joined.size());
        assertEquals(CpuBox.Reason.TOO_BIG, CpuBox.check(joined, joined, 512).reason());
    }

    @Test
    void reasonsInOrder() {
        Set<CpuShape.Vec> flatBuffers = box(2, 1, 2);
        assertEquals(CpuBox.Reason.TOO_SMALL, CpuBox.check(flatBuffers, Set.of(), 512).reason());
        Set<CpuShape.Vec> holedBuffers = box(3, 3, 3);
        holedBuffers.remove(at(1, 1, 1));
        assertEquals(CpuBox.Reason.GAP, CpuBox.check(holedBuffers, Set.of(), 512).reason());
        Set<CpuShape.Vec> big = box(3, 3, 3);
        assertEquals(CpuBox.Reason.TOO_BIG, CpuBox.check(big, Set.of(), 26).reason());
    }

    @Test
    void placeClassesOfASmallestBox() {
        CpuShape.Vec size = new CpuShape.Vec(2, 2, 2);
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    int part = CpuBox.part(new CpuShape.Vec(x, y, z), size);
                    assertEquals(CpuShape.part(x * 2, y * 2, z * 2), part);
                }
            }
        }
    }

    @Test
    void placeClassesOfA4x3x2Box() {
        CpuShape.Vec size = new CpuShape.Vec(4, 3, 2);
        assertEquals(CpuShape.part(0, 0, 0), CpuBox.part(new CpuShape.Vec(0, 0, 0), size));
        assertEquals(CpuShape.part(1, 0, 0), CpuBox.part(new CpuShape.Vec(1, 0, 0), size));
        assertEquals(CpuShape.part(1, 0, 0), CpuBox.part(new CpuShape.Vec(2, 0, 0), size));
        assertEquals(CpuShape.part(2, 0, 0), CpuBox.part(new CpuShape.Vec(3, 0, 0), size));
        assertEquals(CpuShape.part(1, 1, 2), CpuBox.part(new CpuShape.Vec(2, 1, 1), size));
        assertEquals(CpuShape.part(2, 2, 2), CpuBox.part(new CpuShape.Vec(3, 2, 1), size));
        assertEquals(CpuShape.part(0, 2, 0), CpuBox.part(new CpuShape.Vec(0, 2, 0), size));
    }

    @Test
    void onlyTheCornerIsPartZero() {
        CpuShape.Vec size = new CpuShape.Vec(2, 3, 4);
        int zeros = 0;
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 4; z++) {
                    if (CpuBox.part(new CpuShape.Vec(x, y, z), size) == 0) {
                        zeros++;
                        assertEquals(new CpuShape.Vec(0, 0, 0), new CpuShape.Vec(x, y, z));
                    }
                }
            }
        }
        assertEquals(1, zeros);
    }
}
