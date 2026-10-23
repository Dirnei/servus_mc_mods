package io.github.dirnei.mc.servus.shuttles.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OrphanSweepTest {

    @Test
    void knownClaimsStay() {
        OrphanSweep<Integer> sweep = new OrphanSweep<>();
        assertEquals(List.of(), sweep.sweep(List.of(1, 2), id -> true));
        assertEquals(List.of(), sweep.sweep(List.of(1, 2), id -> true));
    }

    @Test
    void unknownTwiceIsReleased() {
        OrphanSweep<Integer> sweep = new OrphanSweep<>();
        assertEquals(List.of(), sweep.sweep(List.of(1, 2), id -> id == 1));
        assertEquals(List.of(2), sweep.sweep(List.of(1, 2), id -> id == 1));
    }

    @Test
    void knownAgainResets() {
        OrphanSweep<Integer> sweep = new OrphanSweep<>();
        Set<Integer> known = new HashSet<>();
        sweep.sweep(List.of(3), known::contains);
        known.add(3);
        assertEquals(List.of(), sweep.sweep(List.of(3), known::contains));
        known.clear();
        assertEquals(List.of(), sweep.sweep(List.of(3), known::contains));
        assertEquals(List.of(3), sweep.sweep(List.of(3), known::contains));
    }
}
