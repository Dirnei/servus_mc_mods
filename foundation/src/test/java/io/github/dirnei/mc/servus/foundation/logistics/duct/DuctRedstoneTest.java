package io.github.dirnei.mc.servus.foundation.logistics.duct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DuctRedstoneTest {

    @Test
    void settingMetTable() {
        assertTrue(DuctRedstone.IGNORE.met(true));
        assertTrue(DuctRedstone.IGNORE.met(false));
        assertTrue(DuctRedstone.POWERED.met(true));
        assertFalse(DuctRedstone.POWERED.met(false));
        assertFalse(DuctRedstone.UNPOWERED.met(true));
        assertTrue(DuctRedstone.UNPOWERED.met(false));
    }

    @Test
    void activeNeedsOnAndTheSettingMet() {
        for (DuctRedstone setting : DuctRedstone.values()) {
            for (boolean powered : new boolean[] {true, false}) {
                assertEquals(setting.met(powered), setting.active(true, powered), setting + " on, powered " + powered);
                assertFalse(setting.active(false, powered), setting + " off, powered " + powered);
            }
        }
    }

    @Test
    void cycleWraps() {
        assertEquals(DuctRedstone.POWERED, DuctRedstone.IGNORE.next());
        assertEquals(DuctRedstone.UNPOWERED, DuctRedstone.POWERED.next());
        assertEquals(DuctRedstone.IGNORE, DuctRedstone.UNPOWERED.next());
    }

    @Test
    void idRoundTrip() {
        for (DuctRedstone setting : DuctRedstone.values()) {
            assertEquals(setting, DuctRedstone.byId(setting.id()));
        }
        assertEquals(DuctRedstone.IGNORE, DuctRedstone.byId(-1));
        assertEquals(DuctRedstone.IGNORE, DuctRedstone.byId(99));
    }

    @Test
    void legacyControlIds() {
        assertEquals(DuctRedstone.IGNORE, DuctRedstone.byId(0));
        assertEquals(DuctRedstone.POWERED, DuctRedstone.byId(1));
        assertEquals(DuctRedstone.UNPOWERED, DuctRedstone.byId(2));
        assertEquals(DuctRedstone.IGNORE, DuctRedstone.byId(3));
        assertTrue(DuctRedstone.legacyOff(3));
        assertFalse(DuctRedstone.legacyOff(0));
        assertFalse(DuctRedstone.legacyOff(2));
    }
}
