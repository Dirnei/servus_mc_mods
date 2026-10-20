package io.github.dirnei.mc.servus.brewery.brewhouse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Cell;
import io.github.dirnei.mc.servus.brewery.brewhouse.ModelSlicer.Part;
import io.github.dirnei.mc.servus.brewery.brewhouse.ModelSlicer.Piece;
import io.github.dirnei.mc.servus.brewery.brewhouse.ModelSlicer.Side;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModelSlicerTest {

    private static final float EPS = 1e-5F;

    private static Map<Side, String> allSides() {
        Map<Side, String> textures = new EnumMap<>(Side.class);
        for (Side side : Side.values()) {
            textures.put(side, "#t");
        }
        return textures;
    }

    private static Piece pieceIn(List<Piece> pieces, Cell cell) {
        return pieces.stream().filter(piece -> piece.cell() == cell).findFirst().orElseThrow();
    }

    @Test
    void cutAcrossColumnsAndRowsDropsTheCutFaces() {
        Part part = new Part("box", 10, 10, 0, 20, 20, 8, allSides(), false);
        List<Piece> pieces = ModelSlicer.slice(part);
        assertEquals(4, pieces.size());
        Piece frontLeft = pieceIn(pieces, Cell.KETTLE_FRONT_LEFT);
        assertEquals(EnumSet.of(Side.DOWN, Side.UP, Side.FRONT, Side.LEFT), frontLeft.sides());
        Piece backRight = pieceIn(pieces, Cell.KETTLE_BACK_RIGHT);
        assertEquals(EnumSet.of(Side.DOWN, Side.UP, Side.BACK, Side.RIGHT), backRight.sides());
    }

    @Test
    void uRunsWestInTheNorthModel() {
        Part part = new Part("box", 10, 10, 0, 20, 20, 8, allSides(), false);
        Piece frontLeft = pieceIn(ModelSlicer.slice(part), Cell.KETTLE_FRONT_LEFT);
        assertArrayEquals(new float[] {0, 0, 10, 6, 8, 16}, frontLeft.box(), EPS);
        Piece frontRight = pieceIn(ModelSlicer.slice(part), Cell.KETTLE_FRONT_RIGHT);
        assertArrayEquals(new float[] {12, 0, 10, 16, 8, 16}, frontRight.box(), EPS);
    }

    @Test
    void partsOverTwoLayersAreCutAtTheBlockTop() {
        Part kettle = BrewhouseModel.KETTLE;
        Piece low = pieceIn(ModelSlicer.slice(kettle), Cell.KETTLE_FRONT_LEFT);
        Piece high = pieceIn(ModelSlicer.slice(kettle), Cell.COPPER_FRONT_LEFT);
        assertEquals(12, low.y0(), EPS);
        assertEquals(16, low.y1(), EPS);
        assertEquals(0, high.y0(), EPS);
        assertEquals(14, high.y1(), EPS);
        assertEquals(EnumSet.of(Side.FRONT, Side.LEFT), low.sides());
        assertEquals(EnumSet.of(Side.UP, Side.FRONT, Side.LEFT), high.sides());
    }

    @Test
    void stretchedDoorShowsItsShareOfTheTexture() {
        Part door = BrewhouseModel.KETTLE_DOOR;
        Piece left = pieceIn(ModelSlicer.slice(door), Cell.KETTLE_FRONT_LEFT);
        Piece right = pieceIn(ModelSlicer.slice(door), Cell.KETTLE_FRONT_RIGHT);
        assertArrayEquals(new float[] {0, 0, 8, 16}, left.uv(Side.FRONT), EPS);
        assertArrayEquals(new float[] {8, 0, 16, 16}, right.uv(Side.FRONT), EPS);
        assertEquals(-BrewhouseModel.DOOR_DEPTH, left.z0(), EPS);
    }

    @Test
    void uvsAboveTheBlockAreShiftedIntoIt() {
        Piece pipe = pieceIn(ModelSlicer.slice(BrewhouseModel.PIPE), Cell.COPPER_FRONT_LEFT);
        assertEquals(18, pipe.y0(), EPS);
        assertEquals(32, pipe.y1(), EPS);
        float[] uv = pipe.uv(Side.FRONT);
        assertEquals(0, uv[1], EPS);
        assertEquals(14, uv[3], EPS);
    }

    @Test
    void uvsStraddlingABlockStartAtZero() {
        Piece dome = pieceIn(ModelSlicer.slice(BrewhouseModel.DOME), Cell.COPPER_FRONT_LEFT);
        float[] uv = dome.uv(Side.FRONT);
        assertEquals(0, uv[1], EPS);
        assertEquals(4, uv[3], EPS);
    }

    @Test
    void partOutsideTheLayoutIsRefused() {
        Part above = new Part("roof", 40, 0, 20, 44, 4, 24, allSides(), false);
        assertThrows(IllegalArgumentException.class, () -> ModelSlicer.slice(above));
    }

    @Test
    void clippingKeepsThePieceInsideItsBlock() {
        Piece door = pieceIn(ModelSlicer.slice(BrewhouseModel.KETTLE_DOOR), Cell.KETTLE_FRONT_LEFT);
        assertNull(door.clipped());
        Piece pipe = pieceIn(ModelSlicer.slice(BrewhouseModel.PIPE), Cell.COPPER_FRONT_LEFT);
        assertNull(pipe.clipped());
        Piece dome = pieceIn(ModelSlicer.slice(BrewhouseModel.DOME), Cell.COPPER_FRONT_LEFT);
        assertArrayEquals(new float[] {0, 14, 6, 10, 16, 16}, dome.clipped(), EPS);
    }

    @Test
    void everyPartOfTheBrewhouseFits() {
        for (Part part : BrewhouseModel.PARTS) {
            for (Piece piece : ModelSlicer.slice(part)) {
                for (float value : piece.box()) {
                    org.junit.jupiter.api.Assertions.assertTrue(value >= -16 && value <= 32, part.name() + " stays within element bounds");
                }
            }
        }
    }
}
