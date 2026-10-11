package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlotReachTest {

    private static List<FieldPos> row(int from, int to) {
        List<FieldPos> rails = new ArrayList<>();
        for (int x = from; x <= to; x++) {
            rails.add(new FieldPos(x, 0, 0));
        }
        return rails;
    }

    @Test
    void middleRailWorksItsCrossColumn() {
        List<FieldPos> rails = row(0, 4);
        Map<FieldPos, List<FieldPos>> reach = PlotReach.assign(rails, FieldStrip.plots(rails));
        List<FieldPos> middle = reach.get(new FieldPos(2, 0, 0));
        assertEquals(8, middle.size());
        for (FieldPos plot : middle) {
            assertEquals(2, plot.x());
        }
    }

    @Test
    void endRailsTakeTheEndColumns() {
        List<FieldPos> rails = row(0, 4);
        Map<FieldPos, List<FieldPos>> reach = PlotReach.assign(rails, FieldStrip.plots(rails));
        // The end rail works its own cross column and the four columns beyond it.
        assertEquals(8 + 4 * 9, reach.get(new FieldPos(0, 0, 0)).size());
        assertEquals(8 + 4 * 9, reach.get(new FieldPos(4, 0, 0)).size());
    }

    @Test
    void everyPlotOnceAndWithinFour() {
        List<FieldPos> rails = new ArrayList<>(row(0, 3));
        rails.add(new FieldPos(3, 0, 1));
        rails.add(new FieldPos(3, 0, 2));
        Set<FieldPos> plots = FieldStrip.plots(rails);
        Map<FieldPos, List<FieldPos>> reach = PlotReach.assign(rails, plots);
        Set<FieldPos> seen = new HashSet<>();
        int total = 0;
        for (Map.Entry<FieldPos, List<FieldPos>> entry : reach.entrySet()) {
            for (FieldPos plot : entry.getValue()) {
                assertTrue(seen.add(plot), "worked twice: " + plot);
                total++;
                int distance = Math.max(Math.abs(plot.x() - entry.getKey().x()), Math.abs(plot.z() - entry.getKey().z()));
                assertTrue(distance <= FieldStrip.RADIUS, "too far: " + plot);
            }
        }
        assertEquals(plots.size(), total);
    }

    @Test
    void nearestFirstWithinARail() {
        List<FieldPos> rails = row(0, 0);
        List<FieldPos> plots = reachOf(rails).get(new FieldPos(0, 0, 0));
        assertEquals(1, Math.max(Math.abs(plots.get(0).x()), Math.abs(plots.get(0).z())));
        FieldPos last = plots.get(plots.size() - 1);
        assertEquals(4, Math.max(Math.abs(last.x()), Math.abs(last.z())));
    }

    @Test
    void plotsBeyondReachAndRailsAreLeftOut() {
        List<FieldPos> rails = row(0, 0);
        Map<FieldPos, List<FieldPos>> reach = PlotReach.assign(rails,
                List.of(new FieldPos(5, 0, 0), new FieldPos(0, 0, 0), new FieldPos(1, 1, 0), new FieldPos(4, 0, 4)));
        List<FieldPos> worked = reach.get(new FieldPos(0, 0, 0));
        assertEquals(List.of(new FieldPos(4, 0, 4)), worked);
        assertFalse(worked.contains(new FieldPos(5, 0, 0)));
    }

    @Test
    void tiesAreStable() {
        List<FieldPos> rails = List.of(new FieldPos(0, 0, 0), new FieldPos(2, 0, 0));
        FieldPos between = new FieldPos(1, 0, 3);
        Map<FieldPos, List<FieldPos>> first = PlotReach.assign(rails, List.of(between));
        Map<FieldPos, List<FieldPos>> second = PlotReach.assign(List.of(rails.get(1), rails.get(0)), List.of(between));
        assertEquals(first.get(new FieldPos(0, 0, 0)), second.get(new FieldPos(0, 0, 0)));
    }

    private static Map<FieldPos, List<FieldPos>> reachOf(List<FieldPos> rails) {
        return PlotReach.assign(rails, FieldStrip.plots(rails));
    }
}
