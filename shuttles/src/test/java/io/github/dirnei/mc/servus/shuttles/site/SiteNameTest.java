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
    void nameIsCleaned() {
        assertEquals("North Mine", SiteName.display("  North\n Mine ", 1, 2));
    }
}
