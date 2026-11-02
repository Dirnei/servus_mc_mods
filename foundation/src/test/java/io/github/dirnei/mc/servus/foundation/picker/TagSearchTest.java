package io.github.dirnei.mc.servus.foundation.picker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TagSearchTest {

    private static final List<String> TAGS = List.of(
            "minecraft:beacon_payment_items", "c:ingots/iron", "mypack:fuel", "c:ingots", "c:ores", "c:raw_materials");

    @Test
    void pathPrefixComesBeforeContains() {
        assertEquals(List.of("c:ingots", "c:ingots/iron"), TagSearch.search(TAGS, "$ing"));
        assertEquals(List.of("c:ingots", "c:ingots/iron", "minecraft:beacon_payment_items"), TagSearch.search(TAGS, "$in"));
    }

    @Test
    void noMatchIsLeftOut() {
        assertEquals(List.of(), TagSearch.search(TAGS, "$melon"));
    }

    @Test
    void matchesAnywhereInTheIdIgnoringCase() {
        assertEquals(List.of("mypack:fuel"), TagSearch.search(TAGS, "$FUEL"));
        assertEquals(List.of("mypack:fuel"), TagSearch.search(TAGS, "$mypack"));
    }

    @Test
    void topHitIsTheFirstMatch() {
        assertEquals(Optional.of("c:ingots"), TagSearch.topHit(TAGS, "$ingot"));
        assertEquals(Optional.empty(), TagSearch.topHit(TAGS, "$melon"));
    }

    @Test
    void emptySearchAfterDollarListsAllInIdOrder() {
        assertEquals(List.of("c:ingots", "c:ingots/iron", "c:ores", "c:raw_materials", "minecraft:beacon_payment_items", "mypack:fuel"),
                TagSearch.search(TAGS, "$"));
    }

    @Test
    void tagQueryNeedsTheDollar() {
        assertTrue(TagSearch.isTagQuery("$c:ores"));
        assertFalse(TagSearch.isTagQuery("#c:ores"));
        assertFalse(TagSearch.isTagQuery("ores"));
        assertFalse(TagSearch.isTagQuery(null));
    }

    @Test
    void tagIdsKeepTheHash() {
        assertTrue(TagSearch.isTagId("#c:ores"));
        assertFalse(TagSearch.isTagId("$c:ores"));
        assertFalse(TagSearch.isTagId("minecraft:iron_ore"));
        assertFalse(TagSearch.isTagId(null));
    }
}
