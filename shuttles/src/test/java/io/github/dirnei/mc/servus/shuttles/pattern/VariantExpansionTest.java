package io.github.dirnei.mc.servus.shuttles.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VariantExpansionTest {

    private static final List<String> PLANKS = List.of("oak", "spruce", "birch");

    @Test
    void oneGroupGivesOneVariantPerCandidate() {
        List<List<String>> variants = VariantExpansion.expand(List.of(PLANKS), List.of(true), Map.of(), VariantExpansion.MAX_VARIANTS);
        assertEquals(List.of(List.of("oak"), List.of("spruce"), List.of("birch")), variants);
    }

    @Test
    void stockedVariantsFirst() {
        List<List<String>> variants = VariantExpansion.expand(List.of(PLANKS), List.of(true), Map.of("birch", 40, "spruce", 5),
                VariantExpansion.MAX_VARIANTS);
        assertEquals(List.of(List.of("birch"), List.of("spruce"), List.of("oak")), variants);
    }

    @Test
    void capKeepsTheMostStocked() {
        List<List<String>> variants = VariantExpansion.expand(List.of(List.of("oak", "a", "b", "c", "d")), List.of(true),
                Map.of("c", 9, "d", 7, "b", 3), 3);
        assertEquals(List.of(List.of("c"), List.of("d"), List.of("oak")), variants);
    }

    @Test
    void ownVariantAlwaysIncluded() {
        List<List<String>> variants = VariantExpansion.expand(List.of(PLANKS), List.of(true), Map.of("birch", 40, "spruce", 5), 1);
        assertEquals(List.of(List.of("oak")), variants, "with room for one the own variant stays");
        List<List<String>> two = VariantExpansion.expand(List.of(PLANKS), List.of(true), Map.of("birch", 40, "spruce", 5), 2);
        assertEquals(List.of(List.of("birch"), List.of("oak")), two);
    }

    @Test
    void nonMaterialGroupsArePicked() {
        List<List<String>> variants = VariantExpansion.expand(List.of(List.of("coal", "charcoal"), PLANKS), List.of(false, true),
                Map.of("charcoal", 10), VariantExpansion.MAX_VARIANTS);
        assertEquals(3, variants.size(), "only the material group multiplies");
        assertTrue(variants.stream().allMatch(variant -> variant.get(0).equals("charcoal")), "the stocked fuel is picked for every variant");
    }

    @Test
    void exactPatternGivesItself() {
        assertEquals(List.of(List.of()), VariantExpansion.expand(List.<List<String>>of(), List.of(), Map.of("oak", 3), 32));
    }
}
