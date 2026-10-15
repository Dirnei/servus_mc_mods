package io.github.dirnei.mc.servus.foundation.logistics.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChannelColoursTest {

    @Test
    void sixteenColours() {
        assertEquals(16, ChannelColours.COUNT);
        Set<Integer> colours = new HashSet<>();
        Set<String> dyes = new HashSet<>();
        for (int i = 0; i < ChannelColours.COUNT; i++) {
            int argb = ChannelColours.argb(i);
            assertEquals(0xFF, argb >>> 24, "colour " + i + " is opaque");
            colours.add(argb);
            dyes.add(ChannelColours.dye(i));
        }
        assertEquals(16, colours.size());
        assertEquals(16, dyes.size());
        assertEquals("white", ChannelColours.dye(0));
        assertEquals("red", ChannelColours.dye(14));
        assertEquals(ChannelColours.argb(0), ChannelColours.argb(99));
    }

    @Test
    void blackIsLifted() {
        assertEquals("black", ChannelColours.dye(15));
        int black = ChannelColours.argb(15);
        assertTrue(((black >> 16) & 0xFF) >= ChannelColours.BLACK_FLOOR);
        assertTrue(((black >> 8) & 0xFF) >= ChannelColours.BLACK_FLOOR);
        assertTrue((black & 0xFF) >= ChannelColours.BLACK_FLOOR);
    }
}
