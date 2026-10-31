package io.github.dirnei.mc.servus.shuttles.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.lock.SlotFill.Part;
import io.github.dirnei.mc.servus.shuttles.lock.SlotFill.Reservation;
import io.github.dirnei.mc.servus.shuttles.lock.SlotFill.Slot;
import java.util.ArrayList;
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

    @Test
    void countedSlotsLeaveOutWhatOtherJobsReserved() {
        assertEquals(20, SlotFill.sharedRoom(64, 20, 24));
        assertEquals(0, SlotFill.sharedRoom(64, 20, 50));
        assertEquals(Integer.MAX_VALUE - 5, SlotFill.sharedRoom(Integer.MAX_VALUE, 5, 0));
        assertEquals(10, SlotFill.sharedAvailable(30, 20));
        assertEquals(0, SlotFill.sharedAvailable(10, 20));
        assertEquals(10, SlotFill.sharedAvailable(10, 0));
    }

    @Test
    void inUseCountsHoldingAndLockedSlots() {
        List<Slot<String>> slots = List.of(empty(), holding("stone", 3), locked(null, 0), locked("dirt", 5), empty());
        assertEquals(3, SlotFill.inUse(slots));
        assertEquals(0, SlotFill.inUse(List.of(empty(), empty())));
    }

    @Test
    void unreservedIsWhatNoOutLockTakes() {
        assertEquals(20, SlotFill.unreserved(20, 0));
        assertEquals(12, SlotFill.unreserved(20, 8));
        assertEquals(0, SlotFill.unreserved(20, 20));
        assertEquals(0, SlotFill.unreserved(20, 30));
    }

    /** Counted slots with their IN reservations, planned the way incoming picks see them. */
    private static final class Incoming {
        private final List<String> keys = new ArrayList<>();
        private final List<Integer> counts = new ArrayList<>();
        private final List<Integer> limits = new ArrayList<>();
        private final List<List<Reservation<String>>> reservations = new ArrayList<>();

        @SafeVarargs
        private Incoming slot(String key, int count, int limit, Reservation<String>... reserved) {
            keys.add(key);
            counts.add(count);
            limits.add(limit);
            reservations.add(new ArrayList<>(List.of(reserved)));
            return this;
        }

        Incoming empties(int number) {
            for (int i = 0; i < number; i++) {
                slot(null, 0, 64);
            }
            return this;
        }

        List<Slot<String>> views() {
            List<Slot<String>> views = new ArrayList<>();
            for (int i = 0; i < keys.size(); i++) {
                views.add(SlotFill.incoming(keys.get(i), counts.get(i), limits.get(i), reservations.get(i)));
            }
            return views;
        }

        /** Plans the pick and reserves what the plan found. */
        List<Part> pick(String key, int amount) {
            List<Part> parts = SlotFill.in(views(), key, amount, 64);
            for (Part part : parts) {
                reservations.get(part.slot()).add(new Reservation<>(key, part.count()));
            }
            return parts;
        }

        int reservedAt(int slot) {
            return reservations.get(slot).stream().mapToInt(Reservation::amount).sum();
        }
    }

    @Test
    void smallPicksOfOneItemEndInOneSlot() {
        Incoming buffer = new Incoming().empties(9);
        for (int amount : new int[] {3, 1, 2, 3, 2, 3}) {
            for (Part part : buffer.pick("coal", amount)) {
                assertEquals(0, part.slot());
            }
        }
        assertEquals(14, buffer.reservedAt(0));
        for (int slot = 1; slot < 9; slot++) {
            assertEquals(0, buffer.reservedAt(slot));
        }
    }

    @Test
    void pickJoinsAPickOnItsWay() {
        Incoming buffer = new Incoming().slot(null, 0, 64, new Reservation<>("coal", 3)).empties(1);
        assertEquals(List.of(new Part(0, 2)), buffer.pick("coal", 2));
    }

    @Test
    void nearlyFullStackSplits() {
        Incoming buffer = new Incoming().slot("coal", 60, 64).empties(1);
        assertEquals(List.of(new Part(0, 4), new Part(1, 6)), buffer.pick("coal", 10));
    }

    @Test
    void fullStackSpillsToAnEmptySlot() {
        Incoming buffer = new Incoming().slot("coal", 64, 64).empties(1);
        assertEquals(List.of(new Part(1, 8)), buffer.pick("coal", 8));
    }

    @Test
    void slotReservedForAnotherItemTakesNothing() {
        Incoming buffer = new Incoming().slot(null, 0, 64, new Reservation<>("iron", 8));
        assertEquals(List.of(), buffer.pick("coal", 8));
    }

    @Test
    void stackAndReservationsDisagreeTakesNothing() {
        Slot<String> mixed = SlotFill.incoming("coal", 10, 64, List.of(new Reservation<>("iron", 4)));
        assertTrue(mixed.locked());
        assertEquals(List.of(), SlotFill.in(List.of(mixed), "coal", 8, 64));
        assertEquals(List.of(), SlotFill.in(List.of(mixed), "iron", 8, 64));
    }

    @Test
    void planNeverExceedsRoomLeft() {
        Incoming buffer = new Incoming().slot("coal", 50, 64, new Reservation<>("coal", 10)).empties(2);
        List<Part> parts = buffer.pick("coal", 200);
        assertEquals(List.of(new Part(0, 4), new Part(1, 64), new Part(2, 64)), parts);
        assertEquals(132, SlotFill.total(parts));
    }

    @Test
    void reservationsOnTheirWayCountAgainstRoom() {
        Incoming buffer = new Incoming().slot("coal", 60, 64).slot(null, 0, 16);
        assertEquals(12, SlotFill.total(buffer.pick("coal", 12)));
        assertEquals(8, SlotFill.total(SlotFill.in(buffer.views(), "coal", 10, 64)));
    }
}
