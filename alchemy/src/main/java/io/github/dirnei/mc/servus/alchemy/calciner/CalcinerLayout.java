package io.github.dirnei.mc.servus.alchemy.calciner;

import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.alchemy.structure.Offset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The fixed layout of the Calciner structure: a wall one block deep, 3 wide and 5 tall,
 * described from its front with the Calciner (the furnace mouth) in the middle.
 *
 * <pre>
 *  D H _     H Sulfur Hood, D Copper Duct, _ any block
 *  D L R     L Alembic, R Receiver
 *  S 0 S     0 Calciner, S Stone Bricks
 *  T F T     F Firebox, T Fluid Tank
 *  E A E     A Ash Pan, E Deepslate Tiles
 * </pre>
 *
 * Right is the player's right when looking at the front. Pure Java: forming, the part
 * lookups and the Journal's schematic all read this one table.
 */
public final class CalcinerLayout {

    /** What stands at a cell. */
    public enum Role {
        CALCINER('0'), STONE_BRICKS('S'), DEEPSLATE_TILES('E'), FIREBOX('F'), ASH_PAN('A'), ALEMBIC('L'),
        RECEIVER('R'), HOOD('H'), TANK('T'), DUCT('D');

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
            new Cell(1, Role.STONE_BRICKS, -1, 0),
            new Cell(2, Role.STONE_BRICKS, 1, 0),
            new Cell(3, Role.DEEPSLATE_TILES, -1, -2),
            new Cell(4, Role.DEEPSLATE_TILES, 1, -2),
            new Cell(5, Role.FIREBOX, 0, -1),
            new Cell(6, Role.ASH_PAN, 0, -2),
            new Cell(7, Role.ALEMBIC, 0, 1),
            new Cell(8, Role.RECEIVER, 1, 1),
            new Cell(9, Role.HOOD, 0, 2),
            new Cell(10, Role.TANK, -1, -1),
            new Cell(11, Role.TANK, 1, -1),
            // The Copper Duct from the Hood across and down into the left Stone Bricks.
            new Cell(12, Role.DUCT, -1, 2),
            new Cell(13, Role.DUCT, -1, 1));

    /** The first duct cell; a duct block's piece is its cell index minus this. */
    public static final int FIRST_DUCT = 12;
    /** The lower duct piece, which leads the Sulfur down into the stone. */
    public static final int SULFUR_OUTLET = 13;
    /** The Fluid Tank the Calciner fills with Sulfur. */
    public static final int SULFUR_TANK = 10;
    /** The Fluid Tank the Calciner fills with Mercury. */
    public static final int MERCURY_TANK = 11;

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

    public static Cell cell(int index) {
        return CELLS.get(index);
    }

    /** The one cell of a part role; stones, tanks and ducts have several cells, use {@link #cells(Role)} for them. */
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
        return facing.offset(cell.right(), cell.up());
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
        return Arrays.stream(rows).map(StringBuilder::toString).toList();
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
            // Toward the middle, past free corners, to the first part.
            int step = cell.right() < 0 ? 1 : -1;
            for (int right = cell.right() + step; ; right += step) {
                int at = right;
                Optional<Cell> anchor = CELLS.stream()
                        .filter(other -> other.right() == at && other.up() == cell.up())
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
