package io.github.dirnei.mc.servus.foundation.logistics.duct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DuctControlTest {

    @Test
    void activityTable() {
        assertTrue(DuctControl.ALWAYS.active(true));
        assertTrue(DuctControl.ALWAYS.active(false));
        assertTrue(DuctControl.POWERED.active(true));
        assertFalse(DuctControl.POWERED.active(false));
        assertFalse(DuctControl.UNPOWERED.active(true));
        assertTrue(DuctControl.UNPOWERED.active(false));
        assertFalse(DuctControl.OFF.active(true));
        assertFalse(DuctControl.OFF.active(false));
    }

    @Test
    void cycleWraps() {
        assertEquals(DuctControl.POWERED, DuctControl.ALWAYS.next());
        assertEquals(DuctControl.UNPOWERED, DuctControl.POWERED.next());
        assertEquals(DuctControl.OFF, DuctControl.UNPOWERED.next());
        assertEquals(DuctControl.ALWAYS, DuctControl.OFF.next());
    }

    @Test
    void idRoundTrip() {
        for (DuctControl control : DuctControl.values()) {
            assertEquals(control, DuctControl.byId(control.id()));
        }
    }

    @Test
    void unknownIdIsAlways() {
        assertEquals(DuctControl.ALWAYS, DuctControl.byId(-1));
        assertEquals(DuctControl.ALWAYS, DuctControl.byId(99));
    }
}
