package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProduceTest {

    @Test
    void wheatWinsOverMoreSeeds() {
        assertEquals(Optional.of("minecraft:wheat"),
                Produce.pick(Map.of("minecraft:wheat", 12L, "minecraft:wheat_seeds", 20L), "minecraft:wheat_seeds"));
    }

    @Test
    void carrotsOnly() {
        assertEquals(Optional.of("minecraft:carrot"), Produce.pick(Map.of("minecraft:carrot", 30L), "minecraft:carrot"));
    }

    @Test
    void tiesGoToTheSmallerId() {
        assertEquals(Optional.of("minecraft:beetroot"),
                Produce.pick(Map.of("minecraft:wheat", 5L, "minecraft:beetroot", 5L), null));
    }

    @Test
    void harvestOnlyTakesTheHighestCount() {
        assertEquals(Optional.of("minecraft:potato"),
                Produce.pick(Map.of("minecraft:potato", 9L, "minecraft:poisonous_potato", 1L), null));
    }

    @Test
    void cropCountsWhenOthersCameWithNone() {
        assertEquals(Optional.of("minecraft:carrot"),
                Produce.pick(Map.of("minecraft:carrot", 4L, "minecraft:stick", 0L), "minecraft:carrot"));
    }

    @Test
    void nothingHarvested() {
        assertTrue(Produce.pick(Map.of(), "minecraft:wheat_seeds").isEmpty());
    }
}
