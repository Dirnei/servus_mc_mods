package io.github.dirnei.mc.servus.shuttles.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.foundation.search.SearchFacts;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleListingTest {

    private record Row(String name, List<String> tags) implements SearchFacts {

        @Override
        public List<String> names() {
            return List.of(name);
        }
    }

    private static final List<Row> ROWS = List.of(
            new Row("Torch", List.of()),
            new Row("stick", List.of("c:rods", "c:rods/wooden")),
            new Row("Oak Planks", List.of("minecraft:planks")),
            new Row("Redstone Torch", List.of()),
            new Row("Iron Ingot", List.of("c:ingots", "c:ingots/iron")));

    @Test
    void sortedByNameIgnoringCase() {
        assertEquals(List.of(4, 2, 3, 1, 0), RuleListing.shown(ROWS, ""));
    }

    @Test
    void filterKeepsMatchingNames() {
        assertEquals(List.of(3, 0), RuleListing.shown(ROWS, "torch"));
        assertEquals(List.of(3, 0), RuleListing.shown(ROWS, "  TORCH "));
    }

    @Test
    void tagTermFiltersByTag() {
        assertEquals(List.of(4), RuleListing.shown(ROWS, "$ingots"));
        assertEquals(List.of(4, 1), RuleListing.shown(ROWS, "$ingots | $c:rods"));
    }

    @Test
    void emptyQueryKeepsAll() {
        assertEquals(5, RuleListing.shown(ROWS, null).size());
        assertEquals(5, RuleListing.shown(ROWS, "   ").size());
    }

    @Test
    void noMatchShowsNothing() {
        assertEquals(List.of(), RuleListing.shown(ROWS, "glass"));
    }
}
