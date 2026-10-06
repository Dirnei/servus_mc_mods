package io.github.dirnei.mc.servus.storage.rack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RackRowTest {

    @Test
    void singleBayShowsBothPosts() {
        assertFalse(RackRow.leftPostHidden(0, 3));
        assertFalse(RackRow.rightPostHidden(0, 1, 3));
    }

    @Test
    void rowOfThreeHidesTheInnerPosts() {
        assertFalse(RackRow.leftPostHidden(0, 3));
        assertTrue(RackRow.rightPostHidden(0, 3, 3));
        assertTrue(RackRow.leftPostHidden(1, 3));
        assertTrue(RackRow.rightPostHidden(1, 3, 3));
        assertTrue(RackRow.leftPostHidden(2, 3));
        assertFalse(RackRow.rightPostHidden(2, 3, 3));
    }

    @Test
    void rowOfFiveHasAPostAfterTheThirdBay() {
        assertFalse(RackRow.rightPostHidden(2, 5, 3), "post between bay 3 and 4");
        assertFalse(RackRow.leftPostHidden(3, 3), "bay 4 shows its left post");
        assertTrue(RackRow.rightPostHidden(3, 5, 3));
        assertTrue(RackRow.leftPostHidden(4, 3));
        assertFalse(RackRow.rightPostHidden(4, 5, 3), "row end");
    }

    @Test
    void postsEveryOneMeansEveryBayHasPosts() {
        assertFalse(RackRow.leftPostHidden(1, 1));
        assertFalse(RackRow.rightPostHidden(1, 4, 1));
        assertFalse(RackRow.leftPostHidden(2, 0), "zero is treated as one");
    }
}
