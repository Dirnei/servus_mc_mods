package io.github.dirnei.mc.servus.manufacturing.alchemy.purifier;

import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Offset;
import java.util.List;

/**
 * The fixed layout of the Purifier: a wall one block deep, 3 wide and 2 tall, seen from its
 * front with the Leaching Vat in the middle of the top row.
 *
 * <pre>
 *  _ V _     V Leaching Vat (the Purifier block), _ anything
 *  B L R     B Catch Basin, L log of any wood, R Residue Crock
 * </pre>
 *
 * Seen from the back it reads {@code R L B}, so checking all four facings also accepts the
 * mirrored build. Pure Java.
 */
public final class PurifierLayout {

    /** What stands at a cell. */
    public enum Role {
        VAT('V'), BASIN('B'), LOG('L'), CROCK('R');

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
            new Cell(1, Role.BASIN, -1, -1),
            new Cell(2, Role.LOG, 0, -1),
            new Cell(3, Role.CROCK, 1, -1));

    /** Every facing: two axes, each with the build and its mirror. */
    public static final List<Facing> FACINGS = List.of(Facing.NORTH, Facing.EAST, Facing.SOUTH, Facing.WEST);

    private PurifierLayout() {
    }

    public static List<Cell> cells() {
        return CELLS;
    }

    public static Cell cell(int index) {
        return CELLS.get(index);
    }

    public static Cell cell(Role role) {
        return CELLS.stream().filter(cell -> cell.role() == role).findFirst().orElseThrow();
    }

    public static Offset worldOffset(Cell cell, Facing facing) {
        return facing.offset(cell.right(), cell.up());
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
            case LOG -> "below the Vat";
            case BASIN -> "beside the log";
            case CROCK -> "on the other side of the log";
        };
    }
}
