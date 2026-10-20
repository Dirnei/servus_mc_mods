package io.github.dirnei.mc.servus.brewery.brewhouse;

import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Cell;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cuts the one big Brewhouse model into the block models of its cells. Pure Java.
 *
 * <p>Parts are boxes in structure pixels: {@code u} from the left (0 to 48), {@code v} from the front (0 to 32),
 * {@code y} up. Parts are cut only at the inner cell borders, so a part may stick out of the outer cells. A piece lives
 * in its cell's model space, built facing north: the front is north and the left is east.
 */
public final class ModelSlicer {

    private static final float CELL = 16;

    /** The sides of a box in structure terms. */
    public enum Side {
        DOWN,
        UP,
        FRONT,
        BACK,
        LEFT,
        RIGHT
    }

    /**
     * A box of the big model.
     *
     * @param textures       the texture of each side that is drawn; a side missing here is never drawn
     * @param frontStretched whether the front shows the whole texture over the whole part instead of tiling it
     */
    public record Part(String name, float u0, float v0, float y0, float u1, float v1, float y1, Map<Side, String> textures,
                       boolean frontStretched) {
    }

    /**
     * One part's piece in one cell, in that cell's model pixels.
     *
     * @param sides the sides drawn: the part's drawn sides that are not on a cut
     */
    public record Piece(Part part, Cell cell, float x0, float y0, float z0, float x1, float y1, float z1, Set<Side> sides) {

        public float[] box() {
            return new float[] {x0, y0, z0, x1, y1, z1};
        }

        /** The piece cut to its block, for a block shape, or null when nothing of it lies inside. */
        public float[] clipped() {
            float[] box = {clamp(x0), clamp(y0), clamp(z0), clamp(x1), clamp(y1), clamp(z1)};
            return box[0] < box[3] && box[1] < box[4] && box[2] < box[5] ? box : null;
        }

        public String texture(Side side) {
            return part.textures().get(side);
        }

        /**
         * The UVs {u0, v0, u1, v1} of a side: the vanilla rule for a box face, shifted by whole blocks into 0 to 16, or
         * the part's share of the whole texture on a stretched front.
         */
        public float[] uv(Side side) {
            if (side == Side.FRONT && part.frontStretched()) {
                float lowU = 16 - x1 + 16 * cell.column();
                float highU = 16 - x0 + 16 * cell.column();
                float lowY = y0 + 16 * cell.layer();
                float highY = y1 + 16 * cell.layer();
                float width = part.u1() - part.u0();
                float height = part.y1() - part.y0();
                return new float[] {(lowU - part.u0()) / width * 16, (part.y1() - highY) / height * 16,
                        (highU - part.u0()) / width * 16, (part.y1() - lowY) / height * 16};
            }
            float[] uv = switch (side) {
                case DOWN -> new float[] {x0, 16 - z1, x1, 16 - z0};
                case UP -> new float[] {x0, z0, x1, z1};
                case FRONT -> new float[] {16 - x1, 16 - y1, 16 - x0, 16 - y0};
                case BACK -> new float[] {x0, 16 - y1, x1, 16 - y0};
                case RIGHT -> new float[] {z0, 16 - y1, z1, 16 - y0};
                case LEFT -> new float[] {16 - z1, 16 - y1, 16 - z0, 16 - y0};
            };
            float[] u = intoBlock(uv[0], uv[2]);
            float[] v = intoBlock(uv[1], uv[3]);
            return new float[] {u[0], v[0], u[1], v[1]};
        }

        private static float clamp(float value) {
            return Math.max(0, Math.min(CELL, value));
        }

        private static float[] intoBlock(float low, float high) {
            float shift = (float) Math.floor(low / CELL) * CELL;
            float a = low - shift;
            float b = high - shift;
            return b <= CELL ? new float[] {a, b} : new float[] {0, high - low};
        }
    }

    private ModelSlicer() {
    }

    /** The pieces of {@code part}, one per cell it reaches; a part reaching a cell the layout lacks is refused. */
    public static List<Piece> slice(Part part) {
        List<Piece> pieces = new ArrayList<>();
        for (int column = 0; column < BrewhouseLayout.COLUMNS; column++) {
            float lowU = Math.max(part.u0(), column == 0 ? Float.NEGATIVE_INFINITY : column * CELL);
            float highU = Math.min(part.u1(), column == BrewhouseLayout.COLUMNS - 1 ? Float.POSITIVE_INFINITY : (column + 1) * CELL);
            for (int row = 0; row < BrewhouseLayout.ROWS; row++) {
                float lowV = Math.max(part.v0(), row == 0 ? Float.NEGATIVE_INFINITY : row * CELL);
                float highV = Math.min(part.v1(), row == BrewhouseLayout.ROWS - 1 ? Float.POSITIVE_INFINITY : (row + 1) * CELL);
                for (int layer = 0; layer < BrewhouseLayout.LAYERS; layer++) {
                    float lowY = Math.max(part.y0(), layer == 0 ? Float.NEGATIVE_INFINITY : layer * CELL);
                    float highY = Math.min(part.y1(), layer == BrewhouseLayout.LAYERS - 1 ? Float.POSITIVE_INFINITY : (layer + 1) * CELL);
                    if (lowU < highU && lowV < highV && lowY < highY) {
                        pieces.add(piece(part, column, row, layer, lowU, lowV, lowY, highU, highV, highY));
                    }
                }
            }
        }
        return pieces;
    }

    /** The pieces of {@code parts} that lie in {@code cell}. */
    public static List<Piece> piecesIn(List<Part> parts, Cell cell) {
        List<Piece> pieces = new ArrayList<>();
        for (Part part : parts) {
            for (Piece piece : slice(part)) {
                if (piece.cell() == cell) {
                    pieces.add(piece);
                }
            }
        }
        return pieces;
    }

    /** A structure point in the model pixels of {@code cell}, as {x, y, z}. */
    public static float[] localPoint(Cell cell, float u, float v, float y) {
        return new float[] {CELL - (u - CELL * cell.column()), y - CELL * cell.layer(), v - CELL * cell.row()};
    }

    private static Piece piece(Part part, int column, int row, int layer, float lowU, float lowV, float lowY,
                               float highU, float highV, float highY) {
        Cell cell = Cell.at(column, row, layer).orElseThrow(() ->
                new IllegalArgumentException(part.name() + " reaches column " + column + ", row " + row + ", layer " + layer
                        + ", which the Brewhouse does not have"));
        Set<Side> sides = EnumSet.noneOf(Side.class);
        for (Side side : part.textures().keySet()) {
            boolean outer = switch (side) {
                case LEFT -> lowU == part.u0();
                case RIGHT -> highU == part.u1();
                case FRONT -> lowV == part.v0();
                case BACK -> highV == part.v1();
                case DOWN -> lowY == part.y0();
                case UP -> highY == part.y1();
            };
            if (outer) {
                sides.add(side);
            }
        }
        float[] low = localPoint(cell, highU, lowV, lowY);
        float[] high = localPoint(cell, lowU, highV, highY);
        return new Piece(part, cell, low[0], low[1], low[2], high[0], high[1], high[2], sides);
    }
}
