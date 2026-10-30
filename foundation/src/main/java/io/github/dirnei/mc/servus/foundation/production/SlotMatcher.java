package io.github.dirnei.mc.servus.foundation.production;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Matches a shapeless list of requirements against slots. Pure Java. Every requirement needs a slot of its own, and
 * every slot that is not empty must serve a requirement.
 */
public final class SlotMatcher {

    private SlotMatcher() {
    }

    /**
     * The slot index serving each requirement, in requirement order, or empty when the slots do not fit the
     * requirements.
     */
    public static <T> Optional<int[]> match(List<? extends Predicate<? super T>> wants, List<T> slots, Predicate<? super T> isEmpty) {
        long filled = slots.stream().filter(slot -> !isEmpty.test(slot)).count();
        if (filled != wants.size()) {
            return Optional.empty();
        }
        int[] assignment = new int[wants.size()];
        boolean[] used = new boolean[slots.size()];
        return assign(0, wants, slots, isEmpty, assignment, used) ? Optional.of(assignment) : Optional.empty();
    }

    private static <T> boolean assign(int want, List<? extends Predicate<? super T>> wants, List<T> slots, Predicate<? super T> isEmpty,
                                      int[] assignment, boolean[] used) {
        if (want == wants.size()) {
            return true;
        }
        for (int slot = 0; slot < slots.size(); slot++) {
            T content = slots.get(slot);
            if (!used[slot] && !isEmpty.test(content) && wants.get(want).test(content)) {
                used[slot] = true;
                assignment[want] = slot;
                if (assign(want + 1, wants, slots, isEmpty, assignment, used)) {
                    return true;
                }
                used[slot] = false;
            }
        }
        return false;
    }
}
