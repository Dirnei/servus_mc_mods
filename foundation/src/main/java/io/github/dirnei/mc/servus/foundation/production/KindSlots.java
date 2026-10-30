package io.github.dirnei.mc.servus.foundation.production;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * The one kind per slot rule of input slots that serve a shapeless recipe: each kind goes into at most one slot.
 * Pure Java.
 */
public final class KindSlots {

    private KindSlots() {
    }

    /**
     * Whether {@code candidate} may go into {@code slot}: the slot holds the same kind, or it is empty and no other slot
     * holds that kind.
     */
    public static <T> boolean accepts(List<T> slots, int slot, T candidate, Predicate<? super T> isEmpty,
                                      BiPredicate<? super T, ? super T> sameKind) {
        T here = slots.get(slot);
        if (!isEmpty.test(here)) {
            return sameKind.test(here, candidate);
        }
        for (int other = 0; other < slots.size(); other++) {
            T content = slots.get(other);
            if (other != slot && !isEmpty.test(content) && sameKind.test(content, candidate)) {
                return false;
            }
        }
        return true;
    }
}
