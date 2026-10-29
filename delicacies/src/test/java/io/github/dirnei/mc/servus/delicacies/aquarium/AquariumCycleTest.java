package io.github.dirnei.mc.servus.delicacies.aquarium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AquariumCycleTest {

    private static final AquariumStats STATS = AquariumStats.DEFAULTS;
    private static final double EPSILON = 1e-6;

    /** A powered, fed, clean aquarium with room for {@code capacity} fish and no catch. */
    private static AquariumCycle.Input clean(double stock, int capacity) {
        return new AquariumCycle.Input(stock, (long) capacity * STATS.waterPerFish(), 0, 64, 0, 512, 1, 0, false, 0);
    }

    @Test
    void growsTowardCapacity() {
        AquariumCycle.Output out = AquariumCycle.run(clean(20, 100), STATS);
        assertEquals(21.92, out.stock(), 1e-3);
        assertEquals(0, out.died(), EPSILON);
    }

    @Test
    void growthStopsAtCapacity() {
        AquariumCycle.Output out = AquariumCycle.run(clean(100, 100), STATS);
        assertEquals(100, out.stock(), EPSILON);
    }

    @Test
    void aSingleFishDoesNotBreed() {
        AquariumCycle.Output out = AquariumCycle.run(clean(1, 100), STATS);
        assertEquals(1, out.stock(), EPSILON);
    }

    @Test
    void unfedFishDoNotGrow() {
        AquariumCycle.Input in = new AquariumCycle.Input(20, 100L * STATS.waterPerFish(), 0, 0, 0, 0, 1, 0, false, 0);
        AquariumCycle.Output out = AquariumCycle.run(in, STATS);
        assertEquals(20, out.stock(), EPSILON);
        assertEquals(1, out.unfedCycles());
        assertEquals(0, out.feedUsed());
    }

    @Test
    void starvingAfterTheLimitLosesAShare() {
        AquariumCycle.Input in = new AquariumCycle.Input(40, 100L * STATS.waterPerFish(), 0, 0, 3, 0, 1, 0, false, 0);
        AquariumCycle.Output out = AquariumCycle.run(in, STATS);
        assertEquals(4, out.unfedCycles());
        assertEquals(38, out.stock(), EPSILON);
        assertEquals(2, out.died(), EPSILON);
    }

    @Test
    void fishAndFeedAddDirt() {
        AquariumCycle.Input in = new AquariumCycle.Input(32, 128_000, 0, 64, 0, 0, 0, 0, false, 0);
        AquariumCycle.Output out = AquariumCycle.run(in, STATS);
        assertEquals(4, out.feedUsed());
        assertEquals(32 * 0.5 + 4, out.dirt(), EPSILON);
        assertEquals(1 - 20.0 / 1280, out.quality(), EPSILON);
    }

    @Test
    void qualityFormula() {
        assertEquals(0.5, AquariumCycle.quality(500, 100_000, STATS), EPSILON);
        assertEquals(0, AquariumCycle.quality(5000, 100_000, STATS), EPSILON);
        assertEquals(0, AquariumCycle.quality(0, 0, STATS), EPSILON);
    }

    @Test
    void dirtyWaterKillsAShare() {
        long water = 400_000;
        double dirt = water / 1000.0 * STATS.dirtPerBucket() * 0.85 - 50 * 0.5 - 5;
        AquariumCycle.Input in = new AquariumCycle.Input(50, water, dirt, 64, 0, 0, 0, 0, false, 0);
        AquariumCycle.Output out = AquariumCycle.run(in, STATS);
        assertEquals(0.15, out.quality(), 1e-3);
        assertEquals(45, out.stock(), EPSILON);
    }

    @Test
    void drainingClampsToTheNewCapacity() {
        AquariumCycle.Input in = new AquariumCycle.Input(60, 120_000, 0, 64, 0, 0, 1, 0, false, 0);
        AquariumCycle.Output out = AquariumCycle.run(in, STATS);
        assertEquals(30, out.stock(), EPSILON);
        assertEquals(30, out.died(), EPSILON);
    }

    @Test
    void noWaterKillsAll() {
        AquariumCycle.Input in = new AquariumCycle.Input(12, 0, 0, 64, 0, 0, 1, 0, false, 0);
        AquariumCycle.Output out = AquariumCycle.run(in, STATS);
        assertEquals(0, out.stock(), EPSILON);
        assertEquals(12, out.died(), EPSILON);
    }

    @Test
    void filterCleansUpToItsRateAndReportsWear() {
        AquariumCycle.Input light = new AquariumCycle.Input(24, 400_000, 0, 64, 0, 512, 1, 0, false, 0);
        AquariumCycle.Output out = AquariumCycle.run(light, STATS);
        assertEquals(15, out.filterUsed());
        assertEquals(0, out.dirt(), EPSILON);
        assertEquals(1, out.quality(), EPSILON);

        AquariumCycle.Input heavy = new AquariumCycle.Input(24, 400_000, 100, 64, 0, 512, 1, 0, false, 0);
        AquariumCycle.Output worn = AquariumCycle.run(heavy, STATS);
        assertEquals(40, worn.filterUsed());
        assertEquals(75, worn.dirt(), EPSILON);
    }

    @Test
    void filterSpendsNoMoreThanItHasLeft() {
        AquariumCycle.Input in = new AquariumCycle.Input(24, 400_000, 100, 64, 0, 10, 1, 0, false, 0);
        assertEquals(10, AquariumCycle.run(in, STATS).filterUsed());
    }

    @Test
    void noPowerStopsBreedingAndCleaningButNotFeedingOrDirt() {
        AquariumCycle.Input in = new AquariumCycle.Input(20, 400_000, 0, 64, 0, 512, 0, 0, false, 0);
        AquariumCycle.Output out = AquariumCycle.run(in, STATS);
        assertEquals(20, out.stock(), EPSILON);
        assertEquals(0, out.filterUsed());
        assertEquals(2, out.feedUsed());
        assertEquals(12, out.dirt(), EPSILON);
    }

    @Test
    void halfPowerHalvesBreedingAndCleaning() {
        AquariumCycle.Input in = new AquariumCycle.Input(20, 400_000, 100, 64, 0, 512, 0.5, 0, false, 0);
        AquariumCycle.Output out = AquariumCycle.run(in, STATS);
        assertEquals(20, out.filterUsed());
        assertTrue(out.stock() > 20 && out.stock() < 21.92);
    }

    @Test
    void catchTakesWholeFishAboveTheKeepLevel() {
        AquariumCycle.Input in = new AquariumCycle.Input(20, 400_000, 0, 64, 0, 0, 0, 8, true, 64);
        AquariumCycle.Output out = AquariumCycle.run(in, STATS);
        assertEquals(12, out.caught());
        assertEquals(8, out.stock(), EPSILON);
    }

    @Test
    void catchIsLimitedByRoom() {
        AquariumCycle.Input in = new AquariumCycle.Input(20, 400_000, 0, 64, 0, 0, 0, 8, true, 5);
        AquariumCycle.Output out = AquariumCycle.run(in, STATS);
        assertEquals(5, out.caught());
        assertEquals(15, out.stock(), EPSILON);
    }

    @Test
    void autoCatchOffKeepsEveryFish() {
        AquariumCycle.Input in = new AquariumCycle.Input(20, 400_000, 0, 64, 0, 0, 0, 8, false, 64);
        AquariumCycle.Output out = AquariumCycle.run(in, STATS);
        assertEquals(0, out.caught());
        assertEquals(20, out.stock(), EPSILON);
    }

    @Test
    void capacityFollowsTheWater() {
        assertEquals(64, AquariumCycle.capacity(256_000, STATS));
        assertEquals(0, AquariumCycle.capacity(3_999, STATS));
    }
}
