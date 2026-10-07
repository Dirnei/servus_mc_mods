package io.github.dirnei.mc.servus.lib.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class FluidRoutingTest {

    /** A one fluid tank that can act as source and sink, moving fluid in steps of {@code step}. */
    static final class Tank implements FluidRouting.Source<String>, FluidRouting.Sink<String> {
        String fluid;
        int amount;
        final int capacity;
        final int step;

        Tank(String fluid, int amount, int capacity) {
            this(fluid, amount, capacity, 1);
        }

        Tank(String fluid, int amount, int capacity, int step) {
            this.fluid = fluid;
            this.amount = amount;
            this.capacity = capacity;
            this.step = step;
        }

        @Override
        public FluidRouting.Offer<String> offer() {
            return amount > 0 ? new FluidRouting.Offer<>(fluid, amount) : null;
        }

        @Override
        public int drain(String key, int max) {
            if (!key.equals(fluid)) {
                return 0;
            }
            int taken = Math.min(max, amount) / step * step;
            amount -= taken;
            if (amount == 0) {
                fluid = null;
            }
            return taken;
        }

        @Override
        public int fill(String key, int max, boolean simulate) {
            if (fluid != null && !fluid.equals(key)) {
                return 0;
            }
            int accepted = Math.min(max, capacity - amount) / step * step;
            if (!simulate && accepted > 0) {
                amount += accepted;
                fluid = key;
            }
            return accepted;
        }
    }

    @Test
    void oneSourceSharesEvenlyBetweenTwoSinks() {
        Tank source = new Tank("water", 100, 1000);
        Tank a = new Tank(null, 0, 1000);
        Tank b = new Tank(null, 0, 1000);

        int moved = FluidRouting.route(List.of(source), List.of(a, b));

        assertEquals(100, moved);
        assertEquals(50, a.amount);
        assertEquals(50, b.amount);
        assertEquals(0, source.amount);
    }

    @Test
    void wholeContentMovesWithoutLimit() {
        Tank source = new Tank("water", 64_000, 64_000);
        Tank sink = new Tank(null, 0, 64_000);

        assertEquals(64_000, FluidRouting.route(List.of(source), List.of(sink)));
        assertEquals(64_000, sink.amount);
        assertEquals(0, source.amount);
    }

    @Test
    void shortSupplyLeftoverGoesToTheHungrySink() {
        Tank source = new Tank("water", 100, 1000);
        Tank almostFull = new Tank("water", 980, 1000);
        Tank empty = new Tank(null, 0, 1000);

        FluidRouting.route(List.of(source), List.of(almostFull, empty));

        assertEquals(1000, almostFull.amount);
        assertEquals(80, empty.amount);
    }

    @Test
    void sinkLimitsWhatMoves() {
        Tank source = new Tank("water", 1000, 1000);
        Tank sink = new Tank("water", 700, 1000);

        assertEquals(300, FluidRouting.route(List.of(source), List.of(sink)));
        assertEquals(700, source.amount);
    }

    @Test
    void mixedKeysReachOnlyMatchingSinks() {
        Tank water = new Tank("water", 1000, 1000);
        Tank lava = new Tank("lava", 1000, 1000);
        Tank waterSink = new Tank("water", 500, 4000);
        Tank lavaSink = new Tank("lava", 100, 4000);

        FluidRouting.route(List.of(lava, water), List.of(waterSink, lavaSink));

        assertEquals("water", waterSink.fluid);
        assertEquals(1500, waterSink.amount);
        assertEquals("lava", lavaSink.fluid);
        assertEquals(1100, lavaSink.amount);
    }

    @Test
    void emptySinkTakesTheFirstOfferedKey() {
        Tank water = new Tank("water", 1000, 1000);
        Tank lava = new Tank("lava", 1000, 1000);
        Tank empty = new Tank(null, 0, 4000);
        Tank waterSink = new Tank("water", 0, 4000);

        FluidRouting.route(List.of(water, lava), List.of(empty, waterSink));

        assertEquals("water", empty.fluid);
        assertEquals("water", waterSink.fluid);
        assertEquals(1000, empty.amount + waterSink.amount);
        assertEquals(1000, lava.amount);

        FluidRouting.route(List.of(lava), List.of(empty));
        assertEquals("water", empty.fluid);
        assertEquals(1000, lava.amount);
    }

    @Test
    void stepSinkGetsTheWholeStepInsteadOfAShare() {
        Tank source = new Tank("water", 1000, 1000, 1000);
        Tank a = new Tank(null, 0, 1000, 1000);
        Tank b = new Tank(null, 0, 1000, 1000);

        assertEquals(1000, FluidRouting.route(List.of(source), List.of(a, b)));
        assertEquals(1000, a.amount);
        assertEquals(0, b.amount);
        assertNull(source.fluid);
    }

    @Test
    void noSupplyMovesNothing() {
        Tank empty = new Tank(null, 0, 1000);
        Tank sink = new Tank(null, 0, 1000);

        assertEquals(0, FluidRouting.route(List.of(empty), List.of(sink)));
        assertEquals(0, FluidRouting.route(List.of(), List.of(sink)));
        assertEquals(0, FluidRouting.route(List.of(new Tank("water", 10, 10)), List.of()));
        assertEquals(0, sink.amount);
    }
}
