package io.github.dirnei.mc.servus.shuttles.rack;

import io.github.dirnei.mc.servus.shuttles.lock.SlotFill;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * How full a rack shelf is: the slots in use (holding items or locked by a
 * job), all slots, and the items held most with their totals. Pure Java;
 * {@code K} is an item key compared with {@code equals}.
 *
 * @param used  slots in use, as {@link SlotFill#inUse} counts them
 * @param total all slots
 * @param top   the items held most, largest total first, ties in slot order
 */
public record ShelfFill<K>(int used, int total, List<Entry<K>> top) {

    /** An item and how many of it the shelf holds. */
    public record Entry<K>(K key, int count) {
    }

    /**
     * @param slots    the shelf's slots, bay after bay from left to right
     * @param topLimit how many items {@link #top()} lists at most
     */
    public static <K> ShelfFill<K> of(List<SlotFill.Slot<K>> slots, int topLimit) {
        Map<K, Integer> totals = new LinkedHashMap<>();
        for (SlotFill.Slot<K> slot : slots) {
            if (slot.key() != null && slot.count() > 0) {
                totals.merge(slot.key(), slot.count(), Integer::sum);
            }
        }
        List<Entry<K>> top = new ArrayList<>();
        for (K key : TopCounts.pick(totals, topLimit)) {
            top.add(new Entry<>(key, totals.get(key)));
        }
        return new ShelfFill<>(SlotFill.inUse(slots), slots.size(), List.copyOf(top));
    }

    /** Whether no slot is free. */
    public boolean full() {
        return total > 0 && used >= total;
    }

    /** Whether no slot is in use. */
    public boolean isEmpty() {
        return used == 0;
    }
}
