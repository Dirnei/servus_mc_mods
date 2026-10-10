package io.github.dirnei.mc.servus.manufacturing.alchemy.purifier;

import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Offset;
import java.util.List;

/**
 * The fixed layout of the Purifier: a wall one block deep, 3 wide and 2 tall, seen from its
 * front with the Leaching Vat in the middle of the top row.
 *
 * <pre>
 *  P V P     V Leaching Vat (the Purifier block), P planks of any wood
 *  L B L     B Catch Basin, L logs of any wood
 * </pre>
 *
 * It reads the same from front and back, so only its axis matters. Pure Java.
 */
public final class PurifierLayout {

    /** What stands at a cell. */
    public enum Role {
        VAT('V'), PLANKS('P'), BASIN('B'), LOGS('L');

        private final char symbol;

        Role(char symbol) {
            this.symbol = symbol;
        }

        public char symbol() {
            return symbol;
        }
    }

    /** One position: {@code right} and {@code up} from the Vat in the front plane. */
    public record Cell(int index, Role role, int right, int up) {
    }

    private static final List<Cell> CELLS = List.of(
            new Cell(0, Role.VAT, 0, 0),
            new Cell(1, Role.PLANKS, -1, 0),
            new Cell(2, Role.PLANKS, 1, 0),
            new Cell(3, Role.BASIN, 0, -1),
            new Cell(4, Role.LOGS, -1, -1),
            new Cell(5, Role.LOGS, 1, -1));

    /** The two facings that cover both axes; the build is the same seen from the other side. */
    public static final List<Facing> AXES = List.of(Facing.NORTH, Facing.EAST);

    private PurifierLayout() {
    }

    public static List<Cell> cells() {
        return CELLS;
    }

    public static Cell cell(int index) {
        return CELLS.get(index);
    }

    public static Offset worldOffset(Cell cell, Facing facing) {
        return new Offset(cell.right() * facing.rightX(), cell.up(), cell.right() * facing.rightZ());
    }

    /** The front view top to bottom, each row left to right. */
    public static List<String> patternRows() {
        StringBuilder top = new StringBuilder("___");
        StringBuilder bottom = new StringBuilder("___");
        for (Cell cell : CELLS) {
            (cell.up() == 0 ? top : bottom).setCharAt(cell.right() + 1, cell.role().symbol());
        }
        return List.of(top.toString(), bottom.toString());
    }

    /** Where a cell sits, for the message when forming fails. */
    public static String placement(Cell cell) {
        return switch (cell.role()) {
            case VAT -> "itself";
            case BASIN -> "below the Vat";
            case PLANKS -> cell.right() < 0 ? "left of the Vat" : "right of the Vat";
            case LOGS -> cell.right() < 0 ? "left of the Basin" : "right of the Basin";
        };
    }
}
