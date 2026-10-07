package io.github.dirnei.mc.servus.storage.rack;

import java.util.function.IntPredicate;

/**
 * Walks the bays of one rack section along a row. Offsets count bays to the
 * right of the start bay (negative to the left). Two neighbouring bays belong
 * to the same section only when both say so: the right one joins left and the
 * left one joins right. Pure, no Minecraft imports.
 */
public final class RackSection {

    private RackSection() {
    }

    /**
     * @param joinsLeft  whether the bay at an offset is joined to its left neighbour
     * @param joinsRight whether the bay at an offset is joined to its right neighbour
     * @param limit      steps the walk may take in each direction
     * @return the offsets of the section's bays from leftmost to rightmost, always containing 0
     */
    public static int[] bays(IntPredicate joinsLeft, IntPredicate joinsRight, int limit) {
        int left = 0;
        while (-left < limit && joinsLeft.test(left) && joinsRight.test(left - 1)) {
            left--;
        }
        int right = 0;
        while (right < limit && joinsRight.test(right) && joinsLeft.test(right + 1)) {
            right++;
        }
        int[] offsets = new int[right - left + 1];
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = left + i;
        }
        return offsets;
    }
}
