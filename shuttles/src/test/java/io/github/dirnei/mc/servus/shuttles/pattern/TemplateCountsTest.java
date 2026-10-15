package io.github.dirnei.mc.servus.shuttles.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateCountsTest {

    @Test
    void countsPerKey() {
        List<String> torch = Arrays.asList("coal", null, null, "stick", null, null, null, null, null);
        assertEquals(Map.of("coal", 1, "stick", 1), TemplateCounts.counts(torch));
        List<String> chest = Arrays.asList("plank", "plank", "plank", "plank", null, "plank", "plank", "plank", "plank");
        assertEquals(Map.of("plank", 8), TemplateCounts.counts(chest));
    }

    @Test
    void emptyCellsIgnored() {
        List<String> empty = Arrays.asList(null, null, null, null, null, null, null, null, null);
        assertTrue(TemplateCounts.counts(empty).isEmpty());
    }

    @Test
    void sameCountsInAnyLayout() {
        List<String> left = Arrays.asList("plank", null, null, "plank", null, null, null, null, null);
        List<String> right = Arrays.asList(null, null, "plank", null, null, "plank", null, null, null);
        assertTrue(TemplateCounts.same(left, right));
    }

    @Test
    void differentItemsDiffer() {
        List<String> oak = Arrays.asList("oak", null, null, "oak", null, null, null, null, null);
        List<String> spruce = Arrays.asList("spruce", null, null, "spruce", null, null, null, null, null);
        assertFalse(TemplateCounts.same(oak, spruce));
        List<String> three = Arrays.asList("oak", "oak", null, "oak", null, null, null, null, null);
        assertFalse(TemplateCounts.same(oak, three));
    }
}
