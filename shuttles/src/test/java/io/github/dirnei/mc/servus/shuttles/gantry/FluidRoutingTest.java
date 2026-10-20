package io.github.dirnei.mc.servus.shuttles.gantry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.shuttles.gantry.FluidRouting.Candidate;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidRouting.Target;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryTanks.Tank;
import java.util.List;
import org.junit.jupiter.api.Test;

class FluidRoutingTest {

    private static final int CAP = 16000;

    private static Candidate<String, String> tank(String id, String lock, String content, int amount, int distance) {
        return new Candidate<>(id, new Tank<>(lock, content, amount, CAP), 0, null, distance);
    }

    private static List<String> ids(List<Target<String>> targets) {
        return targets.stream().map(Target::id).toList();
    }

    @Test
    void lockedBeforeAnyHoldingBeforeEmpty() {
        List<Candidate<String, String>> tanks = List.of(
                tank("empty", null, null, 0, 1),
                tank("holding", null, "water", 2000, 2),
                tank("locked", "water", "water", 8000, 3));
        assertEquals(List.of("locked", "holding", "empty"), ids(FluidRouting.targets(tanks, "water")));
    }

    @Test
    void fullTanksSkipped() {
        List<Candidate<String, String>> tanks = List.of(
                tank("full", "water", "water", CAP, 1),
                tank("empty", null, null, 0, 2));
        List<Target<String>> targets = FluidRouting.targets(tanks, "water");
        assertEquals(List.of("empty"), ids(targets));
        assertEquals(CAP, targets.get(0).room());
    }

    @Test
    void incomingCountsAgainstRoom() {
        Candidate<String, String> locked = new Candidate<>("locked", new Tank<>("water", null, 0, 4000), 4000, "water", 1);
        Candidate<String, String> part = new Candidate<>("part", new Tank<>(null, "water", 1000, 4000), 1000, "water", 2);
        Candidate<String, String> lavaOnTheWay = new Candidate<>("lava", new Tank<>(null, null, 0, 4000), 500, "lava", 3);
        List<Target<String>> targets = FluidRouting.targets(List.of(locked, part, lavaOnTheWay), "water");
        assertEquals(List.of("part"), ids(targets));
        assertEquals(2000, targets.get(0).room());
    }

    @Test
    void otherFluidAndWrongLockSkipped() {
        List<Candidate<String, String>> tanks = List.of(
                tank("lavaLock", "lava", null, 0, 1),
                tank("lavaHeld", null, "lava", 1000, 2),
                tank("mismatch", "water", "lava", 1000, 3));
        assertEquals(List.of(), FluidRouting.targets(tanks, "water"));
    }

    @Test
    void nearerWinsWithinRank() {
        List<Candidate<String, String>> tanks = List.of(
                tank("far", null, null, 0, 9),
                tank("near", null, null, 0, 2),
                tank("farLocked", "water", null, 0, 20));
        assertEquals(List.of("farLocked", "near", "far"), ids(FluidRouting.targets(tanks, "water")));
    }
}
