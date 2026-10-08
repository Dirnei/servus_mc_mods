package io.github.dirnei.mc.servus.foundation.logistics.duct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class DuctPartsTest {

    @Test
    void masksFollowLineOrdinals() {
        assertEquals(1, DuctParts.ITEM);
        assertEquals(2, DuctParts.ENERGY);
        assertEquals(4, DuctParts.FLUID);
        assertEquals(7, DuctParts.ALL);
    }

    @Test
    void sevenLineSets() {
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7), DuctParts.lineSets());
    }

    @Test
    void suffixJoinsLineIdsInOrdinalOrder() {
        assertEquals("item", DuctParts.suffix(DuctParts.ITEM));
        assertEquals("energy", DuctParts.suffix(DuctParts.ENERGY));
        assertEquals("fluid", DuctParts.suffix(DuctParts.FLUID));
        assertEquals("item_energy", DuctParts.suffix(DuctParts.ITEM | DuctParts.ENERGY));
        assertEquals("item_fluid", DuctParts.suffix(DuctParts.ITEM | DuctParts.FLUID));
        assertEquals("energy_fluid", DuctParts.suffix(DuctParts.ENERGY | DuctParts.FLUID));
        assertEquals("item_energy_fluid", DuctParts.suffix(DuctParts.ALL));
    }

    @Test
    void emptyAndUnknownBitsReadAsKnownLines() {
        assertEquals("item", DuctParts.suffix(0));
        assertEquals("energy", DuctParts.suffix(DuctParts.ENERGY | 8));
    }

    @Test
    void linesOfASet() {
        assertEquals(List.of(Line.ITEM, Line.FLUID), DuctParts.lines(DuctParts.ITEM | DuctParts.FLUID));
        assertEquals(List.of(Line.ITEM, Line.ENERGY, Line.FLUID), DuctParts.lines(DuctParts.ALL));
    }

    @Test
    void ringTintsCoverEveryLine() {
        for (Line line : Line.VALUES) {
            for (int side = 0; side < 6; side++) {
                int tint = DuctParts.RING_TINT_BASE + line.ordinal() * 6 + side;
                assertEquals(line, DuctParts.ringLine(tint));
            }
        }
        assertNull(DuctParts.ringLine(DuctParts.RING_TINT_BASE - 1));
        assertNull(DuctParts.ringLine(DuctParts.RING_TINT_BASE + Line.VALUES.length * 6));
    }

    @Test
    void lineIdsRoundTrip() {
        for (Line line : Line.VALUES) {
            assertEquals(line, Line.byId(line.id()));
        }
        assertEquals(Line.FLUID, Line.byId("fluid"));
        assertEquals(3, new HashSet<>(List.of(Line.VALUES)).size());
    }
}
