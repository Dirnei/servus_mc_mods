package io.github.dirnei.mc.servus.shuttles.cpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CpuBufferTest {

    private static CpuBuffer<String> filled(int capacity, long order, String key, int count, int maxStack) {
        CpuBuffer<String> buffer = new CpuBuffer<>(capacity);
        put(buffer, 1000 + order, order, key, count, maxStack);
        return buffer;
    }

    private static void put(CpuBuffer<String> buffer, long transfer, long order, String key, int count, int maxStack) {
        buffer.expect(transfer, order, key, count, maxStack);
        buffer.land(key, count);
        buffer.arrived(transfer);
    }

    @Test
    void sixtyFourPlanksUseOneStack() {
        CpuBuffer<String> buffer = filled(2, 1, "planks", 64, 64);
        assertEquals(1, buffer.used());
        assertEquals(64, buffer.held(1, "planks"));
    }

    @Test
    void sixtyFivePlanksUseTwoStacks() {
        assertEquals(2, filled(2, 1, "planks", 65, 64).used());
    }

    @Test
    void sixteenPearlsUseOneStack() {
        CpuBuffer<String> buffer = filled(2, 1, "pearl", 16, 16);
        assertEquals(1, buffer.used());
    }

    @Test
    void ordersDoNotShareAStack() {
        CpuBuffer<String> buffer = new CpuBuffer<>(4);
        put(buffer, 10, 1, "planks", 4, 64);
        put(buffer, 11, 2, "planks", 4, 64);
        assertEquals(2, buffer.used());
    }

    @Test
    void roomFillsTheLastStackThenWholeStacks() {
        CpuBuffer<String> buffer = filled(3, 1, "planks", 70, 64);
        assertEquals(2, buffer.used());
        assertEquals(58 + 64, buffer.room(1, "planks", 64));
        assertEquals(64, buffer.room(2, "planks", 64));
        assertEquals(16, buffer.room(2, "pearl", 16));
    }

    @Test
    void anArrivalHoldsItsRoom() {
        CpuBuffer<String> buffer = new CpuBuffer<>(1);
        buffer.expect(5, 1, "planks", 64, 64);
        assertEquals(1, buffer.used());
        assertEquals(0, buffer.room(2, "planks", 64));
        assertEquals(64, buffer.expected("planks"));
        assertEquals(0, buffer.held(1, "planks"));
        assertEquals(64, buffer.incoming(1, "planks"));
    }

    @Test
    void landingCreditsOldestArrivalFirst() {
        CpuBuffer<String> buffer = new CpuBuffer<>(2);
        buffer.expect(5, 1, "planks", 4, 64);
        buffer.expect(6, 2, "planks", 4, 64);
        assertEquals(6, buffer.land("planks", 6));
        assertEquals(4, buffer.held(1, "planks"));
        assertEquals(2, buffer.held(2, "planks"));
        assertEquals(2, buffer.expected("planks"));
        assertEquals(2, buffer.land("planks", 5));
        assertEquals(0, buffer.expected("planks"));
        assertEquals(0, buffer.land("stick", 1));
    }

    @Test
    void anArrivalThatEndsShortReleasesTheRest() {
        CpuBuffer<String> buffer = new CpuBuffer<>(2);
        buffer.expect(5, 1, "planks", 100, 64);
        assertEquals(2, buffer.used());
        buffer.land("planks", 10);
        buffer.arrived(5);
        assertFalse(buffer.expects(5));
        assertEquals(10, buffer.held(1, "planks"));
        assertEquals(1, buffer.used());
        assertEquals(0, buffer.expected("planks"));
    }

    @Test
    void anArrivalWithNothingLandedLeavesNoEntry() {
        CpuBuffer<String> buffer = new CpuBuffer<>(1);
        buffer.expect(5, 1, "planks", 4, 64);
        buffer.arrived(5);
        assertTrue(buffer.isEmpty());
        assertEquals(0, buffer.used());
    }

    @Test
    void takeLowersHeldAndFreesAStack() {
        CpuBuffer<String> buffer = filled(2, 1, "planks", 65, 64);
        assertEquals(1, buffer.take(1, "planks", 1));
        assertEquals(64, buffer.held(1, "planks"));
        assertEquals(1, buffer.used());
        assertEquals(64, buffer.take(1, "planks", 100));
        assertEquals(0, buffer.used());
        assertTrue(buffer.entries().isEmpty());
    }

    @Test
    void zeroRoomTakesNothing() {
        CpuBuffer<String> buffer = new CpuBuffer<>(0);
        assertEquals(0, buffer.room(1, "planks", 64));
        CpuBuffer<String> full = filled(1, 1, "planks", 64, 64);
        assertEquals(0, full.room(1, "planks", 64));
        assertEquals(0, full.room(2, "stick", 64));
    }

    @Test
    void leftoversKeepTheirRoomUntilTaken() {
        CpuBuffer<String> buffer = filled(1, 1, "planks", 4, 64);
        buffer.leave(1);
        assertEquals(1, buffer.leftovers().size());
        assertTrue(buffer.leftovers().get(0).leaving());
        assertEquals(1, buffer.used());
        assertEquals(0, buffer.room(2, "planks", 64));
        buffer.take(1, "planks", 4);
        assertTrue(buffer.leftovers().isEmpty());
        assertEquals(64, buffer.room(2, "planks", 64));
    }

    @Test
    void nothingGoesNegative() {
        CpuBuffer<String> buffer = filled(1, 1, "planks", 4, 64);
        assertEquals(0, buffer.take(1, "planks", -3));
        assertEquals(0, buffer.take(2, "planks", 3));
        assertEquals(4, buffer.take(1, "planks", 10));
        assertEquals(0, buffer.take(1, "planks", 10));
        assertEquals(0, buffer.held(1, "planks"));
        assertEquals(0, buffer.land("planks", -2));
        buffer.arrived(99);
        assertEquals(0, buffer.used());
        buffer.setCapacity(-4);
        assertEquals(0, buffer.capacity());
        assertEquals(0, buffer.room(1, "planks", 64));
    }

    @Test
    void restoreRoundTrip() {
        CpuBuffer<String> buffer = new CpuBuffer<>(3);
        put(buffer, 1, 1, "planks", 4, 64);
        buffer.expect(2, 1, "planks", 8, 64);
        buffer.land("planks", 3);
        buffer.leave(1);
        CpuBuffer<String> copy = new CpuBuffer<>(3);
        buffer.entries().forEach(copy::restore);
        buffer.arrivals().forEach(copy::restore);
        assertEquals(buffer.entries(), copy.entries());
        assertEquals(buffer.arrivals(), copy.arrivals());
        assertEquals(5, copy.expected("planks"));
        assertEquals(buffer.used(), copy.used());
    }
}
