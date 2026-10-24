package io.github.dirnei.mc.servus.shuttles.packing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class IngressRankingTest {

    private static final int NONE = PackingStationChoice.UNREACHABLE;

    @Test
    void cheapestTripFirst() {
        assertEquals(List.of("b", "a"), IngressRanking.order(List.of(
                new IngressRanking.Candidate<>("a", 12, 1), new IngressRanking.Candidate<>("b", 2, 2))));
    }

    @Test
    void unreachableLast() {
        assertEquals(List.of("b", "a"), IngressRanking.order(List.of(
                new IngressRanking.Candidate<>("a", NONE, 1), new IngressRanking.Candidate<>("b", 40, 2))));
    }

    @Test
    void tieByLowestPosition() {
        assertEquals(List.of("b", "a"), IngressRanking.order(List.of(
                new IngressRanking.Candidate<>("a", 5, 9), new IngressRanking.Candidate<>("b", 5, 3))));
    }

    @Test
    void emptyStaysEmpty() {
        assertEquals(List.of(), IngressRanking.<String>order(List.of()));
    }
}
