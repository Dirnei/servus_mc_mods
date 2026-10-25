package io.github.dirnei.mc.servus.shuttles.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MostStockedTest {

    private static final List<Map.Entry<String, Long>> STOCK = List.of(
            Map.entry("oak_rail", 10L), Map.entry("spruce_rail", 40L), Map.entry("birch_rail", 40L), Map.entry("iron", 99L));

    @Test
    void highestAcceptedCountWins() {
        assertEquals(Optional.of("spruce_rail"), MostStocked.pick(STOCK, Set.of("oak_rail", "spruce_rail")::contains));
    }

    @Test
    void tieKeepsTheFirstInStock() {
        assertEquals(Optional.of("spruce_rail"), MostStocked.pick(STOCK, Set.of("birch_rail", "spruce_rail")::contains));
    }

    @Test
    void noAcceptedKeyInStockGivesEmpty() {
        assertEquals(Optional.empty(), MostStocked.pick(STOCK, Set.of("stick")::contains));
        assertEquals(Optional.empty(), MostStocked.pick(List.of(Map.entry("stick", 0L)), "stick"::equals));
    }

    @Test
    void noCandidatesGiveEmpty() {
        assertEquals(Optional.empty(), MostStocked.pick(STOCK, Set.<String>of()::contains));
    }

    @Test
    void emptyStockGivesEmpty() {
        assertEquals(Optional.empty(), MostStocked.pick(List.<Map.Entry<String, Long>>of(), key -> true));
    }
}
