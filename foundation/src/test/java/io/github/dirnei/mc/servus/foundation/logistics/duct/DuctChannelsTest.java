package io.github.dirnei.mc.servus.foundation.logistics.duct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DuctChannelsTest {

    @Test
    void eightDistinctOpaqueColors() {
        assertEquals(8, DuctChannels.COUNT);
        Set<Integer> seen = new HashSet<>();
        for (int channel = 0; channel < DuctChannels.COUNT; channel++) {
            int color = DuctChannels.color(channel);
            assertEquals(0xFF, color >>> 24, "channel " + channel + " must be opaque");
            assertTrue(seen.add(color), "channel " + channel + " repeats a color");
        }
    }

    @Test
    void clampKeepsChannelsInRange() {
        assertEquals(0, DuctChannels.clamp(-1));
        assertEquals(7, DuctChannels.clamp(8));
        assertEquals(3, DuctChannels.clamp(3));
        assertEquals(DuctChannels.color(0), DuctChannels.color(-1));
        assertEquals(DuctChannels.color(7), DuctChannels.color(99));
    }

    @Test
    void displayNumbersAndKeysAreOneBased() {
        assertEquals(1, DuctChannels.display(0));
        assertEquals(8, DuctChannels.display(7));
        assertEquals("gui.servus_logistics.channel.1", DuctChannels.nameKey(0));
        assertEquals("gui.servus_logistics.channel.8", DuctChannels.nameKey(7));
        assertNotEquals(DuctChannels.nameKey(0), DuctChannels.nameKey(1));
    }
}
