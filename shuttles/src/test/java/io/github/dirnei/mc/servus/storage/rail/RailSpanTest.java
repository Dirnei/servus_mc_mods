package io.github.dirnei.mc.servus.storage.rail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.storage.rail.RailSpan.Kind;
import io.github.dirnei.mc.servus.storage.rail.RailSpan.Verdict;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RailSpanTest {

    private final Map<String, Kind> kinds = new HashMap<>();
    private final Map<String, List<String>> links = new HashMap<>();

    /** A straight line of rails, each linked to the next; kinds by letter: S standing, H hung, F free. */
    private List<String> line(String pattern) {
        List<String> rails = new ArrayList<>();
        for (int i = 0; i < pattern.length(); i++) {
            String rail = "r" + i;
            kinds.put(rail, switch (pattern.charAt(i)) {
                case 'S' -> Kind.STANDING;
                case 'H' -> Kind.HUNG;
                default -> Kind.FREE;
            });
            links.put(rail, new ArrayList<>());
            if (i > 0) {
                link(rails.get(i - 1), rail);
            }
            rails.add(rail);
        }
        return rails;
    }

    private void link(String a, String b) {
        links.computeIfAbsent(a, k -> new ArrayList<>()).add(b);
        links.computeIfAbsent(b, k -> new ArrayList<>()).add(a);
    }

    private Verdict<String> check(String start) {
        return RailSpan.check(start, kinds::get, links::get, 3);
    }

    @Test
    void threeFreeRailsBetweenStandingRailsHold() {
        line("SFFFS");
        assertTrue(check("r2").holds());
    }

    @Test
    void fourFreeRailsBetweenStandingRailsFall() {
        List<String> rails = line("SFFFFS");
        Verdict<String> verdict = check("r1");
        assertFalse(verdict.holds());
        assertTrue(verdict.drop().contains(rails.get(1)));
        assertFalse(verdict.drop().contains(rails.get(0)), "standing rails never fall");
    }

    @Test
    void oneFreeRailBetweenTwoHungRailsHolds() {
        line("HFH");
        Verdict<String> verdict = check("r1");
        assertTrue(verdict.holds());
        assertTrue(verdict.drop().isEmpty());
    }

    @Test
    void fourBlockGapBetweenAnchorsDropsAllFour() {
        line("HFFH");
        Verdict<String> verdict = check("r1");
        assertFalse(verdict.holds());
        assertEquals(Set.of("r0", "r1", "r2", "r3"), verdict.drop());
    }

    @Test
    void overhangFromAHungRailFalls() {
        line("HF");
        Verdict<String> verdict = check("r1");
        assertFalse(verdict.holds());
        assertEquals(Set.of("r0", "r1"), verdict.drop());
    }

    @Test
    void loneFreeRailFalls() {
        line("F");
        Verdict<String> verdict = check("r0");
        assertFalse(verdict.holds());
        assertEquals(Set.of("r0"), verdict.drop());
    }

    @Test
    void standingAndHungRailsAlwaysHold() {
        line("SH");
        assertTrue(check("r0").holds());
        assertTrue(check("r1").holds());
    }

    @Test
    void rackRowToElevatorHolds() {
        line("HHHHHFH");
        assertTrue(check("r5").holds());
    }

    @Test
    void branchCountsEveryHungRailIntoTheSpan() {
        line("HFH");
        kinds.put("h2", Kind.HUNG);
        link("r1", "h2");
        Verdict<String> verdict = check("r1");
        assertFalse(verdict.holds());
        assertEquals(Set.of("r0", "r1", "r2", "h2"), verdict.drop());
    }

    @Test
    void twoHoldsAtOneEndCountAsTwo() {
        line("SSFFF");
        kinds.put("s2", Kind.STANDING);
        link("s2", "r2");
        assertTrue(check("r4").holds(), "holds are counted, not their side, so a T end can carry a short overhang");
    }

    @Test
    void walkStopsOnALongFreeRun() {
        line("S" + "F".repeat(100) + "S");
        AtomicInteger visits = new AtomicInteger();
        Verdict<String> verdict = RailSpan.check("r50", kinds::get, rail -> {
            visits.incrementAndGet();
            return links.get(rail);
        }, 3);
        assertFalse(verdict.holds());
        assertTrue(visits.get() <= 4, "walk visited " + visits.get() + " rails");
    }
}
