package io.github.dirnei.mc.servus.storage.terminal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Area;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Counter;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Production;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Rect;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerminalLayoutTest {

    /** Slot frames reach one pixel past the item origin area on every side. */
    private static Rect framed(Area area) {
        Rect rect = area.rect();
        boolean slots = area.name().equals("inventory") || area.name().equals("hotbar") || area.name().equals("output")
                || area.name().equals("grid");
        return slots ? new Rect(rect.x() - 1, rect.y() - 1, rect.w() + 2, rect.h() + 2) : rect;
    }

    private static void assertDisjoint(List<Area> areas) {
        for (int i = 0; i < areas.size(); i++) {
            for (int j = i + 1; j < areas.size(); j++) {
                Area a = areas.get(i);
                Area b = areas.get(j);
                assertFalse(framed(a).overlaps(framed(b)), a.name() + " overlaps " + b.name());
            }
        }
    }

    private static void assertInside(List<Area> areas, int width, int height) {
        for (Area area : areas) {
            assertTrue(framed(area).inside(width, height), area.name() + " leaves the panel");
        }
    }

    @Test
    void counterAreasDoNotOverlap() {
        assertDisjoint(Counter.areas());
    }

    @Test
    void counterAreasInsidePanel() {
        assertInside(Counter.areas(), Counter.WIDTH, Counter.HEIGHT);
        assertTrue(Counter.DIALOG.inside(Counter.WIDTH, Counter.HEIGHT), "the dialog leaves the panel");
        assertFalse(Counter.DIALOG.overlaps(Counter.INVENTORY_LABEL), "the dialog covers the inventory label");
        assertTrue(Counter.outputY(8) + TerminalLayout.CELL <= Counter.OUTPUT.bottom(), "nine output slots fit");
    }

    @Test
    void productionAreasDoNotOverlap() {
        assertDisjoint(Production.areas());
        assertDisjoint(Production.editorAreas());
        assertTrue(Production.tabX(2) + Production.TAB_WIDTH <= Production.TABS.right(), "three tabs fit the tab row");
    }

    @Test
    void productionAreasInsidePanel() {
        assertInside(Production.areas(), Production.WIDTH, Production.HEIGHT);
        assertInside(Production.editorAreas(), Production.WIDTH, Production.HEIGHT);
    }

    @Test
    void jobRowPartsApartAndInsideRow() {
        assertDisjoint(Production.jobAreas());
        assertInside(Production.jobAreas(), Production.LIST.w(), Production.JOB_HEIGHT);
    }

    @Test
    void machineRowPartsApartAndInsideRow() {
        assertDisjoint(Production.machineAreas());
        assertInside(Production.machineAreas(), Production.LIST.w(), Production.MACHINE_HEIGHT);
    }

    @Test
    void panelsFit1080p() {
        assertTrue(Counter.HEIGHT <= TerminalLayout.MAX_HEIGHT, "counter panel too tall");
        assertTrue(Production.HEIGHT <= TerminalLayout.MAX_HEIGHT, "production panel too tall");
        assertTrue(Counter.WIDTH <= 480 && Production.WIDTH <= 480, "panels too wide for 1080p at scale 4");
    }
}
