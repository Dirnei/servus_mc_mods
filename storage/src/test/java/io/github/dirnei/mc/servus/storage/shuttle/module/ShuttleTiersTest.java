package io.github.dirnei.mc.servus.storage.shuttle.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleTiers.Kind;
import org.junit.jupiter.api.Test;

class ShuttleTiersTest {

    private static final ShuttleTiers TIERS = ShuttleTiers.DEFAULT;

    @Test
    void speedGrowsPerCount() {
        assertEquals(0.15, TIERS.speed(1, 0), 1e-9);
        assertEquals(0.23, TIERS.speed(1, 2), 1e-9);
        assertEquals(0.31, TIERS.speed(2, 4), 1e-9);
    }

    @Test
    void countsClampToTheTierMax() {
        assertEquals(TIERS.speed(1, 2), TIERS.speed(1, 5), 1e-9, "Mk1 takes two Speed upgrades at most");
        assertEquals(2, TIERS.clamp(1, Kind.HANDLING, 9));
        assertEquals(0, TIERS.clamp(1, Kind.SPEED, -3));
    }

    @Test
    void handlingFactorPerCount() {
        assertEquals(1.0, TIERS.factor(1, 0), 1e-9);
        assertEquals(0.64, TIERS.factor(1, 2), 1e-9);
    }

    @Test
    void stopTicksWithTheExampleNumbers() {
        assertEquals(18, TIERS.stopTicks(1.0, 2));
        assertEquals(12, TIERS.stopTicks(TIERS.factor(1, 2), 2));
        assertEquals(10, TIERS.stopTicks(1.0, 0));
    }

    @Test
    void workTicksShortenWithHandling() {
        assertEquals(6, ShuttleTiers.workTicks(10, TIERS.factor(1, 2)));
        assertEquals(10, ShuttleTiers.workTicks(10, 1.0));
    }

    @Test
    void neverBelowOneTick() {
        ShuttleTiers quick = new ShuttleTiers(0.15, 0.04, 0, 0, 0.5, TIERS.tiers());
        assertEquals(1, quick.stopTicks(0.01, 0));
        assertEquals(1, ShuttleTiers.workTicks(1, 0.01));
    }

    @Test
    void roomPerKind() {
        assertEquals(2, TIERS.room(1, Kind.SPEED, 0));
        assertEquals(0, TIERS.room(1, Kind.SPEED, 2));
        assertEquals(3, TIERS.room(2, Kind.HANDLING, 1));
        assertEquals(0, TIERS.room(1, Kind.HANDLING, 5), "never negative");
    }

    @Test
    void tierNumbersClamp() {
        assertEquals(2, TIERS.tier(0).stacks());
        assertEquals(8, TIERS.tier(9).stacks());
        assertEquals(3, TIERS.topTier());
    }

    @Test
    void nextBuildIsTheSheetOfTheTierAbove() {
        assertEquals(TIERS.tier(2).build(), TIERS.nextBuild(1));
        assertTrue(TIERS.nextBuild(3).isEmpty());
    }
}
