package io.github.dirnei.mc.servus.shuttles.gantry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.shuttle.module.TankerLook;

import org.junit.jupiter.api.Test;

class GantryLookTest {

    @Test
    void emptyIsZero() {
        assertEquals(0, GantryLook.level(0, 16000, 32));
        assertEquals(0, GantryLook.level(500, 0, 32));
    }

    @Test
    void fullIsTopStep() {
        assertEquals(32, GantryLook.level(16000, 16000, 32));
        assertEquals(31, GantryLook.level(15999, 16000, 32));
        assertEquals(16, GantryLook.level(8000, 16000, 32));
    }

    @Test
    void anyFluidShowsAtLeastOneStep() {
        assertEquals(1, GantryLook.level(1, 16000, 32));
        assertEquals(1, GantryLook.level(499, 16000, 32));
    }

    @Test
    void loweredNozzleStaysInTheBeamBlockAboveTheCap() {
        float lowered = GantryLook.NOZZLE_BOTTOM - GantryLook.spoutDrop();
        assertTrue(GantryLook.spoutDrop() > 0, "the spout lowers");
        assertTrue(lowered >= 0, "the nozzle stays in the beam block");
        assertEquals(TankerLook.capTopInBlock() - 16 + GantryLook.STREAM_GAP, lowered, 1e-4);
        assertTrue(TankerLook.capTopInBlock() < 16, "the tanker stays below the top of its block");
    }
}
