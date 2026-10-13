package io.github.dirnei.mc.servus.foundation.machine.anchor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TeamLoadTest {

    private static Set<Long> square(int centreX, int centreZ, int radius) {
        return Set.copyOf(AnchorArea.of(radius, 0, 0).chunks(centreX, centreZ));
    }

    @Test
    void overlapCountedOnce() {
        Map<String, Set<Long>> entries = new HashMap<>();
        entries.put("a", square(0, 0, 1));
        entries.put("b", square(1, 0, 1));
        assertEquals(12, TeamLoad.load(entries));
    }

    @Test
    void ownEntryIgnoredOnRecheck() {
        Map<String, Set<Long>> entries = new HashMap<>();
        entries.put("a", square(0, 0, 1));
        entries.put("b", square(10, 10, 2));
        // a grows from 9 to 25: checked against the 25 of b alone, 50 in total
        assertEquals(50, TeamLoad.loadWith(entries, "a", square(0, 0, 2)));
        assertTrue(TeamLoad.fits(entries, "a", square(0, 0, 2), 50));
        assertFalse(TeamLoad.fits(entries, "a", square(0, 0, 2), 49));
    }

    @Test
    void exactlyAtTheCapFits() {
        Map<String, Set<Long>> entries = new HashMap<>();
        entries.put("a", square(0, 0, 2));
        entries.put("b", square(100, 0, 1));
        assertTrue(TeamLoad.fits(entries, "c", square(-100, 0, 1), 43));
    }

    @Test
    void oneOverTheCapRefused() {
        Map<String, Set<Long>> entries = new HashMap<>();
        entries.put("a", square(0, 0, 2));
        entries.put("b", square(100, 0, 1));
        assertFalse(TeamLoad.fits(entries, "c", square(-100, 0, 1), 42));
        assertFalse(TeamLoad.fits(entries, "c", List.of(AnchorArea.pack(50, 50)), 34));
    }

    @Test
    void emptyTeamHasNoLoad() {
        assertEquals(0, TeamLoad.load(Map.<String, Set<Long>>of()));
        assertTrue(TeamLoad.fits(Map.<String, Set<Long>>of(), "a", square(0, 0, 2), 25));
    }
}
