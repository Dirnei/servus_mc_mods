package io.github.dirnei.mc.servus.shuttles.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.lock.SlotFill.Part;
import io.github.dirnei.mc.servus.shuttles.lock.SlotFill.Slot;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlotFillTest {

    private static Slot<String> empty() {
        return new Slot<>(null, 0, 64, false);
    }

    private static Slot<String> holding(String key, int count) {
        return new Slot<>(key, count, 64, false);
    }

    private static Slot<String> locked(String key, int count) {
        return new Slot<>(key, count, 64, true);
    }

    @Test
    void topsUpPartialStacksBeforeEmptySlots() {
        List<Slot<String>> slots = List.of(empty(), holding("stone", 20), holding("dirt", 10), holding("stone", 64));
        assertEquals(List.of(new Part(1, 44), new Part(0, 20)), SlotFill.in(slots, "stone", 64, 64));
    }

    @Test
    void lockedSlotsTakeNothing() {
        List<Slot<String>> slots = List.of(locked("stone", 20), locked(null, 0), empty());
        assertEquals(List.of(new Part(2, 64)), SlotFill.in(slots, "stone", 100, 64));
        assertEquals(64, SlotFill.room(slots, "stone", 64));
    }

    @Test
    void emptyOnlyIgnoresPartials() {
        List<Slot<String>> slots = List.of(holding("stone", 20), empty(), empty());
        assertEquals(List.of(new Part(1, 36)), SlotFill.inEmpty(slots, 36, 64));
        assertEquals(List.of(new Part(1, 64), new Part(2, 6)), SlotFill.inEmpty(slots, 70, 64));
    }

    @Test
    void stackSizeLimitsEachSlot() {
        List<Slot<String>> slots = List.of(holding("pearl", 10), empty());
        assertEquals(List.of(new Part(0, 6), new Part(1, 16)), SlotFill.in(slots, "pearl", 30, 16));
        assertEquals(22, SlotFill.room(slots, "pearl", 16));
    }

    @Test
    void outTakesSmallestStacksFirstAndStopsAtTheAmount() {
        List<Slot<String>> slots = List.of(holding("stone", 64), holding("stone", 10), locked("stone", 5), holding("dirt", 3), holding("stone", 10));
        assertEquals(List.of(new Part(1, 10), new Part(4, 10), new Part(0, 30)), SlotFill.out(slots, "stone", 50));
        assertEquals(84, SlotFill.available(slots, "stone"));
    }

    @Test
    void countsFreeSlotsAndPartials() {
        List<Slot<String>> slots = List.of(empty(), locked(null, 0), holding("stone", 64), locked("stone", 3));
        assertEquals(1, SlotFill.freeSlots(slots));
        assertFalse(SlotFill.partial(slots, "stone", 64));
        assertTrue(SlotFill.holds(slots, "stone"));
        assertTrue(SlotFill.partial(List.of(holding("stone", 3)), "stone", 64));
    }

    @Test
    void severalJobsShareASlotAndInLockedSlotsGiveNothing() {
        List<Slot<String>> slots = List.of(new Slot<>("stone", 64, 64, true, 28), new Slot<>("stone", 10, 64, true, 0));
        assertEquals(List.of(new Part(0, 36)), SlotFill.out(slots, "stone", 50));
        assertEquals(36, SlotFill.available(slots, "stone"));
        assertEquals(0, SlotFill.room(slots, "stone", 64));
    }
}
