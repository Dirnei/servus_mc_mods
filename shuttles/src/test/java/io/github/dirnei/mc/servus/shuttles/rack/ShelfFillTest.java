package io.github.dirnei.mc.servus.shuttles.rack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.lock.SlotFill;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShelfFillTest {

    private static List<SlotFill.Slot<String>> slots(Object... pairs) {
        List<SlotFill.Slot<String>> slots = new ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2) {
            slots.add(new SlotFill.Slot<>((String) pairs[i], (Integer) pairs[i + 1], 64, false));
        }
        return slots;
    }

    @Test
    void totalsAddUpOverSeveralSlotsOfOneItem() {
        ShelfFill<String> fill = ShelfFill.of(slots("cobble", 64, "iron", 40, "cobble", 64, "cobble", 8), 5);
        assertEquals(List.of(new ShelfFill.Entry<>("cobble", 136), new ShelfFill.Entry<>("iron", 40)), fill.top());
        assertEquals(4, fill.used());
    }

    @Test
    void largestFirstAndTiesInSlotOrder() {
        ShelfFill<String> fill = ShelfFill.of(slots("dirt", 10, "sand", 30, "gravel", 10), 5);
        assertEquals(List.of("sand", "dirt", "gravel"), fill.top().stream().map(ShelfFill.Entry::key).toList());
    }

    @Test
    void topIsLimited() {
        ShelfFill<String> fill = ShelfFill.of(slots("a", 5, "b", 4, "c", 3, "d", 2), 2);
        assertEquals(List.of("a", "b"), fill.top().stream().map(ShelfFill.Entry::key).toList());
    }

    @Test
    void emptySlotsCountTowardsTotalOnly() {
        ShelfFill<String> fill = ShelfFill.of(slots(null, 0, "iron", 3, "iron", 0, null, 0), 5);
        assertEquals(1, fill.used());
        assertEquals(4, fill.total());
        assertFalse(fill.full());
        assertFalse(fill.isEmpty());
    }

    @Test
    void lockedEmptySlotCountsAsUsed() {
        List<SlotFill.Slot<String>> slots = slots("iron", 3, null, 0);
        slots.set(1, new SlotFill.Slot<>(null, 0, 64, true));
        ShelfFill<String> fill = ShelfFill.of(slots, 5);
        assertEquals(2, fill.used());
        assertTrue(fill.full());
        assertEquals(List.of(new ShelfFill.Entry<>("iron", 3)), fill.top());
    }

    @Test
    void fullWhenEverySlotHoldsItems() {
        ShelfFill<String> fill = ShelfFill.of(slots("a", 1, "b", 64), 5);
        assertTrue(fill.full());
    }

    @Test
    void emptyShelf() {
        ShelfFill<String> fill = ShelfFill.of(slots(null, 0, null, 0), 5);
        assertTrue(fill.isEmpty());
        assertFalse(fill.full());
        assertTrue(fill.top().isEmpty());
    }
}
