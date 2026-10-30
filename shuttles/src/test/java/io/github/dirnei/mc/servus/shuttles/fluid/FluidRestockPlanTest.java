package io.github.dirnei.mc.servus.shuttles.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class FluidRestockPlanTest {

    @Test
    void missingCountsIncoming() {
        assertEquals(6000, FluidRestockPlan.missing(8000, 2000, 0));
        assertEquals(2000, FluidRestockPlan.missing(8000, 2000, 4000));
    }

    @Test
    void noMissingNoRun() {
        assertEquals(0, FluidRestockPlan.missing(8000, 6000, 4000));
        assertEquals(0, FluidRestockPlan.missing(0, 0, 0));
        assertEquals(0, FluidRestockPlan.amount(0, 1000, 4000, 20000));
    }

    @Test
    void runsCarryWholeBuckets() {
        assertEquals(1000, FluidRestockPlan.amount(1750, 1000, 4000, 20000));
        assertEquals(3000, FluidRestockPlan.amount(6000, 1000, 3500, 20000));
        assertEquals(2000, FluidRestockPlan.amount(6000, 1000, 4000, 2999));
    }

    @Test
    void lessThanABucketIsNoRun() {
        assertEquals(0, FluidRestockPlan.amount(750, 0, 4000, 20000));
        assertEquals(0, FluidRestockPlan.amount(6000, 0, 4000, 600));
        assertEquals(0, FluidRestockPlan.amount(6000, 0, 900, 20000));
    }

    @Test
    void minLoadWaitsUnlessAllWholeBucketsCome() {
        assertEquals(2000, FluidRestockPlan.amount(2500, 4000, 8000, 20000));
        assertEquals(0, FluidRestockPlan.amount(6000, 4000, 8000, 2000));
        assertEquals(4000, FluidRestockPlan.amount(6000, 4000, 8000, 4000));
    }

    @Test
    void restockStartsAtTheLevel() {
        assertFalse(FluidRestockPlan.restocking(false, 5000, 4000, 8000));
        assertTrue(FluidRestockPlan.restocking(false, 4000, 4000, 8000));
        assertTrue(FluidRestockPlan.restocking(false, 0, 0, 8000));
        assertTrue(FluidRestockPlan.restocking(false, 7000, 8000, 8000));
        assertFalse(FluidRestockPlan.restocking(true, 0, 0, 0));
    }

    @Test
    void restockGoesOnUntilLessThanABucketIsMissing() {
        assertTrue(FluidRestockPlan.restocking(true, 10000, 2000, 16000));
        assertTrue(FluidRestockPlan.restocking(true, 15000, 2000, 16000));
        assertFalse(FluidRestockPlan.restocking(true, 15001, 2000, 16000));
        assertFalse(FluidRestockPlan.restocking(false, 10000, 2000, 16000));
    }

    @Test
    void limitedByShuttleAndSource() {
        assertEquals(4000, FluidRestockPlan.amount(6000, 1000, 4000, 20000));
        assertEquals(3000, FluidRestockPlan.amount(6000, 1000, 4000, 3000));
        assertEquals(0, FluidRestockPlan.amount(6000, 1000, 0, 3000));
    }

    @Test
    void sourceWithMostFreeFirst() {
        List<FluidRestockPlan.Source<String>> sources = FluidRestockPlan.sources(List.of(
                new FluidRestockPlan.Source<>("a", 2000, 1),
                new FluidRestockPlan.Source<>("b", 9000, 9),
                new FluidRestockPlan.Source<>("c", 0, 0)));
        assertEquals(List.of("b", "a"), sources.stream().map(FluidRestockPlan.Source::id).toList());
    }

    @Test
    void rackSourcePickedByMostFreeFluid() {
        List<FluidRestockPlan.Source<String>> sources = FluidRestockPlan.sources(List.of(
                new FluidRestockPlan.Source<>("nearShelf", 6000, 1),
                new FluidRestockPlan.Source<>("rackShelf", 20000, 12)));
        assertEquals(List.of("rackShelf", "nearShelf"), sources.stream().map(FluidRestockPlan.Source::id).toList());
    }

    @Test
    void nearerWinsOnTie() {
        List<FluidRestockPlan.Source<String>> sources = FluidRestockPlan.sources(List.of(
                new FluidRestockPlan.Source<>("far", 5000, 7),
                new FluidRestockPlan.Source<>("near", 5000, 2)));
        assertEquals(List.of("near", "far"), sources.stream().map(FluidRestockPlan.Source::id).toList());
    }
}
