package io.github.dirnei.mc.servus.delicacies.meat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoafSlicesTest {

    @Test
    void anUncutLoafHasEverySlice() {
        assertEquals(LoafSlices.SLICES, LoafSlices.left(0));
        assertEquals(1, LoafSlices.left(LoafSlices.MAX_CUT));
    }

    @Test
    void onlyTheLastCutRemovesTheLoaf() {
        for (int cut = 0; cut < LoafSlices.MAX_CUT; cut++) {
            assertFalse(LoafSlices.isLastSlice(cut), "cut " + cut);
            assertEquals(cut + 1, LoafSlices.next(cut));
        }
        assertTrue(LoafSlices.isLastSlice(LoafSlices.MAX_CUT));
        assertThrows(IllegalArgumentException.class, () -> LoafSlices.next(LoafSlices.MAX_CUT));
    }

    @Test
    void slicesComeOffTheWestEnd() {
        assertArrayEquals(new float[] {3, 0, 5, 13, 5, 11}, LoafSlices.box(0));
        for (int cut = 1; cut <= LoafSlices.MAX_CUT; cut++) {
            float[] before = LoafSlices.box(cut - 1);
            float[] after = LoafSlices.box(cut);
            assertEquals(before[0] + LoafSlices.SLICE_WIDTH, after[0], 1e-6);
            assertEquals(before[3], after[3], 1e-6);
        }
        float[] last = LoafSlices.box(LoafSlices.MAX_CUT);
        assertEquals(LoafSlices.SLICE_WIDTH, last[3] - last[0], 1e-6);
    }

    @Test
    void cutsOutsideTheRangeAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> LoafSlices.left(-1));
        assertThrows(IllegalArgumentException.class, () -> LoafSlices.box(LoafSlices.SLICES));
        assertThrows(IllegalArgumentException.class, () -> LoafSlices.isLastSlice(LoafSlices.SLICES));
    }
}
