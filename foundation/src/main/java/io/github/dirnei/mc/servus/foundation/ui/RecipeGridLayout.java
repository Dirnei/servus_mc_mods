package io.github.dirnei.mc.servus.foundation.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the parts of the recipe grid view sit, relative to the popup's top
 * left corner: the optional title row, the 3 by 3 ghost cells, the arrow, the
 * result, an extra place beside it for a screen's own widget, the mode button
 * under the result and the action row at the bottom. A title moves everything
 * above the action row down by one row. Pure, no Minecraft imports.
 */
public final class RecipeGridLayout {

    /** An area of the popup. The cells' area starts at the item origin of the first cell. */
    public record Box(int x, int y, int w, int h) {
        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }

        public boolean contains(double px, double py) {
            return px >= x && py >= y && px < right() && py < bottom();
        }

        public boolean overlaps(Box other) {
            return x < other.right() && other.x < right() && y < other.bottom() && other.y < bottom();
        }

        /** This box moved by an offset, for absolute screen coordinates. */
        public Box offset(int dx, int dy) {
            return new Box(x + dx, y + dy, w, h);
        }
    }

    public static final int CELL = 18;
    public static final int WIDTH = 236;
    public static final int HEIGHT = 90;
    /** The most actions the row holds: one on each side and two centred. */
    public static final int MAX_ACTIONS = 4;
    public static final int ACTION_WIDTH = 52;
    public static final int ACTION_GAP = 4;
    private static final int TITLE_SHIFT = 9;

    public static final Box POPUP = new Box(0, 0, WIDTH, HEIGHT);
    public static final Box TITLE = new Box(6, 4, 224, 9);
    public static final Box ACTIONS = new Box(6, 72, 224, 14);
    private static final Box CELLS = new Box(59, 6, 3 * CELL, 3 * CELL);
    private static final Box ARROW = new Box(118, 28, 16, 10);
    private static final Box RESULT = new Box(139, 25, 16, 16);
    private static final Box EXTRA = new Box(159, 24, 18, 18);
    private static final Box MODE = new Box(131, 44, 32, 14);

    private RecipeGridLayout() {
    }

    public static Box cells(boolean titled) {
        return shifted(CELLS, titled);
    }

    public static Box arrow(boolean titled) {
        return shifted(ARROW, titled);
    }

    public static Box result(boolean titled) {
        return shifted(RESULT, titled);
    }

    /** Beside the result, for a widget of the screen's own. */
    public static Box extra(boolean titled) {
        return shifted(EXTRA, titled);
    }

    public static Box mode(boolean titled) {
        return shifted(MODE, titled);
    }

    /** Item origin of a cell, 0 to 8 row by row, relative to the popup. */
    public static int cellX(int cell) {
        return CELLS.x() + (cell % 3) * CELL;
    }

    public static int cellY(int cell, boolean titled) {
        return cells(titled).y() + (cell / 3) * CELL;
    }

    /** The cell whose item area holds the point, relative to the popup; -1 elsewhere. */
    public static int cellAt(double x, double y, boolean titled) {
        Box cells = cells(titled);
        if (!cells.contains(x, y)) {
            return -1;
        }
        return (int) (y - cells.y()) / CELL * 3 + (int) (x - cells.x()) / CELL;
    }

    /**
     * The places of {@code count} actions on the action row: the first on the
     * left, the last on the right and the others centred between them.
     */
    public static List<Box> actions(int count) {
        if (count < 0 || count > MAX_ACTIONS) {
            throw new IllegalArgumentException("The action row holds 0 to " + MAX_ACTIONS + " actions, not " + count);
        }
        List<Box> boxes = new ArrayList<>(count);
        if (count == 0) {
            return boxes;
        }
        boxes.add(action(ACTIONS.x()));
        int middle = count - 2;
        int middleWidth = middle * ACTION_WIDTH + Math.max(0, middle - 1) * ACTION_GAP;
        int x = ACTIONS.x() + (ACTIONS.w() - middleWidth) / 2;
        for (int i = 0; i < middle; i++) {
            boxes.add(action(x + i * (ACTION_WIDTH + ACTION_GAP)));
        }
        if (count > 1) {
            boxes.add(action(ACTIONS.right() - ACTION_WIDTH));
        }
        return boxes;
    }

    /** Every fixed part, for tests and hit checks. */
    public static List<Box> parts(boolean titled) {
        List<Box> parts = new ArrayList<>(List.of(cells(titled), arrow(titled), result(titled), extra(titled), mode(titled), ACTIONS));
        if (titled) {
            parts.add(TITLE);
        }
        return parts;
    }

    private static Box action(int x) {
        return new Box(x, ACTIONS.y(), ACTION_WIDTH, ACTIONS.h());
    }

    private static Box shifted(Box box, boolean titled) {
        return titled ? box.offset(0, TITLE_SHIFT) : box;
    }
}
