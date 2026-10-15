package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class FieldRouteTest {

    /** A row 0 to 5 with a branch 6 off rail 2. */
    private static final Map<Integer, List<Integer>> RAILS = Map.of(
            0, List.of(1), 1, List.of(0, 2), 2, List.of(1, 3, 6), 3, List.of(2, 4), 4, List.of(3, 5), 5, List.of(4), 6, List.of(2));
    private static final Function<Integer, Collection<Integer>> NEIGHBOURS = RAILS::get;

    @Test
    void nearestWithWork() {
        assertEquals(Optional.of(4), FieldRoute.next(5, NEIGHBOURS, Set.of(0, 4)::contains));
    }

    @Test
    void startCounts() {
        assertEquals(Optional.of(3), FieldRoute.next(3, NEIGHBOURS, Set.of(3, 4)::contains));
    }

    @Test
    void noneLeft() {
        assertTrue(FieldRoute.next(0, NEIGHBOURS, rail -> false).isEmpty());
    }

    @Test
    void orderWalksAlongTheRow() {
        assertEquals(List.of(0, 3, 5), FieldRoute.order(0, NEIGHBOURS, Set.of(0, 3, 5)::contains));
    }

    @Test
    void orderTakesTheBranchWhenNearest() {
        assertEquals(List.of(1, 6, 4), FieldRoute.order(0, NEIGHBOURS, Set.of(1, 4, 6)::contains));
    }

    @Test
    void unreachableRailsAreLeftOut() {
        Function<Integer, Collection<Integer>> split = rail -> rail < 3 ? RAILS.get(rail).stream().filter(n -> n < 3).toList() : List.of();
        assertEquals(List.of(1), FieldRoute.order(0, split, Set.of(1, 4)::contains));
    }
}
