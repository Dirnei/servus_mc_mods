package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FieldReachTest {

    private static final FieldPos RAIL = new FieldPos(0, 64, 0);

    private static Set<FieldPos> candidates() {
        return FieldStrip.plots(List.of(RAIL));
    }

    @Test
    void openGroundIsReachedWhole() {
        Set<FieldPos> reached = FieldReach.connected(List.of(RAIL), candidates(), plot -> true);
        assertEquals(80, reached.size());
    }

    @Test
    void wallCutsOffWhatLiesBehindIt() {
        // A wall along x = 2 from z = -4 to 4: everything east of it is out of sight.
        Set<FieldPos> reached = FieldReach.connected(List.of(RAIL), candidates(), plot -> plot.x() != 2);
        assertTrue(reached.contains(new FieldPos(1, 64, 0)));
        assertFalse(reached.contains(new FieldPos(2, 64, 0)));
        assertFalse(reached.contains(new FieldPos(3, 64, 0)));
        assertEquals(9 * 6 - 1, reached.size());
    }

    @Test
    void blockerWithAWayAroundKeepsTheFieldWhole() {
        Set<FieldPos> reached = FieldReach.connected(List.of(RAIL), candidates(), plot -> !plot.equals(new FieldPos(2, 64, 0)));
        assertTrue(reached.contains(new FieldPos(3, 64, 0)));
        assertEquals(79, reached.size());
    }

    @Test
    void nothingOpenNothingReached() {
        assertTrue(FieldReach.connected(List.of(RAIL), candidates(), plot -> false).isEmpty());
    }

    @Test
    void neverLeavesTheCandidates() {
        Set<FieldPos> reached = FieldReach.connected(List.of(RAIL), candidates(), plot -> true);
        assertFalse(reached.contains(new FieldPos(5, 64, 0)));
        assertFalse(reached.contains(RAIL));
    }

    @Test
    void stepsAreSideBySideOnly() {
        // Only the diagonal (1, 1) is open: it touches the rail on a corner, which does not count.
        Set<FieldPos> reached = FieldReach.connected(List.of(RAIL), candidates(), plot -> plot.equals(new FieldPos(1, 64, 1)));
        assertTrue(reached.isEmpty());
    }
}
