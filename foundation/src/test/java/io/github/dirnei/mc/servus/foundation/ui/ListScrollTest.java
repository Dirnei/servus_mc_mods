package io.github.dirnei.mc.servus.foundation.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ListScrollTest {

    @Test
    void clampsToLastPage() {
        assertEquals(8, ListScroll.clamp(12, 11, 3));
        assertEquals(2, ListScroll.clamp(8, 5, 3));
    }

    @Test
    void fewerRowsThanVisible() {
        assertEquals(0, ListScroll.clamp(4, 2, 3));
        assertFalse(ListScroll.scrolls(3, 3));
        assertEquals(27, ListScroll.thumbLength(2, 3, 27));
    }

    @Test
    void scrollStopsAtEnds() {
        assertEquals(2, ListScroll.scroll(0, 2, 11, 3));
        assertEquals(8, ListScroll.scroll(8, 1, 11, 3));
        assertEquals(0, ListScroll.scroll(0, -1, 11, 3));
    }

    @Test
    void thumbSpansTrack() {
        assertTrue(ListScroll.scrolls(11, 3));
        assertEquals(7, ListScroll.thumbLength(11, 3, 27));
        assertEquals(2, ListScroll.thumbLength(1000, 3, 27));
    }

    @Test
    void thumbMovesWithOffset() {
        assertEquals(0, ListScroll.thumbStart(0, 11, 3, 27));
        assertEquals(20, ListScroll.thumbStart(8, 11, 3, 27));
        assertEquals(10, ListScroll.thumbStart(4, 11, 3, 27));
    }

    @Test
    void dragPositionGivesOffset() {
        assertEquals(0, ListScroll.offsetAt(7.5, 15, 115, 20), "thumb at the top");
        assertEquals(20, ListScroll.offsetAt(107.5, 15, 115, 20), "thumb at the bottom");
        assertEquals(10, ListScroll.offsetAt(57.5, 15, 115, 20), "middle of the bar");
    }

    @Test
    void dragOutsideTheBarClamps() {
        assertEquals(0, ListScroll.offsetAt(-40, 15, 115, 20));
        assertEquals(20, ListScroll.offsetAt(500, 15, 115, 20));
    }

    @Test
    void dragWithNothingToScrollStaysAtTop() {
        assertEquals(0, ListScroll.offsetAt(80, 15, 115, 0));
    }
}
