package io.github.dirnei.mc.servus.shuttles.shuttle.module.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WeldBoardTest {

    @Test
    void farFromTheSeamNothingIsWelded() {
        WeldBoard board = new WeldBoard(4, 1, 120, 100);
        assertFalse(board.cover(-50, -50));
        assertEquals(0, board.current());
    }

    @Test
    void aSweepAlongTheSeamFinishesIt() {
        WeldBoard board = new WeldBoard(4, 1, 120, 100);
        for (Spot point : board.seams().get(0)) {
            board.cover(point.x(), point.y());
        }
        assertTrue(board.done());
    }

    @Test
    void mostOfTheSeamIsEnough() {
        WeldBoard board = new WeldBoard(9, 1, 120, 100);
        List<Spot> seam = board.seams().get(0);
        // Leave the last point, which can be on its own.
        for (int i = 0; i < seam.size() - 1; i++) {
            board.cover(seam.get(i).x(), seam.get(i).y());
        }
        assertTrue(board.done());
    }

    @Test
    void seamsOneAfterAnother() {
        WeldBoard board = new WeldBoard(6, 2, 120, 100);
        List<Spot> second = board.seams().get(1);
        for (Spot point : second) {
            board.cover(point.x(), point.y());
        }
        assertEquals(0, board.current(), "the second seam waits for the first");
        for (Spot point : board.seams().get(0)) {
            board.cover(point.x(), point.y());
        }
        assertEquals(1, board.current());
        for (Spot point : second) {
            board.cover(point.x(), point.y());
        }
        assertTrue(board.done());
    }

    @Test
    void seamsStayOnTheBoard() {
        WeldBoard board = new WeldBoard(2, 3, 120, 100);
        for (List<Spot> seam : board.seams()) {
            for (Spot point : seam) {
                assertTrue(point.x() >= 0 && point.x() <= 120 && point.y() >= 0 && point.y() <= 100);
            }
        }
    }
}
