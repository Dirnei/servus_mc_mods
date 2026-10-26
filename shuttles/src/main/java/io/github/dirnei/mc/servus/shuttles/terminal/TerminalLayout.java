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
    /** Pixels between two tabs of a tab row. */
    public static final int TAB_GAP = 2;

    private TerminalLayout() {
    }

    /**
     * The tabs on a tab row, left to right: each as wide as its label plus an
     * even share of the row's spare width, the remainder to the first tabs.
     * Labels wider than the row get no padding and are cut by the caller; the
     * tabs then still end inside the row.
     */
    public static List<Rect> tabs(Rect row, int[] labelWidths) {
        int count = labelWidths.length;
        if (count == 0) {
            return List.of();
        }
        int room = row.w() - (count - 1) * TAB_GAP;
        int labels = 0;
        for (int width : labelWidths) {
            labels += width;
        }
        int spare = room - labels;
        List<Rect> rects = new ArrayList<>(count);
        int x = row.x();
        for (int i = 0; i < count; i++) {
            int width = spare >= 0 ? labelWidths[i] + share(spare, i, count) : share(room, i, count);
            rects.add(new Rect(x, row.y(), width, row.h()));
            x += width + TAB_GAP;
        }
        return rects;
    }

    /**
     * Tabs as narrow as their labels with {@code padding} on each side, from
     * the row's left edge, never wider than the row.
     */
    public static List<Rect> compactTabs(Rect row, int[] labelWidths, int padding) {
        int needed = (labelWidths.length - 1) * TAB_GAP;
        for (int width : labelWidths) {
            needed += width + 2 * padding;
        }
        return tabs(new Rect(row.x(), row.y(), Math.min(row.w(), Math.max(0, needed)), row.h()), labelWidths);
    }

    /** Part {@code index} of {@code total} split evenly into {@code count}, the remainder to the first parts. */
    private static int share(int total, int index, int count) {
        return total / count + (index < total % count ? 1 : 0);
    }

    /**
     * The Storage Terminal, Refined Storage style: the Stock and Rules tabs
     * above the panel, view buttons in a column left of it, title, Craft
     * button and search, the item grid with
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
        /** Left edge of the column right of the panel, which holds the Craft button. */
        public static final int RIGHT_SIDE_X = WIDTH + 4;

        public static final Rect MESSAGE = new Rect(8, 92, 174, 9);
        public static final Rect INPUT = new Rect(8, 104, ROW_SLOTS * CELL, CELL);
        public static final Rect INPUT_TAG = new Rect(174, 104, 8, CELL);
        public static final Rect OUTPUT = new Rect(8, 126, ROW_SLOTS * CELL, CELL);
        public static final Rect OUTPUT_TAG = new Rect(174, 126, 8, CELL);
        public static final Rect INVENTORY_LABEL = new Rect(8, 148, 162, 9);
        public static final Rect INVENTORY = new Rect(8, 159, 9 * CELL, 3 * CELL);
        public static final Rect HOTBAR = new Rect(8, 217, 9 * CELL, CELL);

        /**
         * The order grid popup at fixed offsets of the base rows, since its
         * ghost cells are menu slots that cannot move; centred on the panel,
         * wider than it and above the inventory label.
         */
        public static final Rect ORDER_POPUP = new Rect(-23, 40, 236, 90);
        public static final Rect ORDER_GRID = new Rect(36, 46, 3 * CELL, 3 * CELL);
        public static final Rect ORDER_ARROW = new Rect(95, 68, 16, 10);
        public static final Rect ORDER_RESULT = new Rect(116, 65, 16, 16);
        public static final Rect ORDER_SAVE = new Rect(136, 64, 18, 18);
        public static final Rect ORDER_BUTTONS = new Rect(-17, 110, 224, 14);

        /** The amount popup of the craft dialog and the order grid's second step, centred like the order grid. */
        public static final Rect CRAFT_POPUP = new Rect(-23, 18, 236, 128);
        public static final Rect CRAFT_TITLE = new Rect(-17, 24, 224, 9);
        public static final Rect CRAFT_STEPPER = new Rect(58, 36, 74, 46);
        /** The crafted item beside the stepper's field, inside {@link #CRAFT_STEPPER}'s middle row. */
        public static final Rect CRAFT_ITEM = new Rect(110, 51, 16, 16);
        public static final Rect CRAFT_LISTS = new Rect(-17, 86, 224, 36);
        public static final int CRAFT_LIST_WIDTH = 72;
        public static final Rect CRAFT_BUTTONS = new Rect(-17, 126, 224, 14);
        /** Width of Cancel, Back and Clear; Next and Start are {@link #WIDE_BUTTON} wide. */
        public static final int BUTTON = 50;
        public static final int WIDE_BUTTON = 60;

        /** The Request, Craft and Rule tabs of the item popup, on the popup's top edge over the dimmed title row. */
        public static final Rect POPUP_TABS = new Rect(-17, 4, 224, 14);
        /** Space on each side of a tab label. */
        public static final int TAB_PADDING = 6;
        /** Height of the Stock and Rules tab strip above the panel's top. */
        public static final int TAB_STRIP = 14;

        /** The Rule tab of the item popup: item and state, the Make and Limit groups, the note lines and the buttons. */
        public static final Rect RULE_TITLE = new Rect(-17, 24, 224, 16);
        public static final Rect RULE_MAKE = new Rect(-17, 44, 224, 23);
        public static final Rect RULE_LIMIT = new Rect(-17, 71, 224, 23);
        public static final Rect RULE_NOTE = new Rect(-17, 98, 224, 20);
        public static final Rect RULE_BUTTONS = CRAFT_BUTTONS;
        /** The group label column of the Make and Limit rows. */
        public static final int RULE_GROUP_LABEL = 30;
        /** One field block of a group row: a label over a one line stepper. */
        public static final int RULE_FIELD = 94;
        public static final int RULE_FIELD_LABEL = 9;

        /** The Rules tab lists one rule per row in the grid's area; the first row is the add slot. */
        public static final int RULE_ROW = CELL;
        public static final Rect RULE_ROW_ICON = new Rect(0, 0, 16, 16);
        public static final Rect RULE_ROW_MAKE = new Rect(18, 4, 52, 9);
        public static final Rect RULE_ROW_REFUSE = new Rect(72, 4, 28, 9);
        public static final Rect RULE_ROW_STATE = new Rect(102, 4, 60, 9);

        private Storage() {
        }

        /** The parts that move with the number of grid rows. */
        public record Layout(int rows, int top, Rect title, Rect craft, Rect search, Rect grid, Rect scrollbar, Rect tabs,
                             List<Rect> side) {

            /** Panel height including the rows above the base top. */
            public int height() {
                return BASE_HEIGHT - top;
            }

            /** Panel height with the Stock and Rules tab strip above it. */
            public int heightWithTabs() {
                return height() + TAB_STRIP;
            }

            /** Everything but the popups and the order grid, which lie over the grid on purpose. */
            public List<Area> areas() {
                List<Area> areas = new ArrayList<>(List.of(new Area("tabs", tabs), new Area("title", title), new Area("side craft", craft),
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

        /** The parts of the order grid. */
        public static List<Area> orderAreas() {
            return List.of(new Area("order grid", ORDER_GRID), new Area("order arrow", ORDER_ARROW),
                    new Area("order result", ORDER_RESULT), new Area("order save", ORDER_SAVE),
                    new Area("order buttons", ORDER_BUTTONS));
        }

        /** The parts of the Rule tab. */
        public static List<Area> ruleAreas() {
            return List.of(new Area("rule title", RULE_TITLE), new Area("rule make", RULE_MAKE),
                    new Area("rule limit", RULE_LIMIT), new Area("rule note", RULE_NOTE), new Area("rule buttons", RULE_BUTTONS));
        }

        /** Left edge of field block {@code column} (0 or 1) of a Make or Limit row. */
        public static int ruleFieldX(Rect group, int column) {
            int gap = group.w() - RULE_GROUP_LABEL - 2 * RULE_FIELD;
            return group.x() + RULE_GROUP_LABEL + column * (RULE_FIELD + gap);
        }

        /** The parts of one Rules tab row, relative to the row's top left corner. */
        public static List<Area> ruleRowAreas() {
            return List.of(new Area("rule row icon", RULE_ROW_ICON), new Area("rule row make", RULE_ROW_MAKE),
                    new Area("rule row refuse", RULE_ROW_REFUSE), new Area("rule row state", RULE_ROW_STATE));
        }

        /** The parts of the amount panel. */
        public static List<Area> craftAreas() {
            return List.of(new Area("craft title", CRAFT_TITLE), new Area("craft stepper", CRAFT_STEPPER),
                    new Area("craft lists", CRAFT_LISTS), new Area("craft buttons", CRAFT_BUTTONS));
        }

        /** Left edge of list {@code column} (0 to 2) of the amount panel. */
        public static int craftListX(int column) {
            return CRAFT_LISTS.x() + column * (CRAFT_LIST_WIDTH + (CRAFT_LISTS.w() - 3 * CRAFT_LIST_WIDTH) / 2);
        }

        /** The layout for a number of grid rows, at least {@link #BASE_ROWS}. */
        public static Layout layout(int rows) {
            int shown = Math.max(BASE_ROWS, rows);
            int top = -(shown - BASE_ROWS) * CELL;
            List<Rect> side = new ArrayList<>();
            for (int i = 0; i < SIDE_BUTTONS; i++) {
                side.add(new Rect(SIDE_X, top + 4 + i * SIDE_STEP, SIDE_SIZE, SIDE_SIZE));
            }
            return new Layout(shown, top, new Rect(8, top + 6, 112, 9), new Rect(RIGHT_SIDE_X, top + 4, SIDE_SIZE, SIDE_SIZE),
                    new Rect(122, top + 4, 60, 12), new Rect(8, top + 18, COLUMNS * CELL, shown * CELL),
                    new Rect(174, top + 18, 8, shown * CELL),
                    new Rect(8, top - TAB_STRIP, 174, TAB_STRIP), List.copyOf(side));
        }

        /**
         * Grid rows for a height setting on a screen this many GUI pixels
         * high: what the setting asks for, but no more than fit with the tab
         * strip and never fewer than {@link #BASE_ROWS}.
         */
        public static int rowsFor(TerminalSettings.Height height, int screenHeight) {
            int fit = BASE_ROWS + Math.max(0, (screenHeight - 4 - BASE_HEIGHT - TAB_STRIP) / CELL);
            return Math.max(BASE_ROWS, Math.min(height.rows(), fit));
        }

        /** Item origin of an order grid cell. */
        public static int orderX(int cell) {
            return ORDER_GRID.x() + (cell % 3) * CELL;
        }

        public static int orderY(int cell) {
            return ORDER_GRID.y() + (cell / 3) * CELL;
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
        public static final int TAB_GAP = TerminalLayout.TAB_GAP;

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

        /** The tabs on the tab row, see {@link TerminalLayout#tabs(Rect, int[])}. */
        public static List<Rect> tabs(int[] labelWidths) {
            return TerminalLayout.tabs(TABS, labelWidths);
        }

        /** A warehouse header of the Storage tab; parts below are relative to the row's top left corner on the list. */
        public static final int STORAGE_HEADER_HEIGHT = 14;
        /** A zone or fluid row of the Storage tab. */
        public static final int STORAGE_ROW_HEIGHT = 11;
        public static final Rect STORAGE_NAME = new Rect(2, 2, 110, 9);
        public static final Rect STORAGE_NUMBERS = new Rect(114, 2, 60, 9);
        public static final Rect STORAGE_BAR = new Rect(178, 3, 42, 6);
        /** Zone and fluid rows indent their name under the warehouse header. */
        public static final int STORAGE_INDENT = 8;

        /** The parts of a Storage tab row. */
        public static List<Area> storageAreas() {
            return List.of(new Area("storage name", STORAGE_NAME), new Area("storage numbers", STORAGE_NUMBERS),
                    new Area("storage bar", STORAGE_BAR));
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
