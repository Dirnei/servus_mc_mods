package io.github.dirnei.mc.servus.shuttles.rack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RackDockTest {

    @Test
    void singleBayWithoutRailIsOpen() {
        assertFalse(RackDock.closed(new int[] {0}, offset -> false));
    }

    @Test
    void singleBayWithRailIsClosed() {
        assertTrue(RackDock.closed(new int[] {0}, offset -> offset == 0));
    }

    @Test
    void railAtTheLeftBayClosesAShelfOfThree() {
        assertTrue(RackDock.closed(new int[] {-2, -1, 0}, offset -> offset == -2));
    }

    @Test
    void shelfOfThreeWithoutRailsIsOpen() {
        assertFalse(RackDock.closed(new int[] {-1, 0, 1}, offset -> false));
    }

    @Test
    void offsetsFromRackShelfAreUsedAsGiven() {
        int[] offsets = RackShelf.bays(offset -> offset > -1, offset -> offset < 1, 8);
        List<Integer> asked = new ArrayList<>();
        assertFalse(RackDock.closed(offsets, offset -> {
            asked.add(offset);
            return false;
        }));
        List<Integer> expected = new ArrayList<>();
        for (int offset : offsets) {
            expected.add(offset);
        }
        assertEquals(expected, asked);
    }
}
