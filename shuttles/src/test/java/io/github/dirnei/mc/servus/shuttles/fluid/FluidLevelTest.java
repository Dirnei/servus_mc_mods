package io.github.dirnei.mc.servus.shuttles.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FluidLevelTest {

    @Test
    void emptyIsZero() {
        assertEquals(0, FluidLevel.step(0, 16000, 32));
        assertEquals(0, FluidLevel.step(500, 0, 32));
    }

    @Test
    void fullIsTopStep() {
        assertEquals(32, FluidLevel.step(16000, 16000, 32));
        assertEquals(31, FluidLevel.step(15999, 16000, 32));
        assertEquals(16, FluidLevel.step(8000, 16000, 32));
    }

    @Test
    void anyFluidShowsAtLeastOneStep() {
        assertEquals(1, FluidLevel.step(1, 16000, 32));
        assertEquals(1, FluidLevel.step(499, 16000, 32));
    }
}
