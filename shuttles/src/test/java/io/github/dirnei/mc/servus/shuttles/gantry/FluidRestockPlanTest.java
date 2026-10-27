package io.github.dirnei.mc.servus.shuttles.gantry;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void belowMinLoadWaitsUnlessItFills() {
        assertEquals(500, FluidRestockPlan.amount(500, 1000, 4000, 20000));
        assertEquals(0, FluidRestockPlan.amount(6000, 1000, 4000, 500));
        assertEquals(1000, FluidRestockPlan.amount(6000, 1000, 4000, 1000));
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
                new FluidRestockPlan.Source<>("gantryTank", 6000, 1),
                new FluidRestockPlan.Source<>("rackSection", 20000, 12)));
        assertEquals(List.of("rackSection", "gantryTank"), sources.stream().map(FluidRestockPlan.Source::id).toList());
    }

    @Test
    void nearerWinsOnTie() {
        List<FluidRestockPlan.Source<String>> sources = FluidRestockPlan.sources(List.of(
                new FluidRestockPlan.Source<>("far", 5000, 7),
                new FluidRestockPlan.Source<>("near", 5000, 2)));
        assertEquals(List.of("near", "far"), sources.stream().map(FluidRestockPlan.Source::id).toList());
    }
}
