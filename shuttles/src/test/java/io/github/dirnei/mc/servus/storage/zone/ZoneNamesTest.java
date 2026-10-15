package io.github.dirnei.mc.servus.storage.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ZoneNamesTest {

    @Test
    void trimsSpaces() {
        assertEquals("aisle 3", ZoneNames.key("  Aisle 3 "));
        assertEquals("Aisle 3", ZoneNames.display("  Aisle 3 "));
    }

    @Test
    void foldsCase() {
        assertEquals(ZoneNames.key("AISLE 3"), ZoneNames.key("aisle 3"));
        assertEquals("AISLE 3", ZoneNames.display("AISLE 3"));
    }

    @Test
    void blankGivesEmpty() {
        assertEquals("", ZoneNames.key("   "));
        assertEquals("", ZoneNames.key(null));
        assertEquals("", ZoneNames.display(null));
    }

    @Test
    void cutsLongNames() {
        String longName = "x".repeat(ZoneNames.MAX_LENGTH + 10);
        assertEquals(ZoneNames.MAX_LENGTH, ZoneNames.key(longName).length());
        assertEquals(ZoneNames.MAX_LENGTH, ZoneNames.display(longName).length());
    }
}
