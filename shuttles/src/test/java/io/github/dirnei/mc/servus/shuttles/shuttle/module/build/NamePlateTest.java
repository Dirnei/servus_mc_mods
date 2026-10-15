package io.github.dirnei.mc.servus.shuttles.shuttle.module.build;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NamePlateTest {

    @Test
    void plainNamesStay() {
        assertEquals("Berta", NamePlate.clean("Berta"));
    }

    @Test
    void trimmed() {
        assertEquals("Kurt", NamePlate.clean("  Kurt \t"));
    }

    @Test
    void formattingCodesGo() {
        assertEquals("Red Berta", NamePlate.clean("§cRed §lBerta"));
    }

    @Test
    void controlCharactersGo() {
        assertEquals("AB", NamePlate.clean("A\nB"));
    }

    @Test
    void cutToTwenty() {
        assertEquals(20, NamePlate.clean("abcdefghijklmnopqrstuvwxyz").length());
    }

    @Test
    void emptyStaysEmpty() {
        assertEquals("", NamePlate.clean("   "));
        assertEquals("", NamePlate.clean(null));
    }
}
