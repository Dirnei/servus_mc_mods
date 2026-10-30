package io.github.dirnei.mc.servus.shuttles.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;

class BasketFitTest {

    private static final ToIntFunction<String> STACK = key -> key.equals("pearl") ? 16 : 64;

    private static SlotFill.Slot<String> empty() {
        return new SlotFill.Slot<>(null, 0, 64, false);
    }

    private static SlotFill.Slot<String> holding(String key, int count) {
        return new SlotFill.Slot<>(key, count, 64, false);
    }

    private static BasketFit.Move<String> move(String key, int count) {
        return new BasketFit.Move<>(key, count);
    }

    @Test
    void twoItemsShareTwoEmptySlots() {
        List<SlotFill.Slot<String>> slots = List.of(empty(), empty());
        assertTrue(BasketFit.fits(slots, List.of(move("cobblestone", 64), move("bread", 64)), STACK));
    }

    @Test
    void oneMoreDoesNotFit() {
        List<SlotFill.Slot<String>> slots = List.of(empty(), empty());
        assertFalse(BasketFit.fits(slots, List.of(move("cobblestone", 64), move("bread", 64), move("bread", 1)), STACK));
    }

    @Test
    void partialStackIsToppedUpFirst() {
        List<SlotFill.Slot<String>> slots = List.of(empty(), holding("cobblestone", 32));
        List<List<SlotFill.Part>> placed = BasketFit.place(slots, List.of(move("cobblestone", 32), move("bread", 64)), STACK).orElseThrow();
        assertEquals(List.of(new SlotFill.Part(1, 32)), placed.get(0));
        assertEquals(List.of(new SlotFill.Part(0, 64)), placed.get(1));
    }

    @Test
    void lockedSlotIsIgnored() {
        List<SlotFill.Slot<String>> slots = List.of(new SlotFill.Slot<>(null, 0, 64, true), empty());
        assertTrue(BasketFit.fits(slots, List.of(move("bread", 64)), STACK));
        assertFalse(BasketFit.fits(slots, List.of(move("bread", 64), move("stone", 1)), STACK));
    }

    @Test
    void lockedPartialStackIsIgnored() {
        List<SlotFill.Slot<String>> slots = List.of(new SlotFill.Slot<>("bread", 10, 64, true));
        assertFalse(BasketFit.fits(slots, List.of(move("bread", 1)), STACK));
    }

    @Test
    void exactFit() {
        List<SlotFill.Slot<String>> slots = List.of(holding("bread", 60), empty(), empty());
        assertTrue(BasketFit.fits(slots, List.of(move("bread", 4), move("bread", 64), move("stone", 64)), STACK));
    }

    @Test
    void aSlotTakenByOneLoadIsClosedToTheNext() {
        List<SlotFill.Slot<String>> slots = List.of(empty(), empty());
        assertFalse(BasketFit.fits(slots, List.of(move("bread", 30), move("bread", 30), move("stone", 1)), STACK));
    }

    @Test
    void stackLimitPerKey() {
        List<SlotFill.Slot<String>> slots = List.of(empty());
        assertTrue(BasketFit.fits(slots, List.of(move("pearl", 16)), STACK));
        assertFalse(BasketFit.fits(slots, List.of(move("pearl", 17)), STACK));
    }
}
