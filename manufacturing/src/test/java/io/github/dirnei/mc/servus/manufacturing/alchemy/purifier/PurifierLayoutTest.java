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
        assertEquals(Role.BASIN, PurifierLayout.cell(3).role());
        assertEquals(new Offset(0, -1, 0), PurifierLayout.worldOffset(PurifierLayout.cell(3), Facing.EAST));
    }

    @Test
    void pattern() {
        assertEquals(List.of("PVP", "LBL"), PurifierLayout.patternRows());
    }

    @Test
    void axesCoverBothDirections() {
        assertEquals(new Offset(-1, 0, 0), PurifierLayout.worldOffset(PurifierLayout.cell(2), PurifierLayout.AXES.get(0)));
        assertEquals(new Offset(0, 0, -1), PurifierLayout.worldOffset(PurifierLayout.cell(2), PurifierLayout.AXES.get(1)));
    }

    @Test
    void placement() {
        assertEquals("below the Vat", PurifierLayout.placement(PurifierLayout.cell(3)));
        assertEquals("right of the Basin", PurifierLayout.placement(PurifierLayout.cell(5)));
    }
}
