package io.github.dirnei.mc.servus.delicacies.crock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CrockBatchTest {

    @Test
    void limitedByAnIngredient() {
        assertEquals(1, CrockBatch.count(new int[] {6, 4}, new int[] {4, 1}, 0, 0, 64, 4));
    }

    @Test
    void everyFullBatch() {
        assertEquals(2, CrockBatch.count(new int[] {8, 2}, new int[] {4, 1}, 0, 0, 64, 4));
    }

    @Test
    void limitedByTheFluid() {
        assertEquals(2, CrockBatch.count(new int[] {5}, new int[] {1}, 2500, 1000, 64, 4));
    }

    @Test
    void limitedByTheOutputRoom() {
        assertEquals(3, CrockBatch.count(new int[] {64, 64}, new int[] {4, 1}, 0, 0, 13, 4));
    }

    @Test
    void noFluidNeededIgnoresTheTank() {
        assertEquals(1, CrockBatch.count(new int[] {4}, new int[] {4}, 0, 0, 64, 1));
    }

    @Test
    void zeroWhenNothingFits() {
        assertEquals(0, CrockBatch.count(new int[] {3}, new int[] {4}, 0, 0, 64, 4));
        assertEquals(0, CrockBatch.count(new int[] {4}, new int[] {4}, 0, 0, 3, 4));
        assertEquals(0, CrockBatch.count(new int[] {}, new int[] {}, 500, 1000, 64, 4));
    }
}
