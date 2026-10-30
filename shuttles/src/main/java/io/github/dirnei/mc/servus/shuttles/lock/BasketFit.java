package io.github.dirnei.mc.servus.shuttles.lock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * Whether several loads fit into a set of slots at once, placed one after
 * the other as their IN locks would be: each load tops up unlocked partial
 * stacks of its key first, then takes unlocked empty slots, and every slot a
 * load lands in counts as locked for the loads after it. Pure Java.
 */
public final class BasketFit {

    private BasketFit() {
    }

    /** One load to place: {@code count} of the key. */
    public record Move<K>(K key, int count) {
    }

    /**
     * Where each move goes, in move order, or empty when any move finds less
     * room than its count.
     *
     * @param maxStack the stack size of a key, the most one slot takes of it
     */
    public static <K> Optional<List<List<SlotFill.Part>>> place(List<SlotFill.Slot<K>> slots, List<Move<K>> moves,
                                                               ToIntFunction<K> maxStack) {
        List<SlotFill.Slot<K>> states = new ArrayList<>(slots);
        List<List<SlotFill.Part>> placed = new ArrayList<>(moves.size());
        for (Move<K> move : moves) {
            List<SlotFill.Part> parts = SlotFill.in(states, move.key(), move.count(), maxStack.applyAsInt(move.key()));
            if (SlotFill.total(parts) < move.count()) {
                return Optional.empty();
            }
            for (SlotFill.Part part : parts) {
                SlotFill.Slot<K> slot = states.get(part.slot());
                states.set(part.slot(), new SlotFill.Slot<>(move.key(), slot.count(), slot.limit(), true));
            }
            placed.add(List.copyOf(parts));
        }
        return Optional.of(placed);
    }

    /** Whether every move finds room; see {@link #place}. */
    public static <K> boolean fits(List<SlotFill.Slot<K>> slots, List<Move<K>> moves, ToIntFunction<K> maxStack) {
        return place(slots, moves, maxStack).isPresent();
    }
}
