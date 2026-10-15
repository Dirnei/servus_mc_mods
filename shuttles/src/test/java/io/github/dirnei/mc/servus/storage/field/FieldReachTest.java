package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FieldReachTest {

    private static final FieldPos RAIL = new FieldPos(0, 64, 0);

    private static FieldPos at(int x, int z) {
        return new FieldPos(x, 64, z);
    }

    private static Set<FieldPos> reach(List<FieldPos> rails, java.util.function.Predicate<FieldPos> open) {
        return FieldReach.connected(rails, FieldStrip.plots(rails, 4), open, 4);
    }

    @Test
    void lonelyRailSeesItsWholeSquare() {
        assertEquals(80, reach(List.of(RAIL), plot -> true).size());
    }

    @Test
    void wallCutsOffWhatLiesBehindIt() {
        // A wall along x = 2: everything east of it is out of sight.
        Set<FieldPos> reached = reach(List.of(RAIL), plot -> plot.x() != 2);
        assertTrue(reached.contains(at(1, 0)));
        assertFalse(reached.contains(at(3, 0)));
        assertEquals(9 * 6 - 1, reached.size());
    }

    @Test
    void blockerShadowsTheLineBehindIt() {
        // One pipe beside the rail: the straight line behind it stays out, nothing goes around it.
        List<FieldPos> rails = List.of(at(-1, 0), RAIL, at(1, 0));
        Set<FieldPos> reached = reach(rails, plot -> !plot.equals(at(0, 1)));
        for (int z = 2; z <= 4; z++) {
            assertFalse(reached.contains(at(0, z)), "behind the pipe at z " + z);
        }
        assertTrue(reached.contains(at(1, 3)));
        assertTrue(reached.contains(at(-1, 3)));
    }

    @Test
    void middleRailsLookOnlySideways() {
        // A run along x from -4 to 4 sees its two sides; the ends see past the run.
        List<FieldPos> rails = new java.util.ArrayList<>();
        for (int x = -4; x <= 4; x++) {
            rails.add(at(x, 0));
        }
        Set<FieldPos> reached = reach(rails, plot -> true);
        assertTrue(reached.contains(at(0, 4)) && reached.contains(at(0, -4)));
        assertTrue(reached.contains(at(8, 0)), "the end rail looks along its run");
        assertTrue(reached.contains(at(8, 4)), "the end rail sees its corner");
    }

    @Test
    void cornerNeedsAnOpenLeg() {
        // At the east end of a run, the corner is seen only along an L of open plots.
        List<FieldPos> rails = List.of(at(-1, 0), RAIL);
        Set<FieldPos> blockedLegs = reach(rails, plot -> !plot.equals(at(1, 0)) && !plot.equals(at(0, 1)));
        assertFalse(blockedLegs.contains(at(1, 1)), "both legs to the corner plot are blocked");
        Set<FieldPos> oneLeg = reach(rails, plot -> !plot.equals(at(1, 0)));
        assertTrue(oneLeg.contains(at(1, 1)), "out south, then east");
    }

    @Test
    void crossingSeesNoCorners() {
        List<FieldPos> rails = List.of(RAIL, at(1, 0), at(-1, 0), at(0, 1), at(0, -1));
        Set<FieldPos> reached = reach(rails, plot -> true);
        assertTrue(reached.contains(at(2, 0)), "the arm ends look further along their arm");
        assertFalse(reached.contains(at(0, 0)));
    }

    @Test
    void nothingOpenNothingReached() {
        assertTrue(reach(List.of(RAIL), plot -> false).isEmpty());
    }

    @Test
    void neverLeavesTheCandidates() {
        Set<FieldPos> reached = reach(List.of(RAIL), plot -> true);
        assertFalse(reached.contains(at(5, 0)));
        assertFalse(reached.contains(RAIL));
    }

    @Test
    void radiusTwoLooksTwoOut() {
        Set<FieldPos> reached = FieldReach.connected(List.of(RAIL), FieldStrip.plots(List.of(RAIL), 2), plot -> true, 2);
        assertEquals(24, reached.size());
        assertTrue(reached.contains(at(2, 2)));
        assertFalse(reached.contains(at(3, 0)));
    }
}
