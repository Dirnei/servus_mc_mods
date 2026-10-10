package io.github.dirnei.mc.servus.foundation.machine.pump;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class BurnerPumpPartsTest {

    @Test
    void quarterTurnMovesNorthToEast() {
        float[] northEdge = {6, 0, 0, 10, 4, 2};
        assertArrayEquals(new float[] {14, 0, 6, 16, 4, 10}, BurnerPumpParts.rotated(northEdge, 1));
    }

    @Test
    void fourQuartersAreTheSameBox() {
        assertArrayEquals(BurnerPumpParts.CHIMNEY, BurnerPumpParts.rotated(BurnerPumpParts.CHIMNEY, 4));
    }

    @Test
    void pointTurnsLikeTheBox() {
        assertArrayEquals(new float[] {16, 8}, BurnerPumpParts.rotatedPoint(8, 0, 1));
        assertArrayEquals(new float[] {8, 16}, BurnerPumpParts.rotatedPoint(8, 0, 2));
    }

    /** The west stub of the largest size, x 0..3 and y, z 5..11, must not touch the rod or its socket. */
    @Test
    void rodStaysClearOfTheSideStub() {
        float[] stub = {0, 5, 5, 3, 11, 11};
        assertFalse(overlaps(stub, BurnerPumpParts.ROD), "rod");
        assertFalse(overlaps(stub, BurnerPumpParts.SOCKET), "socket");
    }

    private static boolean overlaps(float[] a, float[] b) {
        return a[0] < b[3] && b[0] < a[3] && a[1] < b[4] && b[1] < a[4] && a[2] < b[5] && b[2] < a[5];
    }
}
