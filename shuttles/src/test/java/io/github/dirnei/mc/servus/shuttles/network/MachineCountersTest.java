package io.github.dirnei.mc.servus.shuttles.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MachineCountersTest {

    @Test
    void eachKindCountsFromOne() {
        MachineCounters counters = new MachineCounters();
        assertEquals(0, counters.last("field_head"));
        assertEquals(1, counters.take("field_head"));
        assertEquals(2, counters.take("field_head"));
        assertEquals(2, counters.last("field_head"));
    }

    @Test
    void kindsAreIndependent() {
        MachineCounters counters = new MachineCounters();
        counters.take("inbound_station");
        counters.take("inbound_station");
        assertEquals(1, counters.take("outbound_station"));
        assertEquals(1, counters.take("field_head"));
        assertEquals(3, counters.take("inbound_station"));
    }

    @Test
    void numbersAreNeverReused() {
        MachineCounters counters = new MachineCounters();
        counters.take("shuttle_garage");
        counters.take("shuttle_garage");
        assertEquals(3, counters.take("shuttle_garage"));
    }

    @Test
    void mapRoundTripContinuesTheCount() {
        MachineCounters counters = new MachineCounters();
        counters.take("crafting_cpu");
        counters.take("crafting_cpu");
        counters.take("elevator");
        MachineCounters loaded = new MachineCounters();
        loaded.load(counters.toMap());
        assertEquals(Map.of("crafting_cpu", 2, "elevator", 1), loaded.toMap());
        assertEquals(3, loaded.take("crafting_cpu"));
    }

    @Test
    void loadReplacesAndDropsNonPositive() {
        MachineCounters counters = new MachineCounters();
        counters.take("old");
        counters.load(Map.of("garage", 4, "broken", -2));
        assertEquals(0, counters.last("old"));
        assertEquals(0, counters.last("broken"));
        assertEquals(5, counters.take("garage"));
    }
}
