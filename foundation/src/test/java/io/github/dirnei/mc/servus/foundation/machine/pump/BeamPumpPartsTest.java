package io.github.dirnei.mc.servus.foundation.machine.pump;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class BeamPumpPartsTest {

    /** The west stub of the largest size, x 0..3 and y, z 5..11, must not touch the rod or its socket. */
    @Test
    void rodStaysClearOfTheSideStub() {
        float[] stub = {0, 5, 5, 3, 11, 11};
        assertFalse(overlaps(stub, BeamPumpParts.ROD), "rod");
        assertFalse(overlaps(stub, BeamPumpParts.SOCKET), "socket");
    }

    static boolean overlaps(float[] a, float[] b) {
        return a[0] < b[3] && b[0] < a[3] && a[1] < b[4] && b[1] < a[4] && a[2] < b[5] && b[2] < a[5];
    }
}
