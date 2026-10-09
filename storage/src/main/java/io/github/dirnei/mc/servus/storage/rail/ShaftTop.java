package io.github.dirnei.mc.servus.storage.rail;

import java.util.OptionalInt;
import java.util.function.IntPredicate;

/**
 * Finds the spot above a shaft where a click with an elevator places the next
 * segment, like scaffolding: walk up through the segments of the column to the
 * first height that is not a segment. Pure, no Minecraft types.
 */
public final class ShaftTop {

    private ShaftTop() {
    }

    /**
     * @param start     height of the clicked block
     * @param maxHeight first height that is outside the world
     * @param segment   whether the block at a height is a segment
     * @return the first height above the column that is not a segment, or
     *         empty when the start is no segment or the walk leaves the world
     */
    public static OptionalInt firstFree(int start, int maxHeight, IntPredicate segment) {
        if (!segment.test(start)) {
            return OptionalInt.empty();
        }
        int y = start + 1;
        while (y < maxHeight && segment.test(y)) {
            y++;
        }
        return y < maxHeight ? OptionalInt.of(y) : OptionalInt.empty();
    }
}
