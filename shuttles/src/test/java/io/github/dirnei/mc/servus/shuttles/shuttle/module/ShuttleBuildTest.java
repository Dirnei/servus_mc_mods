package io.github.dirnei.mc.servus.shuttles.shuttle.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleTiers.Supply;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShuttleBuildTest {

    private static final List<Supply> SHEET = List.of(new Supply("gold", 16), new Supply("chest", 2));

    @Test
    void completeSheetIsReady() {
        assertTrue(ShuttleBuild.ready(SHEET, Map.of("gold", 16, "chest", 2)));
        assertTrue(ShuttleBuild.missing(SHEET, Map.of("gold", 16, "chest", 2)).isEmpty());
    }

    @Test
    void missingCountsInSheetOrder() {
        Map<String, Integer> missing = ShuttleBuild.missing(SHEET, Map.of("gold", 10));
        assertEquals(List.of("gold", "chest"), List.copyOf(missing.keySet()));
        assertEquals(6, missing.get("gold"));
        assertEquals(2, missing.get("chest"));
        assertFalse(ShuttleBuild.ready(SHEET, Map.of("gold", 10)));
    }

    @Test
    void extraSuppliesDoNotCount() {
        assertTrue(ShuttleBuild.ready(SHEET, Map.of("gold", 64, "chest", 5, "dirt", 3)));
        assertEquals(Map.of("gold", 16, "chest", 2), ShuttleBuild.needs(SHEET));
    }

    @Test
    void repeatedIdsAddUp() {
        assertEquals(Map.of("gold", 20), ShuttleBuild.needs(List.of(new Supply("gold", 16), new Supply("gold", 4))));
    }

    @Test
    void topTierHasNoSheet() {
        assertFalse(ShuttleBuild.ready(ShuttleTiers.DEFAULT.nextBuild(3), Map.of()));
    }
}
