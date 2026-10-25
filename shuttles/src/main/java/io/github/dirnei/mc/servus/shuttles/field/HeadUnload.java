package io.github.dirnei.mc.servus.shuttles.field;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * How the stacks a field shuttle brings back fill the To storage buffer of
 * its Field Head: slots of the same item with room first, then empty slots;
 * locked slots take nothing. What does not fit stays aboard. Pure Java.
 */
public final class HeadUnload {

    private HeadUnload() {
    }

    /** A buffer slot: its item (null when empty), count, stack size and whether a put-away holds it. */
    public record Slot<K>(K key, int count, int max, boolean locked) {

        public static <K> Slot<K> empty() {
            return new Slot<>(null, 0, 0, false);
        }

        boolean free() {
            return key == null && !locked;
        }
    }

    /** A stack on its way into the buffer. */
    public record Stack<K>(K key, int count, int max) {
    }

    /** The buffer slots after the unload and the stacks that stay aboard. */
    public record Plan<K>(List<Slot<K>> slots, List<Stack<K>> aboard) {
    }

    public static <K> Plan<K> plan(List<Slot<K>> slots, List<Stack<K>> stacks) {
        List<Slot<K>> after = new ArrayList<>(slots);
        List<Stack<K>> aboard = new ArrayList<>();
        for (Stack<K> stack : stacks) {
            int left = fillSameItem(after, stack);
            left = fillEmpty(after, stack, left);
            if (left > 0) {
                aboard.add(new Stack<>(stack.key(), left, stack.max()));
            }
        }
        return new Plan<>(after, aboard);
    }

    /** Whether some slot is empty and unlocked, so a field run can start. */
    public static <K> boolean hasFreeSlot(List<Slot<K>> slots) {
        return slots.stream().anyMatch(Slot::free);
    }

    private static <K> int fillSameItem(List<Slot<K>> slots, Stack<K> stack) {
        int left = stack.count();
        for (int i = 0; i < slots.size() && left > 0; i++) {
            Slot<K> slot = slots.get(i);
            if (!slot.locked() && Objects.equals(slot.key(), stack.key()) && slot.count() < slot.max()) {
                int moved = Math.min(left, slot.max() - slot.count());
                slots.set(i, new Slot<>(slot.key(), slot.count() + moved, slot.max(), false));
                left -= moved;
            }
        }
        return left;
    }

    private static <K> int fillEmpty(List<Slot<K>> slots, Stack<K> stack, int left) {
        for (int i = 0; i < slots.size() && left > 0; i++) {
            if (slots.get(i).free()) {
                int moved = Math.min(left, stack.max());
                slots.set(i, new Slot<>(stack.key(), moved, stack.max(), false));
                left -= moved;
            }
        }
        return left;
    }
}
