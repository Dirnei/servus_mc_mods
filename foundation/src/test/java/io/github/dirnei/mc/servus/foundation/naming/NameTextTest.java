package io.github.dirnei.mc.servus.foundation.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NameTextTest {

    @Test
    void nullAndBlankAreEmpty() {
        assertEquals("", NameText.clean(null));
        assertEquals("", NameText.clean(" \n\t "));
    }

    @Test
    void nameIsTrimmed() {
        assertEquals("North Mine", NameText.clean("  North Mine "));
    }

    @Test
    void longNamesAreCut() {
        String typed = "Averylongwarehousenamethatgoesonandonandon";
        String cleaned = NameText.clean(typed);
        assertEquals(NameText.MAX_LENGTH, cleaned.length());
        assertEquals(typed.substring(0, NameText.MAX_LENGTH), cleaned);
    }

    @Test
    void cutNameLosesTrailingSpace() {
        String typed = "a".repeat(NameText.MAX_LENGTH - 1) + " b";
        assertEquals("a".repeat(NameText.MAX_LENGTH - 1), NameText.clean(typed));
    }

    @Test
    void controlAndFormatCharactersAreDropped() {
        assertEquals("North Mine", NameText.clean("No\nrth Mine"));
        assertEquals("Northc Mine", NameText.clean("North§c Mine"));
        assertEquals("WheatNorth", NameText.clean("  Wheat\nNorth  "));
    }
}
