package io.github.dirnei.mc.servus.storage.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TerminalStoreTest {

    @Test
    void openShowsCached() {
        TerminalStore<String, String> store = new TerminalStore<>();
        store.update("a", "stock 1");
        store.open("a");
        assertEquals("stock 1", store.shown());
    }

    @Test
    void updateWhileClosedShownOnOpen() {
        TerminalStore<String, String> store = new TerminalStore<>();
        store.open("a");
        store.close();
        store.update("a", "stock 2");
        assertNull(store.shown());
        store.open("a");
        assertEquals("stock 2", store.shown());
    }

    @Test
    void roundTripShowsNewest() {
        TerminalStore<String, String> store = new TerminalStore<>();
        store.open("a");
        store.update("a", "before");
        // Another screen in front: removed() closes, the screen's init() opens again.
        store.close();
        store.update("a", "after the pattern was saved");
        store.open("a");
        assertEquals("after the pattern was saved", store.shown());
        store.update("a", "live");
        assertEquals("live", store.shown());
    }

    @Test
    void otherTerminalNotShown() {
        TerminalStore<String, String> store = new TerminalStore<>();
        store.open("a");
        store.update("b", "other");
        assertNull(store.shown());
    }

    @Test
    void forgetClears() {
        TerminalStore<String, String> store = new TerminalStore<>();
        store.update("a", "stock");
        store.forgetAll();
        store.open("a");
        assertNull(store.shown());
    }
}
