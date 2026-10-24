package io.github.dirnei.mc.servus.shuttles.terminal;

import java.util.ArrayList;
import java.util.List;

/**
 * Which output slots of a Storage Terminal a new request owns. Pure, no
 * Minecraft imports.
 */
public final class RequestSlots {

    /** No slot is free. */
    public static final int NONE = -1;

    /**
     * One output slot as the choice sees it.
     *
     * @param owned whether an open request owns the slot
     * @param item  the item it holds, null when empty
     * @param room  whether one more of that item fits
     * @param <K>   how items are told apart
     */
    public record Slot<K>(boolean owned, K item, boolean room) {

        public static <K> Slot<K> empty() {
            return new Slot<>(false, null, true);
        }
    }

    private RequestSlots() {
    }

    /**
     * The slot for a new request of {@code item}: an unowned slot holding the
     * same item with room first, then the leftmost empty unowned slot, else {@link #NONE}.
     */
    public static <K> int choose(List<Slot<K>> slots, K item) {
        List<Integer> chosen = choose(slots, item, 1);
        return chosen.isEmpty() ? NONE : chosen.get(0);
    }

    /**
     * Up to {@code wanted} slots for a new request of {@code item}, at least
     * one: unowned slots holding the same item with room first, then the
     * leftmost empty unowned slots; empty when none is free.
     */
    public static <K> List<Integer> choose(List<Slot<K>> slots, K item, int wanted) {
        int limit = Math.max(1, wanted);
        List<Integer> chosen = new ArrayList<>();
        for (int i = 0; i < slots.size() && chosen.size() < limit; i++) {
            Slot<K> slot = slots.get(i);
            if (!slot.owned() && item.equals(slot.item()) && slot.room()) {
                chosen.add(i);
            }
        }
        for (int i = 0; i < slots.size() && chosen.size() < limit; i++) {
            Slot<K> slot = slots.get(i);
            if (!slot.owned() && slot.item() == null) {
                chosen.add(i);
            }
        }
        return chosen;
    }

    /** How many slots {@code amount} items fill at {@code stackSize} per slot, at least one. */
    public static int needed(int amount, int stackSize) {
        int size = Math.max(1, stackSize);
        return Math.max(1, (Math.max(0, amount) + size - 1) / size);
    }
}
