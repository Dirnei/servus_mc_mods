package io.github.dirnei.mc.servus.foundation.machine.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.machine.sink.ChimneyRules.Mode;
import io.github.dirnei.mc.servus.foundation.machine.sink.ChimneyRules.Smoke;
import org.junit.jupiter.api.Test;

class ChimneyRulesTest {

    @Test
    void smokeStepsAndWraps() {
        assertEquals(Smoke.THIN, ChimneyRules.nextSmoke(Smoke.OFF));
        assertEquals(Smoke.COSY, ChimneyRules.nextSmoke(Smoke.THIN));
        assertEquals(Smoke.SIGNAL, ChimneyRules.nextSmoke(Smoke.COSY));
        assertEquals(Smoke.OFF, ChimneyRules.nextSmoke(Smoke.SIGNAL));
    }

    @Test
    void modeStepsAndWraps() {
        assertEquals(Mode.POWERED, ChimneyRules.nextMode(Mode.ALWAYS));
        assertEquals(Mode.UNPOWERED, ChimneyRules.nextMode(Mode.POWERED));
        assertEquals(Mode.ALWAYS, ChimneyRules.nextMode(Mode.UNPOWERED));
    }

    @Test
    void offNeverSmokes() {
        for (Mode mode : Mode.values()) {
            assertFalse(ChimneyRules.smokes(Smoke.OFF, mode, true, false));
            assertFalse(ChimneyRules.smokes(Smoke.OFF, mode, false, false));
        }
    }

    @Test
    void alwaysSmokesPoweredAndUnpowered() {
        assertTrue(ChimneyRules.smokes(Smoke.COSY, Mode.ALWAYS, true, false));
        assertTrue(ChimneyRules.smokes(Smoke.COSY, Mode.ALWAYS, false, false));
    }

    @Test
    void poweredModesFollowTheSignal() {
        assertTrue(ChimneyRules.smokes(Smoke.THIN, Mode.POWERED, true, false));
        assertFalse(ChimneyRules.smokes(Smoke.THIN, Mode.POWERED, false, false));
        assertFalse(ChimneyRules.smokes(Smoke.THIN, Mode.UNPOWERED, true, false));
        assertTrue(ChimneyRules.smokes(Smoke.THIN, Mode.UNPOWERED, false, false));
    }

    @Test
    void onASinkNeverSmokes() {
        for (Smoke smoke : Smoke.values()) {
            for (Mode mode : Mode.values()) {
                assertFalse(ChimneyRules.smokes(smoke, mode, true, true));
                assertFalse(ChimneyRules.smokes(smoke, mode, false, true));
            }
        }
    }
}
