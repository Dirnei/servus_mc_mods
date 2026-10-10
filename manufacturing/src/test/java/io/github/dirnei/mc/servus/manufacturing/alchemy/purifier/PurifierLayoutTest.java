package io.github.dirnei.mc.servus.manufacturing.alchemy.purifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierLayout.Role;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Offset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PurifierLayoutTest {

    @Test
    void cells() {
        assertEquals(6, PurifierLayout.cells().size());
        assertEquals(Role.VAT, PurifierLayout.cell(0).role());
        assertEquals(Role.LOG, PurifierLayout.cell(4).role());
        assertEquals(new Offset(0, -1, 0), PurifierLayout.worldOffset(PurifierLayout.cell(Role.LOG), Facing.EAST));
    }

    @Test
    void pattern() {
        assertEquals(List.of("PVP", "BLR"), PurifierLayout.patternRows());
    }

    @Test
    void facingsCoverBothAxes() {
        assertEquals(4, PurifierLayout.FACINGS.size());
        assertEquals(new Offset(-1, 0, 0), PurifierLayout.worldOffset(PurifierLayout.cell(2), Facing.NORTH));
        assertEquals(new Offset(0, 0, -1), PurifierLayout.worldOffset(PurifierLayout.cell(2), Facing.EAST));
    }

    @Test
    void oppositeFacingIsTheMirror() {
        Offset basinNorth = PurifierLayout.worldOffset(PurifierLayout.cell(Role.BASIN), Facing.NORTH);
        Offset crockSouth = PurifierLayout.worldOffset(PurifierLayout.cell(Role.CROCK), Facing.SOUTH);
        assertEquals(basinNorth, crockSouth);
    }

    @Test
    void placement() {
        assertEquals("below the Vat", PurifierLayout.placement(PurifierLayout.cell(Role.LOG)));
        assertEquals("below the other plank", PurifierLayout.placement(PurifierLayout.cell(Role.CROCK)));
    }
}
