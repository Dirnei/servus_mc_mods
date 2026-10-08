package io.github.dirnei.mc.servus.foundation.craft;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IngredientGathererTest {

    @Test
    void takesFromSeveralSlots() {
        List<String> keys = Arrays.asList("iron", null, "iron", "copper");
        int[] counts = {1, 0, 5, 3};

        Optional<int[]> take = IngredientGatherer.gather(keys, counts, Map.of("iron", 3, "copper", 2));

        assertTrue(take.isPresent());
        assertArrayEquals(new int[] {1, 0, 2, 2}, take.get());
    }

    @Test
    void failsWhenShort() {
        List<String> keys = Arrays.asList("iron", "copper");
        int[] counts = {2, 1};

        assertTrue(IngredientGatherer.gather(keys, counts, Map.of("iron", 3)).isEmpty());
        assertTrue(IngredientGatherer.gather(keys, counts, Map.of("gold", 1)).isEmpty());
    }

    @Test
    void nothingRequiredTakesNothing() {
        List<String> keys = List.of("iron");
        int[] counts = {4};

        Optional<int[]> take = IngredientGatherer.gather(keys, counts, Map.of("iron", 0));

        assertTrue(take.isPresent());
        assertArrayEquals(new int[] {0}, take.get());
    }
}
