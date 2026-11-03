package io.github.dirnei.mc.servus.notes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BookLayoutTest {

    @Test
    void largeScreenGetsTheBiggestBookInTheMiddle() {
        BookLayout book = BookLayout.fit(960, 540, false);
        assertEquals(BookLayout.MAX_WIDTH, book.width());
        assertEquals(BookLayout.MAX_HEIGHT, book.height());
        assertEquals(1F, book.scale());
        assertEquals((960 - 576) / 2, book.left());
    }

    @Test
    void mediumScreenIsFilledWithAMargin() {
        BookLayout book = BookLayout.fit(500, 300, false);
        assertEquals(500 - 2 * BookLayout.SCREEN_MARGIN, book.width());
        assertEquals(300 - 2 * BookLayout.SCREEN_MARGIN - BookLayout.TAB_RISE, book.height());
        assertEquals(1F, book.scale());
        assertEquals(BookLayout.SCREEN_MARGIN, book.left());
        assertEquals(BookLayout.SCREEN_MARGIN + BookLayout.TAB_RISE, book.top());
    }

    @Test
    void smallWindowKeepsTheSmallestBookAndScalesIt() {
        BookLayout book = BookLayout.fit(300, 200, false);
        assertEquals(BookLayout.MIN_WIDTH, book.width());
        assertEquals(BookLayout.MIN_HEIGHT, book.height());
        assertTrue(book.scale() < 1F);
        assertTrue(book.width() * book.scale() <= 300 - 2 * BookLayout.SCREEN_MARGIN + 0.01F);
        assertTrue((book.height() + BookLayout.TAB_RISE) * book.scale() <= 200 - 2 * BookLayout.SCREEN_MARGIN + 0.01F);
    }

    @Test
    void roomRightMovesTheBookLeftAndLeavesSpace() {
        BookLayout book = BookLayout.fit(640, 360, true);
        assertEquals(BookLayout.SCREEN_MARGIN, book.left());
        assertEquals(640 - 2 * BookLayout.SCREEN_MARGIN - BookLayout.SIDE_ROOM, book.width());
        assertTrue(640 - book.left() - book.width() >= BookLayout.SIDE_ROOM);
    }

    @Test
    void roomRightNeverMakesTheBookSmallerThanTheMinimum() {
        BookLayout book = BookLayout.fit(427, 240, true);
        assertEquals(BookLayout.MIN_WIDTH, book.width());
    }

    @Test
    void pagesSplitTwoFifthsAndThreeFifths() {
        BookLayout small = BookLayout.fit(352, 239, false);
        assertEquals(336, small.width());
        assertEquals(124, small.leftPageWidth());
        assertEquals(136, small.rightPageX());
        assertEquals(188, small.rightPageWidth());
        assertEquals(186, small.pageHeight());
        BookLayout big = BookLayout.fit(1000, 600, false);
        assertEquals(big.width() - BookLayout.COVER_SIDE, big.rightPageX() + big.rightPageWidth());
    }
}
