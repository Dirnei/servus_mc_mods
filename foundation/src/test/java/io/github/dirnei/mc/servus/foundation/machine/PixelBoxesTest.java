package io.github.dirnei.mc.servus.foundation.machine;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class PixelBoxesTest {

    @Test
    void quarterTurnMovesNorthToEast() {
        float[] northEdge = {6, 0, 0, 10, 4, 2};
        assertArrayEquals(new float[] {14, 0, 6, 16, 4, 10}, PixelBoxes.rotated(northEdge, 1));
    }

    @Test
    void fourQuartersAreTheSameBox() {
        float[] box = {1, 2, 3, 4, 5, 6};
        assertArrayEquals(box, PixelBoxes.rotated(box, 4));
    }

    @Test
    void pointTurnsLikeTheBox() {
        assertArrayEquals(new float[] {16, 8}, PixelBoxes.rotatedPoint(8, 0, 1));
        assertArrayEquals(new float[] {8, 16}, PixelBoxes.rotatedPoint(8, 0, 2));
    }
}
