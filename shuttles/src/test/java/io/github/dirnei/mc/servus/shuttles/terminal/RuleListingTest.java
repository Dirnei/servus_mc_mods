package io.github.dirnei.mc.servus.shuttles.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class RuleListingTest {

    private static final List<String> NAMES = List.of("Torch", "stick", "Oak Planks", "Redstone Torch");

    @Test
    void sortedByNameIgnoringCase() {
        assertEquals(List.of(2, 3, 1, 0), RuleListing.shown(NAMES, ""));
    }

    @Test
    void filterKeepsMatchingNames() {
        assertEquals(List.of(3, 0), RuleListing.shown(NAMES, "torch"));
        assertEquals(List.of(3, 0), RuleListing.shown(NAMES, "  TORCH "));
    }

    @Test
    void emptyQueryKeepsAll() {
        assertEquals(4, RuleListing.shown(NAMES, null).size());
        assertEquals(4, RuleListing.shown(NAMES, "   ").size());
    }

    @Test
    void noMatchShowsNothing() {
        assertEquals(List.of(), RuleListing.shown(NAMES, "glass"));
    }
}
