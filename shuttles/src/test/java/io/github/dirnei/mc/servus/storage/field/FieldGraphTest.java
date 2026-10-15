package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FieldGraphTest {

    /** Two runs: a-b-c and d-e, not linked. */
    private static final Map<String, List<String>> LINKS = Map.of(
            "a", List.of("b"), "b", List.of("a", "c"), "c", List.of("b"),
            "d", List.of("e"), "e", List.of("d"));

    private static Collection<String> links(String node) {
        return LINKS.getOrDefault(node, List.of());
    }

    @Test
    void collectsOnlyConnectedRails() {
        FieldGraph.Result<String> result = FieldGraph.collect("b", FieldGraphTest::links, 10);
        assertEquals(Set.of("a", "b", "c"), result.members());
        assertFalse(result.tooLarge());
    }

    @Test
    void singleRail() {
        FieldGraph.Result<String> result = FieldGraph.collect("x", FieldGraphTest::links, 10);
        assertEquals(Set.of("x"), result.members());
        assertFalse(result.tooLarge());
    }

    @Test
    void exactlyMaxIsFine() {
        assertFalse(FieldGraph.collect("a", FieldGraphTest::links, 3).tooLarge());
    }

    @Test
    void stopsPastMax() {
        FieldGraph.Result<String> result = FieldGraph.collect("a", FieldGraphTest::links, 2);
        assertTrue(result.tooLarge());
    }
}
