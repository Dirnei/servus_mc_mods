package io.github.dirnei.mc.servus.shuttles.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class HeadUnloadTest {

    private static List<HeadUnload.Slot<String>> empty(int count) {
        return Collections.nCopies(count, HeadUnload.Slot.empty());
    }

    @Test
    void wheatFillsEmptySlots() {
        HeadUnload.Plan<String> plan = HeadUnload.plan(empty(3), List.of(new HeadUnload.Stack<>("wheat", 100, 64)));
        assertEquals(new HeadUnload.Slot<>("wheat", 64, 64, false), plan.slots().get(0));
        assertEquals(new HeadUnload.Slot<>("wheat", 36, 64, false), plan.slots().get(1));
        assertEquals(HeadUnload.Slot.empty(), plan.slots().get(2));
        assertTrue(plan.aboard().isEmpty());
    }

    @Test
    void sameItemSlotWithRoomFirst() {
        List<HeadUnload.Slot<String>> slots = List.of(HeadUnload.Slot.empty(), new HeadUnload.Slot<>("wheat", 60, 64, false));
        HeadUnload.Plan<String> plan = HeadUnload.plan(slots, List.of(new HeadUnload.Stack<>("wheat", 10, 64)));
        assertEquals(new HeadUnload.Slot<>("wheat", 64, 64, false), plan.slots().get(1));
        assertEquals(new HeadUnload.Slot<>("wheat", 6, 64, false), plan.slots().get(0));
    }

    @Test
    void fullBufferLeavesTheRestAboard() {
        List<HeadUnload.Slot<String>> slots = List.of(HeadUnload.Slot.empty());
        HeadUnload.Plan<String> plan = HeadUnload.plan(slots,
                List.of(new HeadUnload.Stack<>("wheat", 64, 64), new HeadUnload.Stack<>("seeds", 20, 64)));
        assertEquals(new HeadUnload.Slot<>("wheat", 64, 64, false), plan.slots().get(0));
        assertEquals(List.of(new HeadUnload.Stack<>("seeds", 20, 64)), plan.aboard());
    }

    @Test
    void lockedSlotsTakeNothing() {
        List<HeadUnload.Slot<String>> slots = List.of(new HeadUnload.Slot<>("wheat", 10, 64, true), new HeadUnload.Slot<>(null, 0, 0, true));
        HeadUnload.Plan<String> plan = HeadUnload.plan(slots, List.of(new HeadUnload.Stack<>("wheat", 5, 64)));
        assertEquals(slots, plan.slots());
        assertEquals(List.of(new HeadUnload.Stack<>("wheat", 5, 64)), plan.aboard());
        assertFalse(HeadUnload.hasFreeSlot(slots));
    }

    @Test
    void freeSlot() {
        assertTrue(HeadUnload.hasFreeSlot(empty(1)));
        assertFalse(HeadUnload.hasFreeSlot(List.of(new HeadUnload.Slot<>("wheat", 1, 64, false))));
    }
}
