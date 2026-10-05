package io.github.dirnei.mc.servus.lib.craft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RoundRobinTest {

    @Test
    void alternatesBetweenEligible() {
        assertEquals(0, RoundRobin.next(3, 0, i -> true));
        assertEquals(1, RoundRobin.next(3, 1, i -> true));
        assertEquals(0, RoundRobin.next(3, 3, i -> true));
    }

    @Test
    void skipsIneligibleAndWraps() {
        assertEquals(2, RoundRobin.next(3, 1, i -> i == 2));
        assertEquals(0, RoundRobin.next(3, 2, i -> i == 0));
        assertEquals(-1, RoundRobin.next(3, 0, i -> false));
        assertEquals(-1, RoundRobin.next(0, 0, i -> true));
    }
}
