package io.github.dirnei.mc.servus.shuttles.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VariantChoiceTest {

    private static final List<String> PLANKS = List.of("oak", "spruce", "birch");

    private static List<String> grid(String... cells) {
        return Arrays.asList(Arrays.copyOf(cells, 9));
    }

    private static List<List<String>> candidates(List<String> own, List<String> accepted) {
        return own.stream().map(cell -> cell == null ? List.<String>of() : accepted).toList();
    }

    private static VariantChoice<String> sticks() {
        List<String> own = grid(null, "oak", null, null, "oak");
        return VariantChoice.of(own, candidates(own, PLANKS));
    }

    @Test
    void picksTheMostStockedCandidate() {
        List<String> chosen = sticks().pick(Map.of("oak", 3L, "spruce", 40L, "birch", 7L));
        assertEquals(grid(null, "spruce", null, null, "spruce"), chosen);
    }

    @Test
    void tiesGoToTheOwnItem() {
        List<String> chosen = sticks().pick(Map.of("oak", 10L, "spruce", 10L));
        assertEquals(grid(null, "oak", null, null, "oak"), chosen);
    }

    @Test
    void noStockKeepsTheOwnItem() {
        List<String> chosen = sticks().pick(Map.of("cobblestone", 64L, "spruce", 0L));
        assertEquals(grid(null, "oak", null, null, "oak"), chosen);
    }

    @Test
    void cellsOfOneIngredientShareAVariant() {
        List<String> own = grid("oak", "spruce", null, "oak", "oak");
        List<String> chosen = VariantChoice.of(own, candidates(own, PLANKS)).pick(Map.of("birch", 5L, "spruce", 4L));
        assertEquals(grid("birch", "birch", null, "birch", "birch"), chosen);
    }

    @Test
    void fixedCellsStay() {
        List<String> own = grid("coal", null, null, "oak");
        List<List<String>> accepted = List.of(List.of("coal"), List.of(), List.of(), PLANKS);
        VariantChoice<String> torch = VariantChoice.of(own, accepted);
        assertTrue(torch.varies());
        assertEquals(grid("coal", null, null, "spruce"), torch.pick(Map.of("spruce", 9L, "charcoal", 99L)));
        assertEquals(1, torch.groups().size());
    }

    @Test
    void exactPatternIsUnchanged() {
        List<String> own = grid(null, "oak", null, null, "oak");
        VariantChoice<String> exact = VariantChoice.exact(own);
        assertFalse(exact.varies());
        assertEquals(own, exact.pick(Map.of("spruce", 40L, "birch", 40L)));
    }
}
