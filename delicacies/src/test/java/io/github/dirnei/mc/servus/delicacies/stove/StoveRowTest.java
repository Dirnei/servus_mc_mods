package io.github.dirnei.mc.servus.delicacies.stove;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class StoveRowTest {

    private static StoveRow.Result row(int maxLength, Integer... tiles) {
        Set<Integer> loose = Set.of(tiles);
        return StoveRow.find(loose::contains, maxLength);
    }

    @Test
    void aSingleTileIsTooShort() {
        assertEquals(StoveRow.Outcome.TOO_SHORT, row(4, 0).outcome());
    }

    @Test
    void twoTilesFormFromEitherEnd() {
        StoveRow.Result right = row(4, 0, 1);
        assertTrue(right.formed());
        assertEquals(0, right.from());
        assertEquals(1, right.to());
        StoveRow.Result left = row(4, -1, 0);
        assertTrue(left.formed());
        assertEquals(-1, left.from());
        assertEquals(2, left.length());
    }

    @Test
    void aMiddleClickTakesBothSides() {
        StoveRow.Result result = row(4, -1, 0, 1);
        assertTrue(result.formed());
        assertEquals(-1, result.from());
        assertEquals(1, result.to());
    }

    @Test
    void aGapEndsTheRow() {
        StoveRow.Result result = row(4, 0, 1, 3, 4);
        assertTrue(result.formed());
        assertEquals(2, result.length());
    }

    @Test
    void exactlyTheMaximumForms() {
        StoveRow.Result result = row(4, -2, -1, 0, 1);
        assertTrue(result.formed());
        assertEquals(4, result.length());
    }

    @Test
    void oneMoreThanTheMaximumIsTooLong() {
        assertEquals(StoveRow.Outcome.TOO_LONG, row(4, -2, -1, 0, 1, 2).outcome());
        assertEquals(StoveRow.Outcome.TOO_LONG, row(4, 0, 1, 2, 3, 4, 5, 6, 7, 8).outcome());
    }
}
