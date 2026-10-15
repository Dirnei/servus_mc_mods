package io.github.dirnei.mc.servus.alchemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class HexColorTest {

    @Test
    void readsHashedColour() {
        assertEquals(OptionalInt.of(0xd88c62), HexColor.parse("#d88c62"));
    }

    @Test
    void readsUpperCaseWithoutHash() {
        assertEquals(OptionalInt.of(0xFAD63E), HexColor.parse("FAD63E"));
    }

    @Test
    void refusesWrongLength() {
        assertTrue(HexColor.parse("#fff").isEmpty());
        assertTrue(HexColor.parse("#ff00ff00").isEmpty());
        assertTrue(HexColor.parse("").isEmpty());
    }

    @Test
    void refusesNonHexDigits() {
        assertTrue(HexColor.parse("#gg0000").isEmpty());
        assertTrue(HexColor.parse("#-12345").isEmpty());
    }

    @Test
    void formatsBackToText() {
        assertEquals("#50e8de", HexColor.format(0x50E8DE));
        assertEquals("#000a0b", HexColor.format(0xFF000A0B));
    }
}
