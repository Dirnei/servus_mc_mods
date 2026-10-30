package io.github.dirnei.mc.servus.shuttles.rack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RackRowTest {

    @Test
    void loneBayJoinsNothing() {
        assertFalse(RackRow.joinLeft(0, 3));
        assertFalse(RackRow.joinRight(false, 0, 0, 3));
    }

    @Test
    void joinsAShelfWithRoom() {
        assertTrue(RackRow.joinLeft(1, 3));
        assertTrue(RackRow.joinLeft(2, 3));
        assertTrue(RackRow.joinRight(false, 0, 2, 3));
    }

    @Test
    void refusesAFullShelf() {
        assertFalse(RackRow.joinLeft(3, 3));
        assertFalse(RackRow.joinRight(false, 0, 3, 3));
        assertFalse(RackRow.joinLeft(4, 3));
    }

    @Test
    void joinsTwoSingleBaysIntoThree() {
        assertTrue(RackRow.joinLeft(1, 3));
        assertTrue(RackRow.joinRight(true, 1, 1, 3));
    }

    @Test
    void refusesTheRightSideBeyondTheCap() {
        assertTrue(RackRow.joinLeft(2, 3));
        assertFalse(RackRow.joinRight(true, 2, 1, 3), "two plus one plus one would be four");
        assertTrue(RackRow.joinRight(false, 3, 2, 3), "left was full, right shelf of two takes the bay");
    }

    @Test
    void capOfOneNeverJoins() {
        assertFalse(RackRow.joinLeft(1, 1));
        assertFalse(RackRow.joinRight(false, 0, 1, 1));
    }
}
