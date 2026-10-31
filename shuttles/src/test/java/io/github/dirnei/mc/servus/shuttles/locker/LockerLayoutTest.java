package io.github.dirnei.mc.servus.shuttles.locker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LockerLayoutTest {

    private static final LockerLayout.Capacities DEFAULT = new LockerLayout.Capacities(3, 6, 9);

    @Test
    void sixSmallThreeMediumOneLarge() {
        assertEquals(10, LockerLayout.BOXES.size());
        assertEquals(6, LockerLayout.count(LockerLayout.Size.S));
        assertEquals(3, LockerLayout.count(LockerLayout.Size.M));
        assertEquals(1, LockerLayout.count(LockerLayout.Size.L));
        assertEquals(List.of("S-01", "S-02"), LockerLayout.boxesIn(0, 1).stream().map(LockerLayout.Box::id).toList());
        assertEquals(List.of("M-01"), LockerLayout.boxesIn(1, 0).stream().map(LockerLayout.Box::id).toList());
        assertTrue(LockerLayout.boxesIn(LockerLayout.HEAD_COLUMN, 0).isEmpty());
        assertEquals(List.of("L-01"), LockerLayout.boxesIn(4, 1).stream().map(LockerLayout.Box::id).toList());
    }

    @Test
    void slotRangesDoNotOverlap() {
        List<LockerLayout.Range> ranges = DEFAULT.ranges();
        int next = 0;
        for (int i = 0; i < ranges.size(); i++) {
            LockerLayout.Range range = ranges.get(i);
            assertEquals(next, range.first(), "box " + i + " starts where the one before ends");
            assertEquals(DEFAULT.of(LockerLayout.BOXES.get(i).size()), range.count());
            next = range.first() + range.count();
        }
        assertEquals(9, DEFAULT.boxOf(next - 1));
        assertEquals(-1, DEFAULT.boxOf(next));
        assertFalse(ranges.get(0).contains(3));
        assertTrue(ranges.get(1).contains(3));
    }

    @Test
    void totalStacksFromSizes() {
        assertEquals(45, DEFAULT.total());
        assertEquals(6 + 3 * 2 + 4, new LockerLayout.Capacities(1, 2, 4).total());
    }

    @Test
    void lowerSOfFourColumn() {
        assertEquals("S-04", LockerLayout.boxAt(0, 0, 3).id());
        assertEquals("S-03", LockerLayout.boxAt(0, 0, 8).id());
        assertEquals("S-01", LockerLayout.boxAt(0, 1, 15.5).id());
        assertEquals("S-01", LockerLayout.boxAt(0, 1, 16).id());
    }

    @Test
    void mBoxBelowTwoS() {
        assertEquals("M-01", LockerLayout.boxAt(1, 0, 12).id());
        assertEquals("S-06", LockerLayout.boxAt(1, 1, 2).id());
        assertEquals("S-05", LockerLayout.boxAt(1, 1, 9).id());
    }

    @Test
    void lBoxFromBothParts() {
        assertEquals("L-01", LockerLayout.boxAt(4, 0, 0).id());
        assertEquals("L-01", LockerLayout.boxAt(4, 1, 14).id());
        assertEquals("M-03", LockerLayout.boxAt(3, 0, 5).id());
        assertEquals("M-02", LockerLayout.boxAt(3, 1, 5).id());
    }

    @Test
    void headHasNoBox() {
        assertNull(LockerLayout.boxAt(LockerLayout.HEAD_COLUMN, 0, 8));
        assertNull(LockerLayout.boxAt(LockerLayout.HEAD_COLUMN, 1, 8));
    }
}
