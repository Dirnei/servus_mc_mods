package io.github.dirnei.mc.servus.foundation.data.packet;

import static io.github.dirnei.mc.servus.foundation.data.packet.TransferAmount.NO_GATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TransferAmountTest {

    @Test
    void withoutGatesEverythingMovesAtOnce() {
        assertEquals(64, TransferAmount.of(64, 64, 64, NO_GATE, NO_GATE));
        assertEquals(20, TransferAmount.gates(20, NO_GATE, NO_GATE));
    }

    @Test
    void sourceAndTargetBound() {
        assertEquals(10, TransferAmount.of(64, 10, 64, NO_GATE, NO_GATE));
        assertEquals(3, TransferAmount.of(8, 8, 3, NO_GATE, NO_GATE));
    }

    @Test
    void smallestGateWins() {
        assertEquals(1, TransferAmount.of(64, 64, 64, 1, NO_GATE));
        assertEquals(2, TransferAmount.of(64, 64, 64, 5, 2));
        assertEquals(0, TransferAmount.gates(64, 0, NO_GATE));
    }

    @Test
    void neverNegative() {
        assertEquals(0, TransferAmount.of(0, 64, 64, NO_GATE, NO_GATE));
        assertEquals(0, TransferAmount.of(5, -1, 64, NO_GATE, NO_GATE));
    }
}
