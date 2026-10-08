package io.github.dirnei.mc.servus.foundation.logistics.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.logistics.routing.RoundRobinPlan.Offer;
import io.github.dirnei.mc.servus.foundation.logistics.routing.RoundRobinPlan.Target;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoundRobinPlanTest {

    private static RoundRobinPlan<String> plan(Target<String>... targets) {
        RoundRobinPlan<String> plan = new RoundRobinPlan<>();
        plan.rebuild(List.of(targets));
        return plan;
    }

    private static List<String> targets(List<Offer<String>> offers) {
        return offers.stream().map(Offer::target).toList();
    }

    @Test
    void roundRobinWithinAPriority() {
        RoundRobinPlan<String> plan = plan(new Target<>(0, 0, "a"), new Target<>(0, 0, "b"), new Target<>(0, 0, "c"));
        assertEquals(List.of("a"), targets(plan.plan(0, 1, t -> 64)));
        plan.delivered(0, "a");
        assertEquals(List.of("b"), targets(plan.plan(0, 1, t -> 64)));
        plan.delivered(0, "b");
        assertEquals(List.of("c"), targets(plan.plan(0, 1, t -> 64)));
        plan.delivered(0, "c");
        assertEquals(List.of("a"), targets(plan.plan(0, 1, t -> 64)));
    }

    @Test
    void higherPriorityFirst() {
        RoundRobinPlan<String> plan = plan(new Target<>(0, 0, "low"), new Target<>(0, 5, "high"));
        List<Offer<String>> offers = plan.plan(0, 10, t -> 64);
        assertEquals(List.of(new Offer<>("high", 10)), offers);
    }

    @Test
    void overflowToLowerPriority() {
        RoundRobinPlan<String> plan = plan(new Target<>(0, 5, "high"), new Target<>(0, 0, "low"));
        Map<String, Integer> capacity = Map.of("high", 4, "low", 64);
        List<Offer<String>> offers = plan.plan(0, 10, capacity::get);
        assertEquals(List.of(new Offer<>("high", 4), new Offer<>("low", 6)), offers);
    }

    @Test
    void fullTargetsAreSkipped() {
        RoundRobinPlan<String> plan = plan(new Target<>(0, 0, "a"), new Target<>(0, 0, "b"));
        Map<String, Integer> capacity = Map.of("a", 0, "b", 5);
        assertEquals(List.of(new Offer<>("b", 5)), plan.plan(0, 9, capacity::get));
    }

    @Test
    void cursorAdvancesOnlyOnDelivery() {
        RoundRobinPlan<String> plan = plan(new Target<>(0, 0, "a"), new Target<>(0, 0, "b"));
        assertEquals(List.of("a"), targets(plan.plan(0, 1, t -> 64)));
        assertEquals(List.of("a"), targets(plan.plan(0, 1, t -> 64)));
        plan.delivered(0, "a");
        assertEquals(List.of("b"), targets(plan.plan(0, 1, t -> 64)));
    }

    @Test
    void separateCursorsPerChannel() {
        RoundRobinPlan<String> plan = plan(new Target<>(1, 0, "a"), new Target<>(1, 0, "b"),
                new Target<>(2, 0, "c"), new Target<>(2, 0, "d"));
        plan.delivered(1, "a");
        assertEquals(List.of("b"), targets(plan.plan(1, 1, t -> 64)));
        assertEquals(List.of("c"), targets(plan.plan(2, 1, t -> 64)));
        assertTrue(plan.plan(3, 1, t -> 64).isEmpty(), "a channel without targets plans nothing");
        assertFalse(plan.hasTargets(3));
        assertTrue(plan.hasTargets(1));
    }

    @Test
    void cursorsSurviveRebuild() {
        RoundRobinPlan<String> plan = plan(new Target<>(0, 0, "a"), new Target<>(0, 0, "b"));
        plan.delivered(0, "a");
        plan.rebuild(List.of(new Target<>(0, 0, "a"), new Target<>(0, 0, "b")));
        assertEquals(List.of("b"), targets(plan.plan(0, 1, t -> 64)));
    }
}
