package io.github.dirnei.mc.servus.storage.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                || area.name().equals("grid") || area.name().equals("craft grid");
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

    /** GUI heights of a 1080p screen at GUI scales 2, 3 and 4. */
    private static final int[] SCREEN_HEIGHTS = {540, 360, 270};

    @Test
    void counterAreasDoNotOverlap() {
        for (TerminalSettings.Height height : TerminalSettings.Height.values()) {
            for (int screen : SCREEN_HEIGHTS) {
                assertDisjoint(Counter.layout(Counter.rowsFor(height, screen)).areas());
            }
        }
    }

    @Test
    void counterAreasInsidePanel() {
        for (TerminalSettings.Height height : TerminalSettings.Height.values()) {
            for (int screen : SCREEN_HEIGHTS) {
                Counter.Layout layout = Counter.layout(Counter.rowsFor(height, screen));
                String at = height + " at " + screen + ": ";
                for (Area area : layout.areas()) {
                    Rect rect = framed(area);
                    if (area.name().startsWith("side")) {
                        assertTrue(rect.right() <= 0 && rect.x() >= Counter.SIDE_X, at + area.name() + " is not left of the panel");
                        assertTrue(rect.y() >= layout.top() && rect.bottom() <= Counter.BASE_HEIGHT, at + area.name() + " leaves the panel height");
                    } else {
                        assertTrue(rect.x() >= 0 && rect.right() <= Counter.WIDTH && rect.y() >= layout.top()
                                && rect.bottom() <= Counter.BASE_HEIGHT, at + area.name() + " leaves the panel");
                    }
                }
                Rect dialog = layout.dialog();
                assertTrue(dialog.y() >= layout.top() && dialog.bottom() <= Counter.CRAFT_GRID.bottom(), at + "the dialog leaves its area");
                assertFalse(dialog.overlaps(Counter.INVENTORY_LABEL), at + "the dialog covers the inventory label");
                assertTrue(layout.height() <= screen - 4, at + "the panel does not fit the screen");
            }
        }
        assertTrue(Counter.outputY(8) + TerminalLayout.CELL <= Counter.OUTPUT.bottom(), "nine output slots fit");
        assertTrue(Counter.craftX(8) + TerminalLayout.CELL <= Counter.CRAFT_GRID.right()
                && Counter.craftY(8) + TerminalLayout.CELL <= Counter.CRAFT_GRID.bottom(), "nine grid slots fit");
        assertTrue(Counter.CRAFT_GRID.right() < Counter.OUTPUT.x(), "the crafting grid sits left of the output");
    }

    @Test
    void counterRowsFollowTheHeight() {
        assertEquals(4, Counter.rowsFor(TerminalSettings.Height.TALL, 270), "scale 4 fits only the base rows");
        assertEquals(9, Counter.rowsFor(TerminalSettings.Height.TALL, 540));
        assertEquals(6, Counter.rowsFor(TerminalSettings.Height.MEDIUM, 540));
        assertEquals(4, Counter.rowsFor(TerminalSettings.Height.SMALL, 540));
        assertEquals(9, Counter.rowsFor(TerminalSettings.Height.AUTO, 360));
        assertEquals(19, Counter.rowsFor(TerminalSettings.Height.AUTO, 540));
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
        assertTrue(Counter.BASE_HEIGHT <= TerminalLayout.MAX_HEIGHT, "counter panel too tall");
        assertTrue(Production.HEIGHT <= TerminalLayout.MAX_HEIGHT, "production panel too tall");
        assertTrue(Counter.WIDTH <= 480 && Production.WIDTH <= 480, "panels too wide for 1080p at scale 4");
    }
}
