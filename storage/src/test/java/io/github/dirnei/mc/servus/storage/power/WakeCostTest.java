package io.github.dirnei.mc.servus.storage.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WakeCostTest {

    @Test
    void nineChunksAtTwo() {
        assertEquals(18, WakeCost.perTick(9, 2));
        assertEquals(360, WakeCost.toStart(9, 2));
    }

    @Test
    void zeroChunksCostNothing() {
        assertEquals(0, WakeCost.perTick(0, 2));
        assertEquals(0, WakeCost.toStart(0, 2));
        assertEquals(0, WakeCost.perTick(9, 0));
    }

    @Test
    void fractionsRoundUp() {
        assertEquals(5, WakeCost.perTick(9, 0.5));
    }
}
