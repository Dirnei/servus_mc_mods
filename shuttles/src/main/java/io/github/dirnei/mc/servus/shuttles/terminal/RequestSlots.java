package io.github.dirnei.mc.servus.shuttles.terminal;

import java.util.List;

/**
 * Which output slot of a Storage Terminal a new request owns. Pure, no
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
        for (int i = 0; i < slots.size(); i++) {
            Slot<K> slot = slots.get(i);
            if (!slot.owned() && item.equals(slot.item()) && slot.room()) {
                return i;
            }
        }
        for (int i = 0; i < slots.size(); i++) {
            Slot<K> slot = slots.get(i);
            if (!slot.owned() && slot.item() == null) {
                return i;
            }
        }
        return NONE;
    }
}
