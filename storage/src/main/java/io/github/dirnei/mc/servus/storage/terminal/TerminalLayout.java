package io.github.dirnei.mc.servus.storage.terminal;

import java.util.List;

/**
 * Where everything sits on the Counter Terminal and Production Terminal
 * screens, relative to the panel's top left corner. Menus place their slots
 * and screens draw and hit test from these rectangles; a unit test keeps the
 * areas of a screen apart and inside its panel. Pure, no Minecraft imports.
 */
public final class TerminalLayout {

    /** An area on a panel. Slot areas start at the item origin of their first slot. */
    public record Rect(int x, int y, int w, int h) {
        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }

        public boolean contains(int px, int py) {
            return px >= x && py >= y && px < right() && py < bottom();
        }

        public boolean overlaps(Rect other) {
            return x < other.right() && other.x < right() && y < other.bottom() && other.y < bottom();
        }

        public boolean inside(int width, int height) {
            return x >= 0 && y >= 0 && right() <= width && bottom() <= height;
        }
    }

    /** A named area, for test messages. */
    public record Area(String name, Rect rect) {
    }

    public static final int CELL = 18;
    /** 1080p at GUI scale 4 leaves 270 pixels; a little margin stays free. */
    public static final int MAX_HEIGHT = 266;

    private TerminalLayout() {
    }

    /** The Counter Terminal: stock on the left, requests and output on the right, player inventory below the stock. */
    public static final class Counter {
        public static final int WIDTH = 290;
        public static final int HEIGHT = 222;
        public static final int COLUMNS = 9;
        public static final int ROWS = 5;
        public static final int OUTPUT_COLUMNS = 3;
        public static final int REQUEST_ROW = 12;

        public static final Rect TITLE = new Rect(8, 6, 274, 9);
        public static final Rect SEARCH = new Rect(8, 18, 112, 12);
        public static final Rect FILTER = new Rect(124, 18, 46, 12);
        public static final Rect GRID = new Rect(8, 34, COLUMNS * CELL, ROWS * CELL);
        public static final Rect SCROLLBAR = new Rect(174, 34, 8, ROWS * CELL);
        public static final Rect INVENTORY_LABEL = new Rect(8, 127, 162, 9);
        public static final Rect INVENTORY = new Rect(8, 139, 9 * CELL, 3 * CELL);
        public static final Rect HOTBAR = new Rect(8, 197, 9 * CELL, CELL);
        public static final Rect REQUESTS_LABEL = new Rect(190, 18, 92, 9);
        public static final Rect REQUESTS = new Rect(190, 30, 92, 6 * REQUEST_ROW);
        public static final Rect MESSAGE = new Rect(190, 104, 92, 10);
        public static final Rect OUTPUT_LABEL = new Rect(190, 117, 92, 9);
        public static final Rect OUTPUT = new Rect(209, 129, OUTPUT_COLUMNS * CELL, 3 * CELL);
        /** The amount prompt and the craft dialog cover the grid and the requests while open. */
        public static final Rect DIALOG = new Rect(8, 30, 274, 94);

        private Counter() {
        }

        public static List<Area> areas() {
            return List.of(new Area("title", TITLE), new Area("search", SEARCH), new Area("filter", FILTER),
                    new Area("grid", GRID), new Area("scrollbar", SCROLLBAR), new Area("inventory label", INVENTORY_LABEL),
                    new Area("inventory", INVENTORY), new Area("hotbar", HOTBAR), new Area("requests label", REQUESTS_LABEL),
                    new Area("requests", REQUESTS), new Area("message", MESSAGE), new Area("output label", OUTPUT_LABEL),
                    new Area("output", OUTPUT));
        }

        /** Item origin of an output slot. */
        public static int outputX(int slot) {
            return OUTPUT.x() + (slot % OUTPUT_COLUMNS) * CELL;
        }

        public static int outputY(int slot) {
            return OUTPUT.y() + (slot / OUTPUT_COLUMNS) * CELL;
        }
    }

    /** The Production Terminal: title, tab bar, list; the pattern editor with the player inventory over the list. */
    public static final class Production {
        public static final int WIDTH = 240;
        public static final int HEIGHT = 222;
        public static final int TAB_WIDTH = 64;
        public static final int TAB_GAP = 2;

        public static final Rect TITLE = new Rect(8, 6, 224, 9);
        public static final Rect TABS = new Rect(8, 18, 224, 12);
        public static final Rect LIST = new Rect(8, 34, 224, 180);
        public static final Rect POPUP = new Rect(40, 34, 160, 72);
        public static final Rect INVENTORY = new Rect(40, 122, 9 * CELL, 3 * CELL);
        public static final Rect HOTBAR = new Rect(40, 180, 9 * CELL, CELL);

        private Production() {
        }

        /** The areas while no editor is open. */
        public static List<Area> areas() {
            return List.of(new Area("title", TITLE), new Area("tabs", TABS), new Area("list", LIST));
        }

        /** The areas while the pattern editor is open; they replace the list. */
        public static List<Area> editorAreas() {
            return List.of(new Area("title", TITLE), new Area("tabs", TABS), new Area("popup", POPUP),
                    new Area("inventory", INVENTORY), new Area("hotbar", HOTBAR));
        }

        /** Left edge of a tab, tabs left aligned on the tab row. */
        public static int tabX(int index) {
            return TABS.x() + index * (TAB_WIDTH + TAB_GAP);
        }

        // Rows of the list. Their parts are relative to the row's top left corner on the list.

        /** The Crafting tab's one line CPU header. */
        public static final int HEADER_HEIGHT = 12;
        /** One job of the Crafting tab. */
        public static final int JOB_HEIGHT = 22;
        public static final Rect JOB_ICON = new Rect(2, 3, 16, 16);
        public static final Rect JOB_TITLE = new Rect(22, 2, 150, 9);
        public static final Rect JOB_BAR = new Rect(176, 4, 28, 6);
        public static final Rect JOB_STATUS = new Rect(22, 12, 182, 9);
        public static final Rect JOB_CANCEL = new Rect(208, 4, 12, 12);
        /** One member of the Machines tab. */
        public static final int MACHINE_HEIGHT = 20;
        public static final Rect MACHINE_ICON = new Rect(2, 2, 16, 16);
        public static final Rect MACHINE_NAME = new Rect(22, 1, 150, 9);
        public static final Rect MACHINE_STATE = new Rect(22, 11, 150, 9);
        public static final Rect MACHINE_BAR = new Rect(176, 8, 44, 5);

        /** The parts of a job row. */
        public static List<Area> jobAreas() {
            return List.of(new Area("job icon", JOB_ICON), new Area("job title", JOB_TITLE), new Area("job bar", JOB_BAR),
                    new Area("job status", JOB_STATUS), new Area("job cancel", JOB_CANCEL));
        }

        /** The parts of a machine row. */
        public static List<Area> machineAreas() {
            return List.of(new Area("machine icon", MACHINE_ICON), new Area("machine name", MACHINE_NAME),
                    new Area("machine state", MACHINE_STATE), new Area("machine bar", MACHINE_BAR));
        }
    }
}
