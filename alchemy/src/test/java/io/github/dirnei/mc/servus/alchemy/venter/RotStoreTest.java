package io.github.dirnei.mc.servus.alchemy.venter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RotStoreTest {

    @Test
    void capacityAndRoom() {
        RotStore<String> store = new RotStore<>(10);
        assertEquals(6, store.add("flesh", 6));
        assertEquals(4, store.room());
        assertEquals(4, store.add("eye", 9));
        assertEquals(0, store.room());
        assertEquals(0, store.add("flesh", 1));
        assertEquals(10, store.total());
    }

    @Test
    void mixedKindsCount() {
        RotStore<String> store = new RotStore<>(64);
        store.add("flesh", 5);
        store.add("eye", 3);
        store.add("flesh", 2);
        assertEquals(10, store.count(kind -> true));
        assertEquals(7, store.count("flesh"::equals));
    }

    @Test
    void takeIsAllOrNothing() {
        RotStore<String> store = new RotStore<>(64);
        store.add("flesh", 5);
        store.add("stone", 9);
        assertFalse(store.take("flesh"::equals, 8));
        assertEquals(14, store.total());
        assertTrue(store.take("flesh"::equals, 5));
        assertEquals(Map.of("stone", 9), store.contents());
    }

    @Test
    void takesOldestKindsFirst() {
        RotStore<String> store = new RotStore<>(64);
        store.add("flesh", 5);
        store.add("eye", 3);
        store.add("leaves", 4);
        assertTrue(store.take(kind -> true, 7));
        Map<String, Integer> left = new LinkedHashMap<>();
        left.put("eye", 1);
        left.put("leaves", 4);
        assertEquals(left, store.contents());
    }

    @Test
    void drainEmptiesInOrder() {
        RotStore<String> store = new RotStore<>(64);
        store.add("eye", 3);
        store.add("flesh", 2);
        Map<String, Integer> drained = store.drain();
        assertEquals("eye", drained.keySet().iterator().next());
        assertEquals(5, drained.values().stream().mapToInt(Integer::intValue).sum());
        assertTrue(store.isEmpty());
        assertEquals(64, store.room());
    }

    @Test
    void smallerCapacityKeepsWhatIsHeld() {
        RotStore<String> store = new RotStore<>(64);
        store.add("flesh", 20);
        store.setCapacity(10);
        assertEquals(20, store.total());
        assertEquals(0, store.room());
    }
}
