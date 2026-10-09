package io.github.dirnei.mc.servus.manufacturing.alchemy.coagulator;

import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Offset;
import java.util.List;
import java.util.Optional;

/**
 * The fixed layout of the Coagulator structure: a wall one block deep, 3 wide and 2 tall, seen
 * from its front with the Coagulator in the middle of the bottom row.
 *
 * <pre>
 *  V V V     Principle Vats: Sulfur, Salt, Mercury
 *  S 0 S     0 Coagulator, S Stone Bricks
 * </pre>
 *
 * Sulfur is on the left and Mercury on the right, as at the Calciner. Pure Java.
 */
public final class CoagulatorLayout {

    /** What stands at a cell. */
    public enum Role {
        COAGULATOR('0'), STONE('S'), VAT('V');

        private final char symbol;

        Role(char symbol) {
            this.symbol = symbol;
        }

        public char symbol() {
            return symbol;
        }
    }

    /** One position: {@code right} and {@code up} from the Coagulator in the front plane. */
    public record Cell(int index, Role role, int right, int up) {
    }

    private static final List<Cell> CELLS = List.of(
            new Cell(0, Role.COAGULATOR, 0, 0),
            new Cell(1, Role.STONE, -1, 0),
            new Cell(2, Role.STONE, 1, 0),
            new Cell(3, Role.VAT, -1, 1),
            new Cell(4, Role.VAT, 0, 1),
            new Cell(5, Role.VAT, 1, 1));

    /** The first Vat cell; a Vat block's piece is its cell index minus this. */
    public static final int FIRST_VAT = 3;

    private CoagulatorLayout() {
    }

    public static List<Cell> cells() {
        return CELLS;
    }

    public static Cell cell(int index) {
        return CELLS.get(index);
    }

    /** The cell of the Vat of {@code piece}, 0 to 2 from left to right. */
    public static Cell vat(int piece) {
        return CELLS.get(FIRST_VAT + piece);
    }

    /** The principle a Vat cell holds: Sulfur on the left, Salt in the middle, Mercury on the right. */
    public static Principle principle(Cell cell) {
        if (cell.role() != Role.VAT) {
            throw new IllegalArgumentException(cell + " is no Vat");
        }
        return cell.right() < 0 ? Principle.SULFUR : cell.right() > 0 ? Principle.MERCURY : Principle.SALT;
    }

    public static Offset worldOffset(Cell cell, Facing facing) {
        return new Offset(cell.right() * facing.rightX(), cell.up(), cell.right() * facing.rightZ());
    }

    public static Offset controllerOffset(Cell cell, Facing facing) {
        return worldOffset(cell, facing).negate();
    }

    public static Optional<Cell> cellAt(Offset offset, Facing facing) {
        return CELLS.stream().filter(cell -> worldOffset(cell, facing).equals(offset)).findFirst();
    }

    /** The front view top to bottom, each row left to right. */
    public static List<String> patternRows() {
        StringBuilder top = new StringBuilder("___");
        StringBuilder bottom = new StringBuilder("___");
        for (Cell cell : CELLS) {
            (cell.up() == 1 ? top : bottom).setCharAt(cell.right() + 1, cell.role().symbol());
        }
        return List.of(top.toString(), bottom.toString());
    }

    /** "left of the Coagulator", "above the Stone Bricks": where a cell sits, told by the cell beside or below it. */
    public static String placement(Cell cell) {
        if (cell.role() == Role.COAGULATOR) {
            return "itself";
        }
        if (cell.up() == 0) {
            return cell.right() < 0 ? "left of the Coagulator" : "right of the Coagulator";
        }
        return cell.right() == 0 ? "above the Coagulator" : cell.right() < 0 ? "left of the middle Vat" : "right of the middle Vat";
    }
}
