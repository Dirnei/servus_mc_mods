package io.github.dirnei.mc.servus.delicacies.firebox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class HeatingStatsTest {

    @Test
    void emptyFileKeepsTheDefaults() {
        HeatingStats.Partial none = new HeatingStats.Partial(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
        assertEquals(HeatingStats.TILED_STOVE, none.over(HeatingStats.TILED_STOVE));
    }

    @Test
    void fieldsReplaceTheirDefaults() {
        HeatingStats.Partial some = new HeatingStats.Partial(Optional.of(50), Optional.empty(), Optional.of(100),
                Optional.empty(), Optional.of(0.8F));
        assertEquals(new HeatingStats(50, 4000, 100, 1200, 0.8F), some.over(HeatingStats.TILED_STOVE));
    }

    @Test
    void valuesAreKeptInRange() {
        HeatingStats.Partial wild = new HeatingStats.Partial(Optional.of(-5), Optional.of(-1), Optional.of(0),
                Optional.of(-3), Optional.of(4F));
        assertEquals(new HeatingStats(0, 0, 1, 1, 1), wild.over(HeatingStats.TILED_STOVE));
        HeatingStats.Partial small = new HeatingStats.Partial(Optional.of(100), Optional.of(10), Optional.empty(),
                Optional.empty(), Optional.of(0F));
        HeatingStats stats = small.over(HeatingStats.TILED_STOVE);
        assertEquals(100, stats.energyBuffer(), "the buffer holds at least one tick");
        assertEquals(0.01F, stats.hotAt(), 1e-6);
    }

    @Test
    void ratesPerTick() {
        HeatingStats stats = new HeatingStats(20, 4000, 200, 1200, 0.6F);
        assertEquals(1 / 200F, stats.heatUpPerTick(), 1e-8);
        assertEquals(1 / 1200F, stats.coolDownPerTick(), 1e-8);
    }
}
