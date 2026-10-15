package io.github.dirnei.mc.servus.shuttles.site;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SiteNameTest {

    @Test
    void blankNameShowsThePosition() {
        assertEquals("Warehouse at 120, -40", SiteName.display("   ", 120, -40));
        assertEquals("Warehouse at 0, 0", SiteName.display(null, 0, 0));
    }

    @Test
    void nameIsTrimmed() {
        assertEquals("North Mine", SiteName.display("  North Mine ", 1, 2));
    }

    @Test
    void longNamesAreCut() {
        String typed = "Averylongwarehousenamethatgoesonandonandon";
        String cleaned = SiteName.clean(typed);
        assertEquals(SiteName.MAX_LENGTH, cleaned.length());
        assertEquals(typed.substring(0, SiteName.MAX_LENGTH), cleaned);
    }

    @Test
    void controlAndFormatCharactersAreDropped() {
        assertEquals("North Mine", SiteName.clean("No\nrth §cMine".replace("§c", "")));
        assertEquals("Northc Mine", SiteName.clean("North§c Mine"));
    }
}
