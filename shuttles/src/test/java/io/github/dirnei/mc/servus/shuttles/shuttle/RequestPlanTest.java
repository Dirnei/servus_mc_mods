package io.github.dirnei.mc.servus.shuttles.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.shuttle.RequestPlan.Job;
import io.github.dirnei.mc.servus.shuttles.shuttle.RequestPlan.Plan;
import io.github.dirnei.mc.servus.shuttles.shuttle.RequestPlan.Stock;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequestPlanTest {

    @Test
    void reachableStockGivesShuttleJobsOfAtMostAStack() {
        Plan<String> plan = RequestPlan.plan(100, 64, true, List.of(new Stock<>("rack", 200, true)));
        assertEquals(List.of(new Job<>("rack", 64), new Job<>("rack", 36)), plan.jobs());
        assertEquals(0, plan.unservable());
        assertFalse(plan.needsShuttle());
    }

    @Test
    void unreachableStockGivesNoJobAndNeedsShuttle() {
        Plan<String> plan = RequestPlan.plan(10, 64, true, List.of(new Stock<>("farRack", 50, false)));
        assertTrue(plan.jobs().isEmpty());
        assertEquals(10, plan.unservable());
        assertTrue(plan.needsShuttle());
    }

    @Test
    void noShuttleGivesNoJobAndNeedsShuttle() {
        Plan<String> plan = RequestPlan.plan(10, 64, false, List.of(new Stock<>("rack", 50, true)));
        assertTrue(plan.jobs().isEmpty());
        assertEquals(10, plan.unservable());
        assertTrue(plan.needsShuttle());
    }

    @Test
    void mixedStockPlansTheReachablePart() {
        Plan<String> plan = RequestPlan.plan(10, 64, true, List.of(
                new Stock<>("farRack", 50, false),
                new Stock<>("rack", 5, true)));
        assertEquals(List.of(new Job<>("rack", 5)), plan.jobs());
        assertEquals(5, plan.unservable());
        assertTrue(plan.needsShuttle());
    }

    @Test
    void shortStockIsReported() {
        Plan<String> plan = RequestPlan.plan(100, 64, true, List.of(new Stock<>("rack", 70, true)));
        assertEquals(70, plan.planned());
        assertEquals(30, plan.unservable());
        assertFalse(plan.needsShuttle());
    }

    @Test
    void zeroStockIsNotNeedsShuttle() {
        Plan<String> plan = RequestPlan.plan(10, 64, false, List.of());
        assertTrue(plan.jobs().isEmpty());
        assertEquals(10, plan.unservable());
        assertFalse(plan.needsShuttle());
    }

    @Test
    void zeroAmountGivesNoJobs() {
        Plan<String> plan = RequestPlan.plan(0, 64, true, List.of(new Stock<>("rack", 70, true)));
        assertTrue(plan.jobs().isEmpty());
        assertEquals(0, plan.unservable());
    }
}
