package io.github.dirnei.mc.servus.shuttles.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.shuttles.fluid.FluidRouting.Candidate;
import io.github.dirnei.mc.servus.shuttles.fluid.FluidRouting.Target;
import java.util.List;
import org.junit.jupiter.api.Test;

class FluidRoutingTest {

    private static final int CAP = 16000;
    private static final String WATER = "water";

    /** A store set to one fluid or to none: preferred when set to the fluid, allowed when set to it or to none. */
    private static Candidate<String> tank(String id, String lock, String content, int amount, int distance) {
        return Candidate.of(id, WATER, WATER.equals(lock), lock == null || lock.equals(WATER), content, amount, CAP, 0, null, distance);
    }

    /** A rack shelf: preferred when its zone lists the fluid, allowed when it lists it or lists no fluids. */
    private static Candidate<String> rack(String id, List<String> zoneFluids, String content, int amount, int capacity, int distance) {
        return Candidate.of(id, WATER, zoneFluids.contains(WATER), zoneFluids.isEmpty() || zoneFluids.contains(WATER), content, amount,
                capacity, 0, null, distance);
    }

    private static List<String> ids(List<Target<String>> targets) {
        return targets.stream().map(Target::id).toList();
    }

    @Test
    void lockedBeforeAnyHoldingBeforeEmpty() {
        List<Candidate<String>> tanks = List.of(
                tank("empty", null, null, 0, 1),
                tank("holding", null, WATER, 2000, 2),
                tank("locked", WATER, WATER, 8000, 3));
        assertEquals(List.of("locked", "holding", "empty"), ids(FluidRouting.targets(tanks)));
    }

    @Test
    void fullTanksSkipped() {
        List<Candidate<String>> tanks = List.of(
                tank("full", WATER, WATER, CAP, 1),
                tank("empty", null, null, 0, 2));
        List<Target<String>> targets = FluidRouting.targets(tanks);
        assertEquals(List.of("empty"), ids(targets));
        assertEquals(CAP, targets.get(0).room());
    }

    @Test
    void incomingCountsAgainstRoom() {
        Candidate<String> locked = Candidate.of("locked", WATER, true, true, null, 0, 4000, 4000, WATER, 1);
        Candidate<String> part = Candidate.of("part", WATER, false, true, WATER, 1000, 4000, 1000, WATER, 2);
        Candidate<String> lavaOnTheWay = Candidate.of("lava", WATER, false, true, null, 0, 4000, 500, "lava", 3);
        List<Target<String>> targets = FluidRouting.targets(List.of(locked, part, lavaOnTheWay));
        assertEquals(List.of("part"), ids(targets));
        assertEquals(2000, targets.get(0).room());
    }

    @Test
    void otherFluidAndWrongLockSkipped() {
        List<Candidate<String>> tanks = List.of(
                tank("lavaLock", "lava", null, 0, 1),
                tank("lavaHeld", null, "lava", 1000, 2),
                tank("mismatch", WATER, "lava", 1000, 3));
        assertEquals(List.of(), FluidRouting.targets(tanks));
    }

    @Test
    void nearerWinsWithinRank() {
        List<Candidate<String>> tanks = List.of(
                tank("far", null, null, 0, 9),
                tank("near", null, null, 0, 2),
                tank("farLocked", WATER, null, 0, 20));
        assertEquals(List.of("farLocked", "near", "far"), ids(FluidRouting.targets(tanks)));
    }

    @Test
    void preferredRackBeatsEmptyTankOnAny() {
        List<Candidate<String>> stores = List.of(
                tank("emptyTank", null, null, 0, 1),
                rack("zonedRack", List.of(WATER), null, 0, 48000, 9));
        assertEquals(List.of("zonedRack", "emptyTank"), ids(FluidRouting.targets(stores)));
    }

    @Test
    void preferredTankAndRackSameRankShorterWayWins() {
        List<Candidate<String>> stores = List.of(
                tank("lockedTank", WATER, null, 0, 7),
                rack("zonedRack", List.of(WATER), null, 0, 48000, 3));
        assertEquals(List.of("zonedRack", "lockedTank"), ids(FluidRouting.targets(stores)));
    }

    @Test
    void nonAcceptingRackSkipped() {
        List<Candidate<String>> stores = List.of(
                rack("lavaZone", List.of("lava"), null, 0, 48000, 1),
                rack("holdsLava", List.of(), "lava", 1000, 48000, 2),
                rack("general", List.of(), null, 0, 16000, 3));
        assertEquals(List.of("general"), ids(FluidRouting.targets(stores)));
    }
}
