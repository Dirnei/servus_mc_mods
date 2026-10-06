package io.github.dirnei.mc.servus.storage.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.storage.zone.PutAwayPlan.Filter;
import io.github.dirnei.mc.servus.storage.zone.PutAwayPlan.Match;
import io.github.dirnei.mc.servus.storage.zone.PutAwayPlan.Tiers;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class PutAwayPlanTest {

    private static final Function<String, Set<String>> TAGS = item -> switch (item) {
        case "raw_iron", "raw_copper" -> Set.of("c:ores", "c:raw_materials");
        case "bread" -> Set.of("c:foods");
        default -> Set.of();
    };

    private static Filter<String> items(String... items) {
        return new Filter<>(Set.of(items), Set.of());
    }

    private static Filter<String> tags(String... tags) {
        return new Filter<>(Set.of(), Set.of(tags));
    }

    private static final Filter<String> EMPTY = new Filter<>(Set.of(), Set.of());

    @Test
    void exactMatchGoesToTheFirstTier() {
        Tiers<String> tiers = PutAwayPlan.tiers("raw_iron", Map.of("a", items("raw_iron")), TAGS);
        assertEquals(Set.of("a"), tiers.exact());
        assertTrue(tiers.byTag().isEmpty());
        assertTrue(tiers.matched());
    }

    @Test
    void tagMatchGoesToTheSecondTier() {
        Tiers<String> tiers = PutAwayPlan.tiers("raw_copper", Map.of("a", tags("c:ores")), TAGS);
        assertTrue(tiers.exact().isEmpty());
        assertEquals(Set.of("a"), tiers.byTag());
    }

    @Test
    void bothKindsAreSplitKeepingOrder() {
        Map<String, Filter<String>> filtered = new LinkedHashMap<>();
        filtered.put("tagged", tags("c:ores"));
        filtered.put("exact2", items("raw_iron", "bread"));
        filtered.put("exact1", items("raw_iron"));
        filtered.put("other", items("bread"));
        Tiers<String> tiers = PutAwayPlan.tiers("raw_iron", filtered, TAGS);
        assertEquals(List.of("exact2", "exact1"), List.copyOf(tiers.exact()));
        assertEquals(List.of("tagged"), List.copyOf(tiers.byTag()));
    }

    @Test
    void noMatchGivesEmptyTiers() {
        Tiers<String> tiers = PutAwayPlan.tiers("bread", Map.of("a", tags("c:ores"), "b", items("raw_iron")), TAGS);
        assertFalse(tiers.matched());
    }

    @Test
    void emptyFilterNeverMatches() {
        Tiers<String> tiers = PutAwayPlan.tiers("bread", Map.of("a", EMPTY), TAGS);
        assertFalse(tiers.matched());
        assertEquals(Match.NONE, EMPTY.match("bread", Set.of("c:foods")));
        assertTrue(EMPTY.isEmpty());
    }

    @Test
    void exactWinsOverTagInsideOneFilter() {
        Filter<String> both = new Filter<>(Set.of("raw_iron"), Set.of("c:ores"));
        assertEquals(Match.EXACT, both.match("raw_iron", TAGS.apply("raw_iron")));
        assertEquals(Match.TAG, both.match("raw_copper", TAGS.apply("raw_copper")));
        assertEquals(Match.NONE, both.match("bread", TAGS.apply("bread")));
    }
}
