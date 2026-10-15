package io.github.dirnei.mc.servus.shuttles.shuttle.module.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class WireBoardTest {

    @Test
    void bothSidesHoldTheSameColours() {
        WireBoard board = new WireBoard(5, 4);
        assertEquals(4, board.left().size());
        assertEquals(new HashSet<>(board.left()), new HashSet<>(board.right()));
        assertEquals(4, new HashSet<>(board.left()).size(), "every colour once");
        assertNotEquals(board.left(), board.right(), "not every wire runs straight across");
    }

    @Test
    void wrongPairDoesNothing() {
        WireBoard board = new WireBoard(5, 4);
        assertTrue(board.select(0));
        int wrong = board.right().indexOf(board.left().get(1));
        assertFalse(board.connect(wrong));
        assertEquals(-1, board.wiredTo(0));
        assertEquals(0, board.selected(), "the contact stays picked up");
    }

    @Test
    void doneWhenEveryContactIsWired() {
        WireBoard board = new WireBoard(8, 5);
        for (int i = 0; i < board.left().size(); i++) {
            assertFalse(board.done());
            assertTrue(board.select(i));
            assertTrue(board.connect(board.right().indexOf(board.left().get(i))));
        }
        assertTrue(board.done());
        assertFalse(board.select(0), "a wired contact cannot be picked up");
    }

    @Test
    void connectNeedsAPickedUpContact() {
        WireBoard board = new WireBoard(1, 3);
        assertFalse(board.connect(0));
    }

    @Test
    void countsClamp() {
        assertEquals(8, new WireBoard(1, 12).left().size());
        assertEquals(1, new WireBoard(1, 0).left().size());
    }

    @Test
    void sameSeedSameBoard() {
        assertEquals(new WireBoard(77, 6).right(), new WireBoard(77, 6).right());
    }
}
