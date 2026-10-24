package io.github.dirnei.mc.servus.shuttles.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Area;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Storage;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Production;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Rect;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerminalLayoutTest {

    /** Slot frames reach one pixel past the item origin area on every side. */
    private static Rect framed(Area area) {
        Rect rect = area.rect();
        boolean slots = area.name().equals("inventory") || area.name().equals("hotbar") || area.name().equals("output")
                || area.name().equals("input") || area.name().equals("grid") || area.name().equals("order grid");
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
    void storageAreasDoNotOverlap() {
        for (TerminalSettings.Height height : TerminalSettings.Height.values()) {
            for (int screen : SCREEN_HEIGHTS) {
                assertDisjoint(Storage.layout(Storage.rowsFor(height, screen)).areas());
            }
        }
    }

    @Test
    void storageAreasInsidePanel() {
        for (TerminalSettings.Height height : TerminalSettings.Height.values()) {
            for (int screen : SCREEN_HEIGHTS) {
                Storage.Layout layout = Storage.layout(Storage.rowsFor(height, screen));
                String at = height + " at " + screen + ": ";
                for (Area area : layout.areas()) {
                    Rect rect = framed(area);
                    if (area.name().startsWith("side")) {
                        boolean left = rect.right() <= 0 && rect.x() >= Storage.SIDE_X;
                        boolean right = rect.x() >= Storage.WIDTH && rect.right() <= Storage.RIGHT_SIDE_X + Storage.SIDE_SIZE + 1;
                        assertTrue(left || right, at + area.name() + " is not in a side column");
                        assertTrue(rect.y() >= layout.top() && rect.bottom() <= Storage.BASE_HEIGHT, at + area.name() + " leaves the panel height");
                    } else {
                        assertTrue(rect.x() >= 0 && rect.right() <= Storage.WIDTH && rect.y() >= layout.top()
                                && rect.bottom() <= Storage.BASE_HEIGHT, at + area.name() + " leaves the panel");
                    }
                }
                Rect dialog = layout.dialog();
                assertTrue(dialog.y() >= layout.top() && dialog.bottom() <= Storage.OUTPUT.bottom(), at + "the dialog leaves its area");
                assertFalse(dialog.overlaps(Storage.INVENTORY_LABEL), at + "the dialog covers the inventory label");
                assertTrue(layout.height() <= screen - 4, at + "the panel does not fit the screen");
            }
        }
        assertTrue(Storage.outputX(Storage.ROW_SLOTS - 1) + TerminalLayout.CELL <= Storage.OUTPUT.right(), "nine output slots fit");
        assertTrue(Storage.inputX(Storage.ROW_SLOTS - 1) + TerminalLayout.CELL <= Storage.INPUT.right(), "nine input slots fit");
        assertTrue(Storage.INPUT.bottom() < Storage.OUTPUT.y(), "the input row sits above the output row");
    }

    private static void assertInsidePopup(List<Area> areas, Rect popup) {
        for (Area area : areas) {
            Rect rect = framed(area);
            assertTrue(rect.x() >= popup.x() && rect.right() <= popup.right() && rect.y() >= popup.y() && rect.bottom() <= popup.bottom(),
                    area.name() + " leaves its popup");
        }
        assertEquals(Storage.WIDTH, 2 * popup.x() + popup.w(), "the popup is centred on the panel");
        assertTrue(popup.bottom() < Storage.INVENTORY_LABEL.y(), "the popup leaves the inventory free");
    }

    @Test
    void craftPartsApartAndInsideTheOrderArea() {
        assertDisjoint(Storage.craftAreas());
        assertInsidePopup(Storage.craftAreas(), Storage.CRAFT_POPUP);
        Rect stepper = Storage.CRAFT_STEPPER;
        Rect item = Storage.CRAFT_ITEM;
        assertTrue(item.x() >= stepper.x() + 50 && item.right() <= stepper.right() && item.y() >= stepper.y() + 14
                && item.bottom() <= stepper.y() + 32, "the crafted item sits beside the field, between the button rows");
        assertEquals(Storage.CRAFT_LISTS.right(), Storage.craftListX(2) + Storage.CRAFT_LIST_WIDTH, "three lists fill the row");
    }

    @Test
    void orderPartsApartAndInsideTheOrderArea() {
        assertDisjoint(Storage.orderAreas());
        assertInsidePopup(Storage.orderAreas(), Storage.ORDER_POPUP);
        assertTrue(Storage.orderX(8) + TerminalLayout.CELL <= Storage.ORDER_GRID.right()
                && Storage.orderY(8) + TerminalLayout.CELL <= Storage.ORDER_GRID.bottom(), "nine order cells fit");
    }

    @Test
    void storageRowsFollowTheHeight() {
        assertEquals(5, Storage.rowsFor(TerminalSettings.Height.TALL, 270), "scale 4 fits one row more than the base");
        assertEquals(9, Storage.rowsFor(TerminalSettings.Height.TALL, 540));
        assertEquals(6, Storage.rowsFor(TerminalSettings.Height.MEDIUM, 540));
        assertEquals(4, Storage.rowsFor(TerminalSettings.Height.SMALL, 540));
        assertEquals(10, Storage.rowsFor(TerminalSettings.Height.AUTO, 360));
        assertEquals(20, Storage.rowsFor(TerminalSettings.Height.AUTO, 540));
    }

    @Test
    void productionAreasDoNotOverlap() {
        assertDisjoint(Production.areas());
        assertDisjoint(Production.editorAreas());
        assertTabsFit(Production.tabs(new int[] {40, 41, 43, 23, 37}), 4);
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
        assertTrue(Storage.BASE_HEIGHT <= TerminalLayout.MAX_HEIGHT, "storage panel too tall");
        assertTrue(Production.HEIGHT <= TerminalLayout.MAX_HEIGHT, "production panel too tall");
        assertTrue(Storage.WIDTH <= 480 && Production.WIDTH <= 480, "panels too wide for 1080p at scale 4");
    }

    @Test
    void cpuRowPartsDisjointAndInsideTheList() {
        assertDisjoint(Production.cpuAreas());
        assertInside(Production.cpuAreas(), Production.LIST.w(), Production.CPU_HEIGHT);
        assertInside(List.of(new Area("buffer line", Production.BUFFER_LINE)),
                Production.LIST.w(), Production.BUFFER_LINE_HEIGHT);
    }

    @Test
    void slotRowCoversItsFrameAndLabel() {
        Rect row = new Rect(8, 30, 36, 18);
        assertTrue(row.coversRow(7, 19, 19), "left frame pixel on the label line");
        assertTrue(row.coversRow(44, 47, 19), "right frame pixel on the last row line");
        assertFalse(row.coversRow(6, 30, 19), "left of the frame");
        assertFalse(row.coversRow(45, 30, 19), "right of the frame");
        assertFalse(row.coversRow(20, 18, 19), "above the label");
        assertFalse(row.coversRow(20, 48, 19), "below the row");
    }

    @Test
    void cpuItemsCutShortWithARestSlot() {
        int fit = Production.CPU_ITEMS.w() / Production.CPU_ITEM_STEP;
        assertTrue(Production.cpuItemsShown(fit) == fit, "all fit");
        assertTrue(Production.cpuItemsShown(fit + 5) == fit - 1, "one slot left for the rest count");
        assertTrue(Production.cpuItemsShown(0) == 0, "none");
    }

    /** Tabs stay on the row, in order, without overlap, each with at least {@code padding} beside its label. */
    private static void assertTabsFit(List<Rect> tabs, int padding) {
        for (int i = 0; i < tabs.size(); i++) {
            Rect tab = tabs.get(i);
            assertTrue(tab.x() >= Production.TABS.x() && tab.right() <= Production.TABS.right(), "tab " + i + " leaves the row");
            assertTrue(tab.y() == Production.TABS.y() && tab.h() == Production.TABS.h(), "tab " + i + " off the row");
            if (i > 0) {
                assertTrue(tabs.get(i - 1).right() <= tab.x(), "tabs " + (i - 1) + " and " + i + " overlap");
            }
        }
        int[] labels = {40, 41, 43, 23, 37};
        for (int i = 0; i < tabs.size() && padding > 0; i++) {
            assertTrue(tabs.get(i).w() - labels[i] >= padding, "tab " + i + " has too little padding");
        }
    }

    @Test
    void fiveTabsFitTheRow() {
        assertTabsFit(Production.tabs(new int[] {40, 41, 43, 23, 37}), 4);
        List<Rect> tabs = Production.tabs(new int[] {40, 41, 43, 23, 37});
        assertEquals(5, tabs.size());
    }

    @Test
    void tooWideLabelsStillStayOnTheRow() {
        assertTabsFit(Production.tabs(new int[] {90, 90, 90, 90, 90}), 0);
    }

    @Test
    void storageRowPartsDoNotOverlap() {
        assertDisjoint(Production.storageAreas());
        assertInside(Production.storageAreas(), Production.LIST.w(), Production.STORAGE_HEADER_HEIGHT);
        assertInside(Production.storageAreas(), Production.LIST.w(), Production.STORAGE_ROW_HEIGHT);
    }
}
