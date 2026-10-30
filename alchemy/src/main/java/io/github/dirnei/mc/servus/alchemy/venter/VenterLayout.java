package io.github.dirnei.mc.servus.alchemy.venter;

import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.alchemy.structure.Offset;
import java.util.List;

/**
 * The fixed layout of the Venter Equinus: a bed of 3 by 3 blocks on one level, seen from above with
 * its front at the bottom.
 *
 * <pre>
 *  D D D     D Dung Bed
 *  D C D     C Cucurbit (the controller)
 *  D H D     H Bed Hatch, in the middle of the front
 * </pre>
 *
 * Every frame block draws a piece of the bed's frame: an edge (a wall) or a corner (a post with two
 * walls). Its {@code turns} are the clockwise quarter turns of the piece's outward side from the
 * build's front. Pure Java.
 */
public final class VenterLayout {

    /** What stands at a cell. */
    public enum Role {
        CUCURBIT('C'), HATCH('H'), BED('D');

        private final char symbol;

        Role(char symbol) {
            this.symbol = symbol;
        }

        public char symbol() {
            return symbol;
        }
    }

    /** The frame piece a cell draws while formed. */
    public enum Piece {
        NONE, EDGE, CORNER
    }

    /** One position: {@code right} to the player's right and {@code forward} toward the front, from the Cucurbit. */
    public record Cell(int index, Role role, int right, int forward, Piece piece, int turns) {
    }

    private static final List<Cell> CELLS = List.of(
            new Cell(0, Role.CUCURBIT, 0, 0, Piece.NONE, 0),
            new Cell(1, Role.HATCH, 0, 1, Piece.EDGE, 0),
            new Cell(2, Role.BED, -1, 1, Piece.CORNER, 1),
            new Cell(3, Role.BED, 1, 1, Piece.CORNER, 0),
            new Cell(4, Role.BED, -1, 0, Piece.EDGE, 1),
            new Cell(5, Role.BED, 1, 0, Piece.EDGE, 3),
            new Cell(6, Role.BED, -1, -1, Piece.CORNER, 2),
            new Cell(7, Role.BED, 0, -1, Piece.EDGE, 2),
            new Cell(8, Role.BED, 1, -1, Piece.CORNER, 3));

    /** The blocks of the frame around the Cucurbit; the bed holds one bedding item per frame block. */
    public static final int FRAME_BLOCKS = 8;

    private VenterLayout() {
    }

    public static List<Cell> cells() {
        return CELLS;
    }

    public static Cell cell(int index) {
        return CELLS.get(index);
    }

    public static Offset worldOffset(Cell cell, Facing front) {
        return front.ground(cell.right(), cell.forward());
    }

    /** The outward side of a cell's frame piece for a build whose front faces {@code front}. */
    public static Facing pieceFacing(Cell cell, Facing front) {
        return front.turned(cell.turns());
    }

    /** The bed from above, back row first, each row left to right. */
    public static List<String> patternRows() {
        StringBuilder[] rows = {new StringBuilder("___"), new StringBuilder("___"), new StringBuilder("___")};
        for (Cell cell : CELLS) {
            rows[cell.forward() + 1].setCharAt(cell.right() + 1, cell.role().symbol());
        }
        return List.of(rows[0].toString(), rows[1].toString(), rows[2].toString());
    }

    /** Where a cell sits, for the message when forming fails. */
    public static String placement(Cell cell) {
        String side = cell.right() < 0 ? "left" : "right";
        String end = cell.forward() > 0 ? "front" : "back";
        if (cell.role() == Role.CUCURBIT) {
            return "in the middle";
        }
        if (cell.piece() == Piece.CORNER) {
            return "in the " + end + " " + side + " corner";
        }
        return cell.right() == 0 ? "in the middle of the " + end : "on the " + side + " side";
    }
}
