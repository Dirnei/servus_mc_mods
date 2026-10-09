package io.github.dirnei.mc.servus.manufacturing.alchemy.coagulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.coagulator.CoagulatorLayout.Cell;
import io.github.dirnei.mc.servus.manufacturing.alchemy.coagulator.CoagulatorLayout.Role;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Offset;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoagulatorLayoutTest {

    @Test
    void cells() {
        assertEquals(6, CoagulatorLayout.cells().size());
        assertEquals(Role.COAGULATOR, CoagulatorLayout.cell(0).role());
        assertEquals(2, CoagulatorLayout.cells().stream().filter(cell -> cell.role() == Role.STONE).count());
        for (int piece = 0; piece < 3; piece++) {
            assertEquals(Role.VAT, CoagulatorLayout.vat(piece).role());
        }
    }

    @Test
    void vatPrinciplesMatchTheCalciner() {
        assertEquals(Principle.SULFUR, CoagulatorLayout.principle(CoagulatorLayout.vat(0)));
        assertEquals(Principle.SALT, CoagulatorLayout.principle(CoagulatorLayout.vat(1)));
        assertEquals(Principle.MERCURY, CoagulatorLayout.principle(CoagulatorLayout.vat(2)));
        assertThrows(IllegalArgumentException.class, () -> CoagulatorLayout.principle(CoagulatorLayout.cell(1)));
    }

    @Test
    void pattern() {
        assertEquals(List.of("VVV", "S0S"), CoagulatorLayout.patternRows());
    }

    @Test
    void roundTrips() {
        for (Facing facing : Facing.values()) {
            for (Cell cell : CoagulatorLayout.cells()) {
                Offset offset = CoagulatorLayout.worldOffset(cell, facing);
                assertEquals(offset.negate(), CoagulatorLayout.controllerOffset(cell, facing));
                assertEquals(cell, CoagulatorLayout.cellAt(offset, facing).orElseThrow());
            }
        }
        // Facing north the player's right is west: the Mercury Vat stands west of the Salt Vat.
        assertEquals(new Offset(-1, 1, 0), CoagulatorLayout.worldOffset(CoagulatorLayout.vat(2), Facing.NORTH));
    }

    @Test
    void placement() {
        assertEquals("right of the middle Vat", CoagulatorLayout.placement(CoagulatorLayout.vat(2)));
        assertEquals("above the Coagulator", CoagulatorLayout.placement(CoagulatorLayout.vat(1)));
        assertEquals("left of the Coagulator", CoagulatorLayout.placement(CoagulatorLayout.cell(1)));
    }
}
