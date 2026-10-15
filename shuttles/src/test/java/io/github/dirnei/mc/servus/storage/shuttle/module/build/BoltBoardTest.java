package io.github.dirnei.mc.servus.storage.shuttle.module.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BoltBoardTest {

    @Test
    void spotsLieOnTheBoardAndApart() {
        BoltBoard board = new BoltBoard(7, 5, 120, 100);
        assertEquals(5, board.spots().size());
        for (Spot spot : board.spots()) {
            assertTrue(spot.x() >= 0 && spot.x() < 120 && spot.y() >= 0 && spot.y() < 100);
        }
        List<Spot> spots = board.spots();
        for (int i = 0; i < spots.size(); i++) {
            for (int j = i + 1; j < spots.size(); j++) {
                assertFalse(spots.get(i).near(spots.get(j).x(), spots.get(j).y(), BoltBoard.RADIUS * 2), "spots do not overlap");
            }
        }
    }

    @Test
    void onlyTheGlowingBoltCounts() {
        BoltBoard board = new BoltBoard(3, 3, 120, 100);
        Spot second = board.spots().get(1);
        assertFalse(board.click(second.x(), second.y()), "the second bolt is not next yet");
        assertEquals(0, board.tightened());
        Spot first = board.spots().get(0);
        assertFalse(board.click(first.x() + BoltBoard.RADIUS + 3, first.y()), "a miss does nothing");
        assertTrue(board.click(first.x() + 2, first.y() - 2));
        assertEquals(1, board.tightened());
    }

    @Test
    void doneAfterEveryBolt() {
        BoltBoard board = new BoltBoard(11, 4, 120, 100);
        for (Spot spot : board.spots()) {
            assertFalse(board.done());
            assertTrue(board.click(spot.x(), spot.y()));
        }
        assertTrue(board.done());
        assertFalse(board.click(0, 0));
    }

    @Test
    void sameSeedSameLayout() {
        assertEquals(new BoltBoard(99, 4, 120, 100).spots(), new BoltBoard(99, 4, 120, 100).spots());
    }
}
