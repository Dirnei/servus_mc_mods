package io.github.dirnei.mc.servus.alchemy.venter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.alchemy.structure.Offset;
import io.github.dirnei.mc.servus.alchemy.venter.VenterLayout.Cell;
import io.github.dirnei.mc.servus.alchemy.venter.VenterLayout.Piece;
import io.github.dirnei.mc.servus.alchemy.venter.VenterLayout.Role;
import org.junit.jupiter.api.Test;

class VenterLayoutTest {

    @Test
    void table() {
        assertEquals(9, VenterLayout.cells().size());
        assertEquals(Role.CUCURBIT, VenterLayout.cell(0).role());
        assertEquals(Role.HATCH, VenterLayout.cell(1).role());
        assertEquals(VenterLayout.FRAME_BLOCKS, VenterLayout.cells().stream().filter(cell -> cell.role() != Role.CUCURBIT).count());
        assertEquals(4, VenterLayout.cells().stream().filter(cell -> cell.piece() == Piece.CORNER).count());
        assertEquals(Piece.EDGE, VenterLayout.cell(1).piece());
    }

    @Test
    void pattern() {
        assertEquals(java.util.List.of("DDD", "DCD", "DHD"), VenterLayout.patternRows());
    }

    @Test
    void placements() {
        assertEquals("in the back left corner", VenterLayout.placement(VenterLayout.cell(6)));
        assertEquals("in the front right corner", VenterLayout.placement(VenterLayout.cell(3)));
        assertEquals("on the left side", VenterLayout.placement(VenterLayout.cell(4)));
        assertEquals("on the right side", VenterLayout.placement(VenterLayout.cell(5)));
        assertEquals("in the middle of the front", VenterLayout.placement(VenterLayout.cell(1)));
        assertEquals("in the middle of the back", VenterLayout.placement(VenterLayout.cell(7)));
        assertEquals("in the middle", VenterLayout.placement(VenterLayout.cell(0)));
    }

    @Test
    void groundInAllFacings() {
        assertEquals(new Offset(0, 0, -1), Facing.NORTH.ground(0, 1));
        assertEquals(new Offset(-1, 0, 0), Facing.NORTH.ground(1, 0));
        assertEquals(new Offset(1, 0, 0), Facing.EAST.ground(0, 1));
        assertEquals(new Offset(0, 0, -1), Facing.EAST.ground(1, 0));
        assertEquals(new Offset(0, 0, 1), Facing.SOUTH.ground(0, 1));
        assertEquals(new Offset(1, 0, 0), Facing.SOUTH.ground(1, 0));
        assertEquals(new Offset(-1, 0, 0), Facing.WEST.ground(0, 1));
        assertEquals(new Offset(0, 0, 1), Facing.WEST.ground(1, 0));
        assertEquals(new Offset(-2, 0, 3), Facing.SOUTH.ground(-2, 3));
    }

    @Test
    void turned() {
        assertEquals(Facing.EAST, Facing.NORTH.turned(1));
        assertEquals(Facing.WEST, Facing.NORTH.turned(-1));
        assertEquals(Facing.NORTH, Facing.WEST.turned(5));
    }

    @Test
    void everyPieceFacesAwayFromTheCucurbit() {
        for (Facing front : Facing.values()) {
            for (Cell cell : VenterLayout.cells()) {
                if (cell.piece() == Piece.NONE) {
                    continue;
                }
                Facing out = VenterLayout.pieceFacing(cell, front);
                Offset step = step(out);
                Offset expected = cell.piece() == Piece.EDGE ? step : add(step, step(out.turned(-1)));
                assertEquals(expected, VenterLayout.worldOffset(cell, front), cell + " facing " + front);
            }
        }
    }

    private static Offset step(Facing facing) {
        return facing.ground(0, 1);
    }

    private static Offset add(Offset a, Offset b) {
        return new Offset(a.x() + b.x(), a.y() + b.y(), a.z() + b.z());
    }
}
