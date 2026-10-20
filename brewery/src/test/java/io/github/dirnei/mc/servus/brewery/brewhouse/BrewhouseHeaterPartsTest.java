package io.github.dirnei.mc.servus.brewery.brewhouse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.dirnei.mc.servus.brewery.brewhouse.BrewhouseLayout.Cell;
import io.github.dirnei.mc.servus.brewery.brewhouse.ModelSlicer.Piece;
import io.github.dirnei.mc.servus.brewery.brewhouse.ModelSlicer.Side;
import java.util.List;
import org.junit.jupiter.api.Test;

class BrewhouseHeaterPartsTest {

    private static final float EPS = 1e-5F;

    @Test
    void gaugeSitsOnTheFrontBrickBetweenTheDoors() {
        List<Piece> pieces = ModelSlicer.slice(BrewhouseModel.GAUGE_DIAL);
        assertEquals(1, pieces.size());
        Piece gauge = pieces.get(0);
        assertEquals(Cell.KETTLE_FRONT_RIGHT, gauge.cell());
        assertArrayEquals(new float[] {0.5F, 3.5F, -0.25F, 5.5F, 8.5F, 0}, gauge.box(), EPS);
        assertFalse(gauge.sides().contains(Side.BACK), "no face against the bricks");
    }

    @Test
    void gaugeStaysClearOfBothDoors() {
        assertEquals(true, BrewhouseModel.GAUGE_DIAL.u0() > BrewhouseModel.KETTLE_DOOR.u1());
        assertEquals(true, BrewhouseModel.GAUGE_DIAL.u1() < BrewhouseModel.KILN_DOOR.u0());
    }

    @Test
    void portSticksOutOfTheBackKilnBricksRightEnd() {
        List<Piece> pieces = ModelSlicer.slice(BrewhouseModel.POWER_PORT);
        assertEquals(1, pieces.size());
        Piece port = pieces.get(0);
        assertEquals(Cell.KILN_BACK, port.cell());
        assertArrayEquals(new float[] {-1, 3, 5, 0, 9, 11}, port.box(), EPS);
        assertFalse(port.sides().contains(Side.LEFT), "no face against the bricks");
        assertNull(port.clipped(), "the port takes no part in the block shape");
    }

    @Test
    void fixedUvs() {
        assertArrayEquals(new float[] {0, 0, 16, 16}, BrewhouseModel.fixedUv(BrewhouseModel.POWER_PORT, Side.RIGHT), EPS);
        assertNull(BrewhouseModel.fixedUv(BrewhouseModel.POWER_PORT, Side.UP));
        assertArrayEquals(new float[] {0, 1, 16, 1.5F}, BrewhouseModel.fixedUv(BrewhouseModel.GAUGE_DIAL, Side.UP), EPS);
        assertNull(BrewhouseModel.fixedUv(BrewhouseModel.GAUGE_DIAL, Side.FRONT), "the face is stretched by the part rule");
        assertNull(BrewhouseModel.fixedUv(BrewhouseModel.KETTLE, Side.UP));
    }

    @Test
    void gaugeCentreInItsCell() {
        float[] centre = ModelSlicer.localPoint(Cell.KETTLE_FRONT_RIGHT, BrewhouseModel.GAUGE_U, 0, BrewhouseModel.GAUGE_Y);
        assertArrayEquals(new float[] {3, 6, 0}, centre, EPS);
    }
}
