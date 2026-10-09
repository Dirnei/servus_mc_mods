package io.github.dirnei.mc.servus.manufacturing.alchemy.calciner;

import java.util.List;
import java.util.Optional;

/**
 * The fixed layout of the Calciner structure: a wall one block deep, 3 wide and 5 tall,
 * described from its front with the Calciner (the furnace mouth) in the middle row.
 *
 * <pre>
 *  _ H _     H Sulfur Hood
 *  _ L R     L Alembic, R Receiver
 *  B 0 B     0 Calciner, B Bricks
 *  B F B     F Firebox
 *  B A B     A Ash Pan, _ any block
 * </pre>
 *
 * Right is the player's right when looking at the front. Pure Java: forming, the part
 * lookups and the Journal's schematic all read this one table.
 */
public final class CalcinerLayout {

    /** What stands at a cell. */
    public enum Role {
        CALCINER('0'), BRICK('B'), FIREBOX('F'), ASH_PAN('A'), ALEMBIC('L'), RECEIVER('R'), HOOD('H');

        private final char symbol;

        Role(char symbol) {
            this.symbol = symbol;
        }

        /** The character of this role in the schematic pattern. */
        public char symbol() {
            return symbol;
        }
    }

    /** The side the front of the structure faces, with its x and z step. */
    public enum Facing {
        NORTH(0, -1), EAST(1, 0), SOUTH(0, 1), WEST(-1, 0);

        private final int dx;
        private final int dz;

        Facing(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }

        /** The x step to the player's right when they look at a front facing this way. */
        public int rightX() {
            return dz;
        }

        /** The z step to the player's right when they look at a front facing this way. */
        public int rightZ() {
            return -dx;
        }
    }

    /** One position of the structure: {@code right} and {@code up} from the Calciner in the front plane. */
    public record Cell(int index, Role role, int right, int up) {
    }

    /** A world offset in blocks. */
    public record Offset(int x, int y, int z) {

        public Offset negate() {
            return new Offset(-x, -y, -z);
        }
    }

    /** The character of a free position in the schematic pattern. */
    public static final char FREE = '_';

    private static final List<Cell> CELLS = List.of(
            new Cell(0, Role.CALCINER, 0, 0),
            new Cell(1, Role.BRICK, -1, 0),
            new Cell(2, Role.BRICK, 1, 0),
            new Cell(3, Role.BRICK, -1, -1),
            new Cell(4, Role.BRICK, 1, -1),
            new Cell(5, Role.BRICK, -1, -2),
            new Cell(6, Role.BRICK, 1, -2),
            new Cell(7, Role.FIREBOX, 0, -1),
            new Cell(8, Role.ASH_PAN, 0, -2),
            new Cell(9, Role.ALEMBIC, 0, 1),
            new Cell(10, Role.RECEIVER, 1, 1),
            new Cell(11, Role.HOOD, 0, 2));

    private static final int MIN_RIGHT = -1;
    private static final int MAX_RIGHT = 1;
    private static final int MIN_UP = -2;
    private static final int MAX_UP = 2;

    private CalcinerLayout() {
    }

    /** Every cell, the Calciner first. */
    public static List<Cell> cells() {
        return CELLS;
    }

    /** The cell at {@code index}. */
    public static Cell cell(int index) {
        return CELLS.get(index);
    }

    /** The one cell of a part role; bricks have six cells, use {@link #cell(int)} for them. */
    public static Cell part(Role role) {
        if (role == Role.BRICK) {
            throw new IllegalArgumentException("bricks have several cells");
        }
        return CELLS.stream().filter(cell -> cell.role() == role).findFirst().orElseThrow();
    }

    /** Where {@code cell} lies from the Calciner when the front faces {@code facing}. */
    public static Offset worldOffset(Cell cell, Facing facing) {
        return new Offset(cell.right() * facing.rightX(), cell.up(), cell.right() * facing.rightZ());
    }

    /** Where the Calciner lies from {@code cell} when the front faces {@code facing}. */
    public static Offset controllerOffset(Cell cell, Facing facing) {
        return worldOffset(cell, facing).negate();
    }

    /** The cell at {@code offset} from the Calciner, if the structure has one there. */
    public static Optional<Cell> cellAt(Offset offset, Facing facing) {
        return CELLS.stream().filter(cell -> worldOffset(cell, facing).equals(offset)).findFirst();
    }

    /** The front view top to bottom, each row left to right, as {@link Role#symbol()} or {@link #FREE}. */
    public static List<String> patternRows() {
        StringBuilder[] rows = new StringBuilder[MAX_UP - MIN_UP + 1];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = new StringBuilder(String.valueOf(FREE).repeat(MAX_RIGHT - MIN_RIGHT + 1));
        }
        for (Cell cell : CELLS) {
            rows[MAX_UP - cell.up()].setCharAt(cell.right() - MIN_RIGHT, cell.role().symbol());
        }
        return java.util.Arrays.stream(rows).map(StringBuilder::toString).toList();
    }

    /** How a cell lies next to its anchor part. */
    public enum Relation {
        LEFT_OF, RIGHT_OF, ABOVE, BELOW
    }

    /** Where a cell sits, told by a neighbouring part: "right of the Alembic". */
    public record Placement(Relation relation, Role anchor) {
    }

    /**
     * Where {@code cell} sits relative to a part beside it, for the message when forming
     * fails. The Calciner itself is its own anchor.
     */
    public static Placement placement(Cell cell) {
        if (cell.role() == Role.CALCINER) {
            return new Placement(Relation.ABOVE, Role.FIREBOX);
        }
        if (cell.right() != 0) {
            Role anchor = CELLS.stream()
                    .filter(other -> other.right() == 0 && other.up() == cell.up())
                    .findFirst().orElseThrow().role();
            return new Placement(cell.right() < 0 ? Relation.LEFT_OF : Relation.RIGHT_OF, anchor);
        }
        int towardsCalciner = cell.up() > 0 ? -1 : 1;
        Role anchor = CELLS.stream()
                .filter(other -> other.right() == 0 && other.up() == cell.up() + towardsCalciner)
                .findFirst().orElseThrow().role();
        return new Placement(cell.up() > 0 ? Relation.ABOVE : Relation.BELOW, anchor);
    }
}
