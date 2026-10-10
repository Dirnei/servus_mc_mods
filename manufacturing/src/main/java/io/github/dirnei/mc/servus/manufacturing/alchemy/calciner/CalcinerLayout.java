package io.github.dirnei.mc.servus.manufacturing.alchemy.calciner;

import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Offset;
import java.util.List;
import java.util.Optional;

/**
 * The fixed layout of the Calciner structure: a wall one block deep, 5 wide and 5 tall,
 * described from its front with the Calciner (the furnace mouth) in the middle.
 *
 * <pre>
 *  D D H _ _     H Sulfur Hood, D Copper Duct
 *  D _ L   R     L Alembic, R Receiver, the gap is air for the spout
 *  T B 0 B T     0 Calciner, B Bricks, T Fluid Tank
 *  T B F B T     F Firebox
 *  T B A B T     A Ash Pan, _ any block
 * </pre>
 *
 * Right is the player's right when looking at the front. Pure Java: forming, the part
 * lookups and the Journal's schematic all read this one table.
 */
public final class CalcinerLayout {

    /** What stands at a cell. */
    public enum Role {
        CALCINER('0'), BRICK('B'), FIREBOX('F'), ASH_PAN('A'), ALEMBIC('L'), RECEIVER('R'), HOOD('H'),
        TANK('T'), DUCT('D'), AIR(' ');

        private final char symbol;

        Role(char symbol) {
            this.symbol = symbol;
        }

        /** The character of this role in the schematic pattern. */
        public char symbol() {
            return symbol;
        }
    }

    /** One position of the structure: {@code right} and {@code up} from the Calciner in the front plane. */
    public record Cell(int index, Role role, int right, int up) {
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
            new Cell(10, Role.RECEIVER, 2, 1),
            new Cell(11, Role.HOOD, 0, 2),
            // The Sulfur tank on the left, the Mercury tank on the right, each three Fluid Tanks.
            new Cell(12, Role.TANK, -2, 0),
            new Cell(13, Role.TANK, -2, -1),
            new Cell(14, Role.TANK, -2, -2),
            new Cell(15, Role.TANK, 2, 0),
            new Cell(16, Role.TANK, 2, -1),
            new Cell(17, Role.TANK, 2, -2),
            // The Copper Duct from the Hood across and down into the Sulfur tank.
            new Cell(18, Role.DUCT, -1, 2),
            new Cell(19, Role.DUCT, -2, 2),
            new Cell(20, Role.DUCT, -2, 1),
            // The Alembic's spout crosses this cell to the Receiver.
            new Cell(21, Role.AIR, 1, 1));

    /** The first duct cell; a duct block's piece is its cell index minus this. */
    public static final int FIRST_DUCT = 18;
    /** The lowest duct piece, which pours the Sulfur into its tank. */
    public static final int SULFUR_OUTLET = 20;

    private static final int MIN_RIGHT = -2;
    private static final int MAX_RIGHT = 2;
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

    /** The one cell of a part role; bricks, tanks and ducts have several cells, use {@link #cells(Role)} for them. */
    public static Cell part(Role role) {
        List<Cell> cells = cells(role);
        if (cells.size() != 1) {
            throw new IllegalArgumentException(role + " has " + cells.size() + " cells");
        }
        return cells.getFirst();
    }

    /** Every cell of {@code role}, in index order. */
    public static List<Cell> cells(Role role) {
        return CELLS.stream().filter(cell -> cell.role() == role).toList();
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
            // Toward the middle, past air and free corners, to the first part.
            int step = cell.right() < 0 ? 1 : -1;
            for (int right = cell.right() + step; ; right += step) {
                int at = right;
                Optional<Cell> anchor = CELLS.stream()
                        .filter(other -> other.right() == at && other.up() == cell.up() && other.role() != Role.AIR)
                        .findFirst();
                if (anchor.isPresent()) {
                    return new Placement(cell.right() < 0 ? Relation.LEFT_OF : Relation.RIGHT_OF, anchor.get().role());
                }
            }
        }
        int towardsCalciner = cell.up() > 0 ? -1 : 1;
        Role anchor = CELLS.stream()
                .filter(other -> other.right() == 0 && other.up() == cell.up() + towardsCalciner)
                .findFirst().orElseThrow().role();
        return new Placement(cell.up() > 0 ? Relation.ABOVE : Relation.BELOW, anchor);
    }
}
