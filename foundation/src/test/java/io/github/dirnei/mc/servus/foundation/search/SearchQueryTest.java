package io.github.dirnei.mc.servus.foundation.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class SearchQueryTest {

    private record Fake(String name, String id, List<String> mods, List<String> tags, List<String> tooltip, List<String> tabs)
            implements SearchFacts {

        @Override
        public List<String> names() {
            return List.of(name);
        }

        @Override
        public List<String> ids() {
            return List.of(id);
        }
    }

    private static final Fake IRON_INGOT = new Fake("Iron Ingot", "minecraft:iron_ingot", List.of("minecraft", "Minecraft"),
            List.of("c:ingots", "c:ingots/iron", "minecraft:beacon_payment_items"), List.of(), List.of("Ingredients"));
    private static final Fake IRON_ORE = new Fake("Iron Ore", "minecraft:iron_ore", List.of("minecraft", "Minecraft"),
            List.of("c:ores", "c:ores/iron"), List.of(), List.of("Natural Blocks"));
    private static final Fake TIN_INGOT = new Fake("Tin Ingot", "othermod:tin_ingot", List.of("othermod", "Other Mod"),
            List.of("c:ingots", "c:ingots/tin"), List.of(), List.of("Other Mod"));
    private static final Fake IRON_NUGGET = new Fake("Iron Nugget", "minecraft:iron_nugget", List.of("minecraft", "Minecraft"),
            List.of("c:nuggets", "c:nuggets/iron"), List.of(), List.of("Ingredients"));
    private static final Fake TANK = new Fake("Fluid Tank", "servus_foundation:fluid_tank", List.of("servus_foundation", "Servus: Foundation"),
            List.of(), List.of("Floor: Stone Bricks", "Holds 16 buckets"), List.of("Servus: Foundation"));
    private static final Fake OAK_LOG = new Fake("Oak Log", "minecraft:oak_log", List.of("minecraft", "Minecraft"),
            List.of("minecraft:logs"), List.of(), List.of("Building Blocks"));
    private static final Fake STRIPPED = new Fake("Bark Free Wood", "minecraft:oak_wood", List.of("minecraft", "Minecraft"),
            List.of("minecraft:logs"), List.of(), List.of("Building Blocks", "Redstone Blocks"));
    private static final List<Fake> ALL = List.of(IRON_INGOT, IRON_ORE, TIN_INGOT, IRON_NUGGET, TANK, OAK_LOG, STRIPPED);

    private static List<Fake> search(String text) {
        Predicate<Fake> matcher = SearchQuery.parse(text)::matches;
        return ALL.stream().filter(matcher).toList();
    }

    @Test
    void plainWordMatchesNames() {
        assertEquals(List.of(IRON_INGOT, IRON_ORE, IRON_NUGGET), search("iron"));
    }

    @Test
    void caseIsIgnored() {
        assertEquals(List.of(IRON_INGOT, IRON_ORE, IRON_NUGGET), search("IRON"));
        assertEquals(List.of(TANK), search("@SERVUS"));
    }

    @Test
    void modMatchesIdAndName() {
        assertEquals(List.of(TANK), search("@servus"));
        assertEquals(List.of(TANK), search("@foundation"));
        assertEquals(List.of(TIN_INGOT), search("@\"other mod\""));
    }

    @Test
    void tagMatchesShortAndFullIds() {
        assertEquals(List.of(IRON_INGOT, TIN_INGOT), search("$ingots"));
        assertEquals(List.of(IRON_INGOT, TIN_INGOT), search("$c:ingots"));
    }

    @Test
    void tooltipTabAndId() {
        assertEquals(List.of(TANK), search("#\"stone bricks\""));
        assertEquals(List.of(STRIPPED), search("%redstone"));
        assertEquals(List.of(OAK_LOG, STRIPPED), search("&minecraft:oak_"));
    }

    @Test
    void spacesNeedAllTerms() {
        assertEquals(List.of(IRON_INGOT), search("@minecraft $ingots"));
    }

    @Test
    void pipeNeedsEitherSide() {
        assertEquals(List.of(IRON_INGOT, TIN_INGOT, IRON_NUGGET), search("$ingots | $nuggets"));
        assertEquals(List.of(IRON_INGOT, TIN_INGOT, IRON_NUGGET), search("$ingots|$nuggets"));
    }

    @Test
    void minusNegates() {
        assertEquals(List.of(TIN_INGOT), search("$ingots -@minecraft"));
        assertEquals(List.of(IRON_ORE, IRON_NUGGET), search("iron -ingot"));
    }

    @Test
    void quotesKeepSpaces() {
        assertEquals(List.of(IRON_INGOT), search("\"iron ingot\""));
        assertEquals(List.of(), search("\"ingot iron\""));
    }

    @Test
    void prefixFollowedByQuote() {
        assertEquals(List.of(TANK), search("@\"servus: foundation\""));
    }

    @Test
    void unclosedQuoteRunsToTheEnd() {
        assertEquals(List.of(TANK), search("#\"holds 16"));
        SearchQuery query = SearchQuery.parse("\"iron in");
        assertEquals(List.of(List.of(new SearchQuery.Term(SearchQuery.Kind.NAME, "iron in", false))), query.alternatives());
    }

    @Test
    void lonePrefixOrMinusIsIgnored() {
        assertEquals(search("iron"), search("iron @"));
        assertEquals(search("iron"), search("iron -"));
        assertEquals(search("iron"), search("iron -@ |"));
        assertTrue(SearchQuery.parse("@").isEmpty());
        assertTrue(SearchQuery.parse("- $ |").isEmpty());
    }

    @Test
    void emptyAndBlankMatchEverything() {
        assertTrue(SearchQuery.parse("").isEmpty());
        assertTrue(SearchQuery.parse("   ").isEmpty());
        assertTrue(SearchQuery.parse(null).isEmpty());
        assertEquals(ALL, search(""));
        assertEquals(ALL, search("  "));
        assertFalse(SearchQuery.parse("iron").isEmpty());
    }

    @Test
    void quotedPrefixIsText() {
        SearchQuery query = SearchQuery.parse("\"@iron\"");
        assertEquals(List.of(List.of(new SearchQuery.Term(SearchQuery.Kind.NAME, "@iron", false))), query.alternatives());
    }

    @Test
    void tooltipNotAskedWithoutHash() {
        int[] calls = new int[2];
        SearchFacts counting = new SearchFacts() {
            @Override
            public List<String> names() {
                return List.of("Iron Ingot");
            }

            @Override
            public List<String> mods() {
                return List.of("minecraft");
            }

            @Override
            public List<String> tooltip() {
                calls[0]++;
                return List.of();
            }

            @Override
            public List<String> tabs() {
                calls[1]++;
                return List.of();
            }
        };
        assertTrue(SearchQuery.parse("@minecraft iron").matches(counting));
        assertEquals(0, calls[0]);
        assertEquals(0, calls[1]);
        SearchQuery.parse("#iron").matches(counting);
        assertEquals(1, calls[0]);
    }

    @Test
    void extraNamesAndTooltipLines() {
        SearchFacts withLabel = IRON_INGOT.withNames(List.of("Smoker Line (#1)")).withTooltip(List.of("Smoker Line (#1)"));
        assertTrue(SearchQuery.parse("smoker").matches(withLabel));
        assertTrue(SearchQuery.parse("#1").matches(withLabel));
        assertTrue(SearchQuery.parse("$ingots").matches(withLabel));
        assertFalse(SearchQuery.parse("#1").matches(IRON_INGOT));
    }
}
