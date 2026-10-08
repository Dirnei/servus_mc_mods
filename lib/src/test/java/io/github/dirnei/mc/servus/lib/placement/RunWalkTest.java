package io.github.dirnei.mc.servus.lib.placement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.lib.placement.RunWalk.Cell;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RunWalkTest {

    private static final Cell EAST = new Cell(1, 0, 0);
    private static final Cell UP = new Cell(0, 1, 0);

    private static Set<Cell> line(int from, int to) {
        Set<Cell> cells = new HashSet<>();
        for (int x = from; x <= to; x++) {
            cells.add(new Cell(x, 0, 0));
        }
        return cells;
    }

    private static Optional<Cell> walk(Cell start, Cell step, Set<Cell> run, Set<Cell> blocked, int reach) {
        return RunWalk.walk(start, step, run::contains, cell -> !run.contains(cell) && !blocked.contains(cell), reach);
    }

    @Test
    void runOfOnePlacesBeside() {
        assertEquals(Optional.of(new Cell(1, 0, 0)), walk(new Cell(0, 0, 0), EAST, line(0, 0), Set.of(), 16));
    }

    @Test
    void extendFromTheMiddle() {
        assertEquals(Optional.of(new Cell(5, 0, 0)), walk(new Cell(1, 0, 0), EAST, line(0, 4), Set.of(), 16));
    }

    @Test
    void blockedEndGivesNothing() {
        assertEquals(Optional.empty(), walk(new Cell(0, 0, 0), EAST, line(0, 3), Set.of(new Cell(4, 0, 0)), 16));
    }

    @Test
    void targetExactlyAtReach() {
        assertEquals(Optional.of(new Cell(16, 0, 0)), walk(new Cell(0, 0, 0), EAST, line(0, 15), Set.of(), 16));
    }

    @Test
    void targetOnePastReach() {
        assertEquals(Optional.empty(), walk(new Cell(0, 0, 0), EAST, line(0, 16), Set.of(), 16));
        assertEquals(Optional.empty(), walk(new Cell(0, 0, 0), EAST, line(0, 4), Set.of(), 4));
    }

    @Test
    void verticalStep() {
        Set<Cell> column = Set.of(new Cell(0, 0, 0), new Cell(0, 1, 0), new Cell(0, 2, 0), new Cell(0, 3, 0));
        assertEquals(Optional.of(new Cell(0, 4, 0)), walk(new Cell(0, 0, 0), UP, column, Set.of(), 16));
    }

    @Test
    void startIsNeverTested() {
        Set<Cell> asked = new HashSet<>();
        Optional<Cell> target = RunWalk.walk(new Cell(0, 0, 0), EAST, cell -> {
            asked.add(cell);
            return false;
        }, cell -> true, 16);
        assertEquals(Optional.of(new Cell(1, 0, 0)), target);
        assertFalse(asked.contains(new Cell(0, 0, 0)));
        assertTrue(asked.contains(new Cell(1, 0, 0)));
    }
}
