package io.github.dirnei.mc.servus.foundation.logistics.duct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class FluidFilterTest {

    private static final String WATER = "minecraft:water";
    private static final String LAVA = "minecraft:lava";

    @Test
    void emptyPassesAll() {
        FluidFilter filter = new FluidFilter();
        assertTrue(filter.isEmpty());
        assertTrue(filter.allows(WATER));
        assertTrue(filter.allows(LAVA));
    }

    @Test
    void listedPassesOnlyListed() {
        FluidFilter filter = new FluidFilter();
        filter.set(3, LAVA);
        assertTrue(filter.allows(LAVA));
        assertFalse(filter.allows(WATER));
        assertEquals(List.of(LAVA), filter.entries());
    }

    @Test
    void setAndClear() {
        FluidFilter filter = new FluidFilter();
        filter.set(0, WATER);
        assertEquals(WATER, filter.get(0));
        filter.clear(0);
        assertNull(filter.get(0));
        assertTrue(filter.isEmpty());
        filter.set(1, "  ");
        assertNull(filter.get(1));
    }

    @Test
    void firstFreeAfterGaps() {
        FluidFilter filter = new FluidFilter();
        filter.set(0, WATER);
        filter.set(2, LAVA);
        assertEquals(1, filter.firstFree());
        for (int slot = 0; slot < FluidFilter.SLOTS; slot++) {
            filter.set(slot, WATER);
        }
        assertEquals(FluidFilter.NONE, filter.firstFree());
    }

    @Test
    void outOfRangeIgnored() {
        FluidFilter filter = new FluidFilter();
        filter.set(-1, WATER);
        filter.set(FluidFilter.SLOTS, WATER);
        assertTrue(filter.isEmpty());
        assertNull(filter.get(FluidFilter.SLOTS));
    }
}
