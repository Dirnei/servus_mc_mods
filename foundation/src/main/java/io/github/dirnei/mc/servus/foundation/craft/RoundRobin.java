package io.github.dirnei.mc.servus.foundation.craft;

import java.util.function.IntPredicate;

/** Picks the next eligible index after a cursor, wrapping around. Pure Java. */
public final class RoundRobin {

    private RoundRobin() {
    }

    /**
     * @param size     number of candidates
     * @param cursor   index to start the search at (may be out of range)
     * @param eligible which candidates may be picked
     * @return the first eligible index starting at the cursor, or -1
     */
    public static int next(int size, int cursor, IntPredicate eligible) {
        if (size <= 0) {
            return -1;
        }
        int start = Math.floorMod(cursor, size);
        for (int i = 0; i < size; i++) {
            int index = (start + i) % size;
            if (eligible.test(index)) {
                return index;
            }
        }
        return -1;
    }
}
