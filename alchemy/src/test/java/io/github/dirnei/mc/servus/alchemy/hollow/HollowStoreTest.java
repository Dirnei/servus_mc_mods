package io.github.dirnei.mc.servus.alchemy.hollow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HollowStoreTest {

    @Test
    void startsEmptyWithRoom() {
        HollowStore store = new HollowStore(64);
        assertTrue(store.isEmpty());
        assertTrue(store.hasRoom());
        assertEquals(0, store.count());
    }

    @Test
    void fillsToCapacity() {
        HollowStore store = new HollowStore(3);
        assertTrue(store.add());
        assertTrue(store.add());
        assertTrue(store.add());
        assertFalse(store.hasRoom());
        assertFalse(store.add());
        assertEquals(3, store.count());
    }

    @Test
    void takeAllEmpties() {
        HollowStore store = new HollowStore(64);
        store.add();
        store.add();
        assertEquals(2, store.takeAll());
        assertTrue(store.isEmpty());
        assertEquals(0, store.takeAll());
    }

    @Test
    void smallerCapacityKeepsCountButHasNoRoom() {
        HollowStore store = new HollowStore(64);
        store.load(10);
        store.setCapacity(5);
        assertEquals(10, store.count());
        assertFalse(store.hasRoom());
        assertFalse(store.add());
    }

    @Test
    void negativeLoadIsEmpty() {
        HollowStore store = new HollowStore(64);
        store.load(-4);
        assertTrue(store.isEmpty());
    }

    @Test
    void takePart() {
        HollowStore store = new HollowStore(64);
        store.load(5);
        assertEquals(3, store.take(3));
        assertEquals(2, store.count());
        assertEquals(2, store.take(10));
        assertTrue(store.isEmpty());
        assertEquals(0, store.take(4));
        assertEquals(0, store.take(-1));
    }
}
