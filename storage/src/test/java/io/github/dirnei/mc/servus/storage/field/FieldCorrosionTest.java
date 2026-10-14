package io.github.dirnei.mc.servus.storage.field;

import static io.github.dirnei.mc.servus.storage.field.FieldCorrosion.CORRODED;
import static io.github.dirnei.mc.servus.storage.field.FieldCorrosion.COPPER;
import static io.github.dirnei.mc.servus.storage.field.FieldCorrosion.EXPOSED;
import static io.github.dirnei.mc.servus.storage.field.FieldCorrosion.LOOSE;
import static io.github.dirnei.mc.servus.storage.field.FieldCorrosion.OXIDIZED;
import static io.github.dirnei.mc.servus.storage.field.FieldCorrosion.WEATHERED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class FieldCorrosionTest {

    @Test
    void stepsAndBounds() {
        assertEquals(EXPOSED, FieldCorrosion.next(COPPER));
        assertEquals(CORRODED, FieldCorrosion.next(CORRODED));
        assertEquals(EXPOSED, FieldCorrosion.next(LOOSE));
        assertEquals(OXIDIZED, FieldCorrosion.previous(CORRODED));
        assertEquals(COPPER, FieldCorrosion.previous(COPPER));
        assertEquals("weathered", FieldCorrosion.name(WEATHERED));
        assertEquals("copper", FieldCorrosion.name(LOOSE));
    }

    @Test
    void joinPrefersHead() {
        assertEquals(EXPOSED, FieldCorrosion.join(EXPOSED, List.of(OXIDIZED, WEATHERED)));
        assertEquals(CORRODED, FieldCorrosion.join(CORRODED, List.of(COPPER)));
    }

    @Test
    void joinTakesYoungest() {
        assertEquals(EXPOSED, FieldCorrosion.join(LOOSE, List.of(WEATHERED, EXPOSED, LOOSE, OXIDIZED)));
        assertEquals(WEATHERED, FieldCorrosion.join(LOOSE, List.of(LOOSE, WEATHERED)));
    }

    @Test
    void joinOfLooseRailsStaysLoose() {
        assertEquals(LOOSE, FieldCorrosion.join(LOOSE, List.of(LOOSE, LOOSE)));
        assertEquals(LOOSE, FieldCorrosion.join(LOOSE, List.of()));
    }

    @Test
    void agingCountsWater() {
        FieldCorrosion.Aged aged = FieldCorrosion.age(COPPER, 0, 300, 1000, 0);
        assertEquals(new FieldCorrosion.Aged(COPPER, 300, 0), aged);
        aged = FieldCorrosion.age(COPPER, 900, 300, 1000, 0);
        assertEquals(new FieldCorrosion.Aged(EXPOSED, 200, 0), aged);
        aged = FieldCorrosion.age(COPPER, 0, 2500, 1000, 0);
        assertEquals(new FieldCorrosion.Aged(WEATHERED, 500, 0), aged);
    }

    @Test
    void agingUsesUpkeepFirst() {
        assertEquals(new FieldCorrosion.Aged(WEATHERED, 0, 1), FieldCorrosion.age(WEATHERED, 0, 1000, 1000, 3));
        assertEquals(new FieldCorrosion.Aged(EXPOSED, 0, 2), FieldCorrosion.age(COPPER, 0, 3000, 1000, 2));
    }

    @Test
    void agingStopsAtCorroded() {
        assertEquals(new FieldCorrosion.Aged(CORRODED, 0, 0), FieldCorrosion.age(OXIDIZED, 0, 5000, 1000, 0));
        assertEquals(new FieldCorrosion.Aged(CORRODED, 0, 0), FieldCorrosion.age(CORRODED, 400, 5000, 1000, 4));
    }

    @Test
    void agingOffWithoutStep() {
        assertEquals(new FieldCorrosion.Aged(EXPOSED, 0, 0), FieldCorrosion.age(EXPOSED, 0, 100000, 0, 5));
    }

    @Test
    void progressPercent() {
        assertEquals(0, FieldCorrosion.progress(0, 1000));
        assertEquals(50, FieldCorrosion.progress(500, 1000));
        assertEquals(99, FieldCorrosion.progress(999, 1000));
        assertEquals(0, FieldCorrosion.progress(500, 0));
    }

    @Test
    void axeDurabilityCheck() {
        assertTrue(FieldCorrosion.axeCanScrape(true, 5, 5));
        assertFalse(FieldCorrosion.axeCanScrape(true, 3, 5));
        assertTrue(FieldCorrosion.axeCanScrape(false, 0, 128));
    }

    @Test
    void axeDamagePerRail() {
        assertEquals(10, FieldCorrosion.axeDamage(5, 2));
        assertEquals(0, FieldCorrosion.axeDamage(5, 0));
        assertEquals(Integer.MAX_VALUE, FieldCorrosion.axeDamage(Integer.MAX_VALUE, 2));
    }
}
