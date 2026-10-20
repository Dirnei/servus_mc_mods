package io.github.dirnei.mc.servus.shuttles.shuttle.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleTiers.Kind;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.build.BuildStep;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.build.StepKind;
import java.util.List;
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

    @Test
    void buildsWithoutStepsGetTheDefaults() {
        assertEquals(BuildStep.DEFAULTS, TIERS.nextSteps(1));
        assertTrue(TIERS.tier(1).steps().isEmpty(), "Mk1 has no build, so no steps");
        assertTrue(TIERS.nextSteps(3).isEmpty());
    }

    @Test
    void givenStepsAreKeptAndClamped() {
        ShuttleTiers.Tier tier = new ShuttleTiers.Tier(4, 4, 4, List.of(new ShuttleTiers.Supply("minecraft:stone", 1)),
                List.of(new BuildStep(StepKind.BOLTS, 20), new BuildStep(StepKind.WELD, 0)));
        assertEquals(List.of(new BuildStep(StepKind.BOLTS, 8), new BuildStep(StepKind.WELD, 1)), tier.steps());
    }

    @Test
    void speedUpgradesAlsoRaiseAcceleration() {
        assertEquals(0.01, TIERS.accel(1, 0), 1e-9);
        assertEquals(0.015, TIERS.accel(1, 2), 1e-9);
        assertEquals(0.015, TIERS.accel(1, 5), 1e-9);
        assertEquals(0.03, TIERS.accel(3, 8), 1e-9);
    }

    @Test
    void tankerCapacityPerTierWithDefault() {
        assertEquals(4000, TIERS.tier(1).tankerCapacity());
        assertEquals(8000, TIERS.tier(2).tankerCapacity());
        assertEquals(16000, TIERS.tier(3).tankerCapacity());
        assertEquals(ShuttleTiers.DEFAULT_TANKER_CAPACITY, new ShuttleTiers.Tier(2, 2, 2, List.of()).tankerCapacity());
    }

    @Test
    void cornerSpeedGrowsALittlePerUpgrade() {
        assertEquals(0.1, TIERS.cornerSpeed(1, 0), 1e-9);
        assertEquals(0.12, TIERS.cornerSpeed(2, 4), 1e-9);
        assertEquals(0.14, TIERS.cornerSpeed(3, 8), 1e-9);
    }

    @Test
    void cornerSpeedNeverAboveTop() {
        ShuttleTiers slow = new ShuttleTiers(0.08, 0.04, 0.01, 0.0025, 10, 4, 0.8, TIERS.tiers(), 0.1, 0.005);
        assertEquals(0.08, slow.cornerSpeed(1, 0), 1e-9);
    }

    @Test
    void cornerSpeedAtLeastOneAccelStep() {
        ShuttleTiers tight = new ShuttleTiers(0.15, 0.04, 0.05, 0, 10, 4, 0.8, TIERS.tiers(), 0.02, 0);
        assertEquals(0.05, tight.cornerSpeed(1, 0), 1e-9);
    }
}
