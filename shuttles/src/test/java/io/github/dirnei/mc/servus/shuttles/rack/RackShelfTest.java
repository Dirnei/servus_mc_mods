package io.github.dirnei.mc.servus.shuttles.rack;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.function.IntPredicate;
import org.junit.jupiter.api.Test;

class RackShelfTest {

    /** Joins left for every bay of a joined row from {@code first} to {@code last} except the first. */
    private static IntPredicate left(int first, int last) {
        return i -> i > first && i <= last;
    }

    /** Joins right for every bay of a joined row from {@code first} to {@code last} except the last. */
    private static IntPredicate right(int first, int last) {
        return i -> i >= first && i < last;
    }

    @Test
    void loneBay() {
        assertArrayEquals(new int[] {0}, RackShelf.bays(i -> false, i -> false, 64));
    }

    @Test
    void startAtTheLeftEnd() {
        assertArrayEquals(new int[] {0, 1, 2}, RackShelf.bays(left(0, 2), right(0, 2), 64));
    }

    @Test
    void startInTheMiddle() {
        assertArrayEquals(new int[] {-1, 0, 1}, RackShelf.bays(left(-1, 1), right(-1, 1), 64));
    }

    @Test
    void startAtTheRightEnd() {
        assertArrayEquals(new int[] {-2, -1, 0}, RackShelf.bays(left(-2, 0), right(-2, 0), 64));
    }

    @Test
    void oneSidedFlagDoesNotJoin() {
        // Bay 0 claims both neighbours, but neither claims bay 0 back.
        IntPredicate joinsLeft = i -> i == 0;
        IntPredicate joinsRight = i -> i == 0;
        assertArrayEquals(new int[] {0}, RackShelf.bays(joinsLeft, joinsRight, 64));
    }

    @Test
    void limitStopsTheWalk() {
        assertArrayEquals(new int[] {-2, -1, 0, 1, 2}, RackShelf.bays(i -> true, i -> true, 2));
    }
}
