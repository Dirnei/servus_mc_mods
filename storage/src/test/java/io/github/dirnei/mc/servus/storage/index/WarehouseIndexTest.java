package io.github.dirnei.mc.servus.storage.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WarehouseIndexTest {

    @Test
    void totalsSumAcrossSources() {
        WarehouseIndex<String, String> index = new WarehouseIndex<>();
        index.update("a", Map.of("cobble", 50));
        index.update("b", Map.of("cobble", 30, "bread", 4));
        assertEquals(80, index.count("cobble"));
        assertEquals(4, index.count("bread"));
        assertEquals(84, index.totalItems());
        assertEquals(2, index.typeCount());
    }

    @Test
    void updateReplacesPreviousSnapshot() {
        WarehouseIndex<String, String> index = new WarehouseIndex<>();
        index.update("a", Map.of("cobble", 50));
        index.update("a", Map.of("cobble", 10, "iron", 2));
        assertEquals(10, index.count("cobble"));
        assertEquals(2, index.count("iron"));
    }

    @Test
    void removeDropsContributionAndHolders() {
        WarehouseIndex<String, String> index = new WarehouseIndex<>();
        index.update("a", Map.of("cobble", 50));
        index.update("b", Map.of("cobble", 30));
        index.remove("a");
        assertEquals(30, index.count("cobble"));
        assertEquals(List.of("b"), index.sourcesHolding("cobble"));
        index.update("b", Map.of());
        assertEquals(0, index.count("cobble"));
        assertTrue(index.sourcesHolding("cobble").isEmpty());
        assertEquals(0, index.typeCount());
    }

    @Test
    void entriesAreSortedLargestFirst() {
        WarehouseIndex<String, String> index = new WarehouseIndex<>();
        index.update("a", Map.of("bread", 4, "cobble", 50, "iron", 9));
        List<Map.Entry<String, Long>> entries = index.entries();
        assertEquals("cobble", entries.get(0).getKey());
        assertEquals("iron", entries.get(1).getKey());
        assertEquals("bread", entries.get(2).getKey());
    }

    @Test
    void zeroCountsAreIgnored() {
        WarehouseIndex<String, String> index = new WarehouseIndex<>();
        index.update("a", Map.of("cobble", 0));
        assertEquals(0, index.typeCount());
        assertTrue(index.isEmptySource("a"));
    }

    @Test
    void unchangedSnapshotKeepsVersion() {
        WarehouseIndex<String, String> index = new WarehouseIndex<>();
        index.update("a", Map.of("cobble", 50));
        long version = index.version();
        index.update("a", Map.of("cobble", 50));
        assertEquals(version, index.version());
    }

    @Test
    void changeBumpsVersion() {
        WarehouseIndex<String, String> index = new WarehouseIndex<>();
        index.update("a", Map.of("cobble", 50));
        long version = index.version();
        index.update("a", Map.of("cobble", 49));
        assertTrue(index.version() != version);
        assertEquals(49, index.count("cobble"));
    }

    @Test
    void removeBumpsVersion() {
        WarehouseIndex<String, String> index = new WarehouseIndex<>();
        index.update("a", Map.of("cobble", 50));
        long version = index.version();
        index.remove("a");
        assertTrue(index.version() != version);
        long after = index.version();
        index.remove("a");
        assertEquals(after, index.version());
    }

    @Test
    void entriesCachedPerVersion() {
        WarehouseIndex<String, String> index = new WarehouseIndex<>();
        index.update("a", Map.of("cobble", 50, "bread", 4));
        List<Map.Entry<String, Long>> first = index.entries();
        index.update("a", Map.of("cobble", 50, "bread", 4));
        assertTrue(first == index.entries());
        index.update("b", Map.of("bread", 100));
        List<Map.Entry<String, Long>> second = index.entries();
        assertTrue(first != second);
        assertEquals("bread", second.get(0).getKey());
        assertEquals(50L, first.get(0).getValue());
    }
}
