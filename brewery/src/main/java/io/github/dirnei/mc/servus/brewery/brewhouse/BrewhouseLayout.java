package io.github.dirnei.mc.servus.brewery.brewhouse;

import java.util.Optional;

/**
 * Where the ten parts of a Brewhouse stand. Pure Java. A cell is given by its column (0 to 2, left to right seen from
 * the front), row (0 front, 1 back) and layer (0 the bricks, 1 the kettle blocks on the left two by two).
 */
public final class BrewhouseLayout {

    /** Columns, rows and layers of the layout. */
    public static final int COLUMNS = 3;
    public static final int ROWS = 2;
    public static final int LAYERS = 2;

    /** Which block a cell needs. */
    public enum Kind {
        BRICK,
        KETTLE
    }

    /** The cells of a Brewhouse; the first is the master. */
    public enum Cell {
        KETTLE_FRONT_LEFT(0, 0, 0, Kind.BRICK),
        KETTLE_FRONT_RIGHT(1, 0, 0, Kind.BRICK),
        KETTLE_BACK_LEFT(0, 1, 0, Kind.BRICK),
        KETTLE_BACK_RIGHT(1, 1, 0, Kind.BRICK),
        KILN_FRONT(2, 0, 0, Kind.BRICK),
        KILN_BACK(2, 1, 0, Kind.BRICK),
        COPPER_FRONT_LEFT(0, 0, 1, Kind.KETTLE),
        COPPER_FRONT_RIGHT(1, 0, 1, Kind.KETTLE),
        COPPER_BACK_LEFT(0, 1, 1, Kind.KETTLE),
        COPPER_BACK_RIGHT(1, 1, 1, Kind.KETTLE);

        private final int column;
        private final int row;
        private final int layer;
        private final Kind kind;

        Cell(int column, int row, int layer, Kind kind) {
            this.column = column;
            this.row = row;
            this.layer = layer;
            this.kind = kind;
        }

        public int column() {
            return column;
        }

        public int row() {
            return row;
        }

        public int layer() {
            return layer;
        }

        public Kind kind() {
            return kind;
        }

        /** Whether this brick is a kiln tray. */
        public boolean isKiln() {
            return this == KILN_FRONT || this == KILN_BACK;
        }

        /** Whether this brick lies under the kettle. */
        public boolean underKettle() {
            return kind == Kind.BRICK && !isKiln();
        }

        /** Whether this brick carries (a part of) a fire door. */
        public boolean hasDoor() {
            return kind == Kind.BRICK && row == 0;
        }

        /** The cell at a column, row and layer, if the layout has one there. */
        public static Optional<Cell> at(int column, int row, int layer) {
            for (Cell cell : values()) {
                if (cell.column == column && cell.row == row && cell.layer == layer) {
                    return Optional.of(cell);
                }
            }
            return Optional.empty();
        }
    }

    /** Asks whether a loose block of a kind stands at an offset from the clicked block, in columns, rows and layers. */
    @FunctionalInterface
    public interface Probe {
        boolean looseAt(int columns, int rows, int layers, Kind kind);
    }

    /** The master cell. */
    public static final Cell MASTER = Cell.KETTLE_FRONT_LEFT;

    private BrewhouseLayout() {
    }

    /**
     * The cell the clicked block takes in a complete layout around it, or empty when no layout fits. Cells of the
     * clicked kind are tried in order, so a front row cell wins over a back row one.
     */
    public static Optional<Cell> find(Kind clicked, Probe probe) {
        for (Cell candidate : Cell.values()) {
            if (candidate.kind == clicked && fits(candidate, probe)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static boolean fits(Cell clickedAs, Probe probe) {
        for (Cell cell : Cell.values()) {
            if (!probe.looseAt(cell.column - clickedAs.column, cell.row - clickedAs.row, cell.layer - clickedAs.layer, cell.kind)) {
                return false;
            }
        }
        return true;
    }
}
