package io.github.dirnei.mc.servus.alchemy.calciner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class CalcinationDimensionTest {

    @Test
    void everyIdReadsBack() {
        for (CalcinationDimension dimension : CalcinationDimension.values()) {
            assertEquals(Optional.of(dimension), CalcinationDimension.byId(dimension.id()));
        }
    }

    @Test
    void idsAreLowerCase() {
        assertEquals("overworld", CalcinationDimension.OVERWORLD.id());
        assertEquals("nether", CalcinationDimension.NETHER.id());
        assertEquals("end", CalcinationDimension.END.id());
    }

    @Test
    void unknownIdGivesNothing() {
        assertTrue(CalcinationDimension.byId("aether").isEmpty());
    }
}
