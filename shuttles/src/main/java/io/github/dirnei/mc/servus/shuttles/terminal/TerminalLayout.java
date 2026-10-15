package io.github.dirnei.mc.servus.shuttles.terminal;

import java.util.ArrayList;
import java.util.List;

/**
 * Where everything sits on the Storage Terminal and Production Terminal
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

        /** Whether a point is on this slot row with its one pixel frame, or on its label from {@code labelY} down. */
        public boolean coversRow(int px, int py, int labelY) {
            return px >= x - 1 && px < right() + 1 && py >= labelY && py < bottom();
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

    /**
     * The Storage Terminal, Refined Storage style: view buttons in a column
     * left of the panel, title, Craft button and search, the item grid with
     * its scrollbar, a message line, the Input row, the Output row, then the
     * player inventory. Everything from the message line down sits at fixed
     * offsets from the panel's base top, so slots never move; extra grid rows
     * grow the panel upwards, to negative y. The crafting popup lies over the
     * grid's base rows, which every height shows.
     */
    public static final class Storage {
        public static final int WIDTH = 190;
        /** Panel height with {@link #BASE_ROWS} grid rows. */
        public static final int BASE_HEIGHT = 242;
        public static final int COLUMNS = 9;
        public static final int BASE_ROWS = 4;
        /** Slots in the Input and in the Output row. */
        public static final int ROW_SLOTS = 9;
        public static final int SIDE_BUTTONS = 5;
        public static final int SIDE_X = -22;
        public static final int SIDE_SIZE = 18;
        public static final int SIDE_STEP = 20;

        public static final Rect MESSAGE = new Rect(8, 92, 174, 9);
        public static final Rect INPUT = new Rect(8, 104, ROW_SLOTS * CELL, CELL);
        public static final Rect INPUT_TAG = new Rect(174, 104, 8, CELL);
        public static final Rect OUTPUT = new Rect(8, 126, ROW_SLOTS * CELL, CELL);
        public static final Rect OUTPUT_TAG = new Rect(174, 126, 8, CELL);
        public static final Rect INVENTORY_LABEL = new Rect(8, 148, 162, 9);
        public static final Rect INVENTORY = new Rect(8, 159, 9 * CELL, 3 * CELL);
        public static final Rect HOTBAR = new Rect(8, 217, 9 * CELL, CELL);

        /** The crafting popup over the grid's base rows, and its parts. */
        public static final Rect POPUP = new Rect(8, 18, 174, BASE_ROWS * CELL);
        public static final Rect CRAFT_GRID = new Rect(18, 27, 3 * CELL, 3 * CELL);
        public static final Rect CRAFT_ARROW = new Rect(80, 49, 16, 10);
        public static final Rect CRAFT_RESULT = new Rect(102, 45, CELL, CELL);
        public static final Rect CLEAR = new Rect(128, 48, 12, 12);

        private Storage() {
        }

        /** The parts that move with the number of grid rows. */
        public record Layout(int rows, int top, Rect title, Rect craft, Rect search, Rect grid, Rect scrollbar, Rect dialog,
                             List<Rect> side) {

            /** Panel height including the rows above the base top. */
            public int height() {
                return BASE_HEIGHT - top;
            }

            /** Everything but the crafting popup, which lies over the grid on purpose. */
            public List<Area> areas() {
                List<Area> areas = new ArrayList<>(List.of(new Area("title", title), new Area("craft button", craft),
                        new Area("search", search), new Area("grid", grid), new Area("scrollbar", scrollbar),
                        new Area("message", MESSAGE), new Area("input", INPUT), new Area("input tag", INPUT_TAG),
                        new Area("output", OUTPUT), new Area("output tag", OUTPUT_TAG),
                        new Area("inventory label", INVENTORY_LABEL), new Area("inventory", INVENTORY), new Area("hotbar", HOTBAR)));
                for (int i = 0; i < side.size(); i++) {
                    areas.add(new Area("side " + i, side.get(i)));
                }
                return areas;
            }
        }

        /** The parts of the crafting popup. */
        public static List<Area> popupAreas() {
            return List.of(new Area("craft grid", CRAFT_GRID), new Area("craft arrow", CRAFT_ARROW),
                    new Area("craft result", CRAFT_RESULT), new Area("clear", CLEAR));
        }

        /** The layout for a number of grid rows, at least {@link #BASE_ROWS}. */
        public static Layout layout(int rows) {
            int shown = Math.max(BASE_ROWS, rows);
            int top = -(shown - BASE_ROWS) * CELL;
            List<Rect> side = new ArrayList<>();
            for (int i = 0; i < SIDE_BUTTONS; i++) {
                side.add(new Rect(SIDE_X, top + 4 + i * SIDE_STEP, SIDE_SIZE, SIDE_SIZE));
            }
            return new Layout(shown, top, new Rect(8, top + 6, 76, 9), new Rect(86, top + 4, 32, 12),
                    new Rect(122, top + 4, 60, 12), new Rect(8, top + 18, COLUMNS * CELL, shown * CELL),
                    new Rect(174, top + 18, 8, shown * CELL),
                    new Rect(8, top + 18, 174, OUTPUT.bottom() - (top + 18)), List.copyOf(side));
        }

        /**
         * Grid rows for a height setting on a screen this many GUI pixels
         * high: what the setting asks for, but no more than fit and never
         * fewer than {@link #BASE_ROWS}.
         */
        public static int rowsFor(TerminalSettings.Height height, int screenHeight) {
            int fit = BASE_ROWS + Math.max(0, (screenHeight - 4 - BASE_HEIGHT) / CELL);
            return Math.max(BASE_ROWS, Math.min(height.rows(), fit));
        }

        /** Item origin of a crafting grid slot. */
        public static int craftX(int slot) {
            return CRAFT_GRID.x() + (slot % 3) * CELL;
        }

        public static int craftY(int slot) {
            return CRAFT_GRID.y() + (slot / 3) * CELL;
        }

        /** Item origin of an Input row slot. */
        public static int inputX(int slot) {
            return INPUT.x() + slot * CELL;
        }

        public static int inputY(int slot) {
            return INPUT.y();
        }

        /** Item origin of an Output row slot. */
        public static int outputX(int slot) {
            return OUTPUT.x() + slot * CELL;
        }

        public static int outputY(int slot) {
            return OUTPUT.y();
        }
    }

    /** The Production Terminal: title, tab bar, list; the pattern editor with the player inventory over the list. */
    public static final class Production {
        public static final int WIDTH = 240;
        public static final int HEIGHT = 222;
        /** Four tabs fit the 224 wide tab row: 4 x 54 + 3 x 2 = 222. */
        public static final int TAB_WIDTH = 54;
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

        /** The Crafting tab's one line CPU header. Row parts below are relative to the row's top left corner on the list. */
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

        /** A line under a job: what waits in the CPU buffer for one step, or what overflowed to storage. */
        public static final int BUFFER_LINE_HEIGHT = 10;
        public static final Rect BUFFER_LINE = new Rect(22, 0, 198, 9);
        /** One CPU of the CPUs tab. */
        public static final int CPU_HEIGHT = 30;
        public static final Rect CPU_ICON = new Rect(2, 3, 16, 16);
        public static final Rect CPU_TITLE = new Rect(22, 2, 120, 9);
        public static final Rect CPU_ITEMS = new Rect(22, 12, 120, 16);
        public static final Rect CPU_SLOT_BAR = new Rect(146, 2, 74, 5);
        public static final Rect CPU_BUFFER_BAR = new Rect(146, 10, 74, 5);
        public static final Rect CPU_STATE = new Rect(146, 18, 74, 9);
        /** One buffered item in the CPU row's item strip. */
        public static final int CPU_ITEM_STEP = 18;

        /** The parts of a job row. */
        public static List<Area> jobAreas() {
            return List.of(new Area("job icon", JOB_ICON), new Area("job title", JOB_TITLE), new Area("job bar", JOB_BAR),
                    new Area("job status", JOB_STATUS), new Area("job cancel", JOB_CANCEL));
        }

        /** The parts of a CPU row. */
        public static List<Area> cpuAreas() {
            return List.of(new Area("cpu icon", CPU_ICON), new Area("cpu title", CPU_TITLE), new Area("cpu items", CPU_ITEMS),
                    new Area("cpu slot bar", CPU_SLOT_BAR), new Area("cpu buffer bar", CPU_BUFFER_BAR), new Area("cpu state", CPU_STATE));
        }

        /** How many buffered items fit the CPU row's strip, one slot kept for the rest count when they do not all fit. */
        public static int cpuItemsShown(int items) {
            int fit = CPU_ITEMS.w() / CPU_ITEM_STEP;
            return items <= fit ? items : Math.max(0, fit - 1);
        }

        /** The parts of a machine row. */
        public static List<Area> machineAreas() {
            return List.of(new Area("machine icon", MACHINE_ICON), new Area("machine name", MACHINE_NAME),
                    new Area("machine state", MACHINE_STATE), new Area("machine bar", MACHINE_BAR));
        }
    }
}
