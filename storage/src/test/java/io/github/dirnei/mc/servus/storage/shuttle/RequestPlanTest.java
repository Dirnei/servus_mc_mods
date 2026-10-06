package io.github.dirnei.mc.servus.storage.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.storage.shuttle.RequestPlan.Job;
import io.github.dirnei.mc.servus.storage.shuttle.RequestPlan.Plan;
import io.github.dirnei.mc.servus.storage.shuttle.RequestPlan.Stock;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequestPlanTest {

    @Test
    void splitsIntoStacks() {
        Plan<String> plan = RequestPlan.plan(100, 64, true, List.of(new Stock<>("crate", 200, false, true)));
        assertEquals(List.of(new Job<>("crate", 64, true), new Job<>("crate", 36, true)), plan.jobs());
        assertEquals(0, plan.unservable());
        assertFalse(plan.needsShuttle());
    }

    @Test
    void instantSourcesComeBeforeShuttleOnes() {
        Plan<String> plan = RequestPlan.plan(70, 64, true, List.of(
                new Stock<>("railCrate", 64, false, true),
                new Stock<>("nearCrate", 10, false, false)));
        assertEquals(List.of(new Job<>("nearCrate", 10, false), new Job<>("railCrate", 60, true)), plan.jobs());
    }

    @Test
    void withoutShuttlesReachableCratesAreInstant() {
        Plan<String> plan = RequestPlan.plan(30, 64, false, List.of(new Stock<>("railCrate", 64, false, true)));
        assertEquals(List.of(new Job<>("railCrate", 30, false)), plan.jobs());
    }

    @Test
    void rackWithoutShuttleIsRefused() {
        Plan<String> plan = RequestPlan.plan(10, 64, false, List.of(new Stock<>("rack", 50, true, true)));
        assertTrue(plan.jobs().isEmpty());
        assertEquals(10, plan.unservable());
        assertTrue(plan.needsShuttle());
    }

    @Test
    void unreachableRackIsIgnored() {
        Plan<String> plan = RequestPlan.plan(10, 64, true, List.of(
                new Stock<>("farRack", 50, true, false),
                new Stock<>("rack", 5, true, true)));
        assertEquals(List.of(new Job<>("rack", 5, true)), plan.jobs());
        assertEquals(5, plan.unservable());
        assertTrue(plan.needsShuttle());
    }

    @Test
    void shortStockIsReported() {
        Plan<String> plan = RequestPlan.plan(100, 64, true, List.of(new Stock<>("crate", 70, false, false)));
        assertEquals(70, plan.planned());
        assertEquals(30, plan.unservable());
        assertFalse(plan.needsShuttle());
    }

    @Test
    void zeroAmountGivesNoJobs() {
        Plan<String> plan = RequestPlan.plan(0, 64, true, List.of(new Stock<>("crate", 70, false, true)));
        assertTrue(plan.jobs().isEmpty());
        assertEquals(0, plan.unservable());
    }
}
