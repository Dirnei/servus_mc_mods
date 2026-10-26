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
        assertEquals(6, ListScroll.thumbLength(1000, 3, 27));
    }

    @Test
    void thumbMinimumNeverExceedsTrack() {
        assertEquals(4, ListScroll.thumbLength(1000, 3, 4));
    }

    @Test
    void thumbMovesWithOffset() {
        assertEquals(0, ListScroll.thumbStart(0, 11, 3, 27));
        assertEquals(20, ListScroll.thumbStart(8, 11, 3, 27));
        assertEquals(10, ListScroll.thumbStart(4, 11, 3, 27));
    }

    @Test
    void grabOnThumbKeepsPoint() {
        assertEquals(10.0, ListScroll.grabAt(30, 20, 15));
        assertEquals(0.0, ListScroll.grabAt(20, 20, 15));
    }

    @Test
    void grabOnTrackCentres() {
        assertEquals(7.5, ListScroll.grabAt(80, 20, 15));
        assertEquals(7.5, ListScroll.grabAt(35, 20, 15), "the first pixel below the thumb is track");
    }

    @Test
    void dragPositionGivesOffset() {
        assertEquals(0, ListScroll.offsetFor(7.5, 7.5, 15, 115, 20), "thumb at the top");
        assertEquals(20, ListScroll.offsetFor(107.5, 7.5, 15, 115, 20), "thumb at the bottom");
        assertEquals(10, ListScroll.offsetFor(57.5, 7.5, 15, 115, 20), "middle of the bar");
    }

    @Test
    void dragKeepsGrabPoint() {
        assertEquals(0, ListScroll.offsetFor(10, 10, 15, 115, 20));
        assertEquals(10, ListScroll.offsetFor(60, 10, 15, 115, 20));
    }

    @Test
    void dragOutsideTheBarClamps() {
        assertEquals(0, ListScroll.offsetFor(-40, 7.5, 15, 115, 20));
        assertEquals(20, ListScroll.offsetFor(500, 7.5, 15, 115, 20));
    }

    @Test
    void dragWithGrabClampsAtBothEnds() {
        assertEquals(0, ListScroll.offsetFor(2, 14, 15, 115, 20));
        assertEquals(20, ListScroll.offsetFor(114, 0, 15, 115, 20));
    }

    @Test
    void dragWithNothingToScrollStaysAtTop() {
        assertEquals(0, ListScroll.offsetFor(80, 7.5, 15, 115, 0));
        assertEquals(0, ListScroll.offsetFor(80, 7.5, 115, 115, 20));
    }

    @Test
    void pixelUnits() {
        assertEquals(81, ListScroll.thumbLength(400, 180, 180));
        assertEquals(220, ListScroll.offsetFor(99 + 40, 40, 81, 180, 220));
        assertEquals(99, ListScroll.thumbStart(220, 400, 180, 180));
    }
}
