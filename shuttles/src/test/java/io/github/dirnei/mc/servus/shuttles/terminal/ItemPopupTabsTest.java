package io.github.dirnei.mc.servus.shuttles.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.terminal.ItemPopupTabs.Tab;
import org.junit.jupiter.api.Test;

class ItemPopupTabsTest {

    @Test
    void outOfStockAndNotCraftableGreysRequestAndCraft() {
        ItemPopupTabs tabs = ItemPopupTabs.of(0, false, true);
        assertEquals(ItemPopupTabs.OUT_OF_STOCK, tabs.reason(Tab.REQUEST));
        assertEquals(ItemPopupTabs.NOT_CRAFTABLE, tabs.reason(Tab.CRAFT));
        assertTrue(tabs.enabled(Tab.RULE));
    }

    @Test
    void offlineGreysRule() {
        ItemPopupTabs tabs = ItemPopupTabs.of(10, true, false);
        assertFalse(tabs.enabled(Tab.RULE));
        assertEquals(ItemPopupTabs.NO_WAREHOUSE, tabs.reason(Tab.RULE));
        assertFalse(tabs.enabled(Tab.REQUEST));
    }

    @Test
    void stockedItemEnablesRequest() {
        ItemPopupTabs tabs = ItemPopupTabs.of(5, false, true);
        assertTrue(tabs.enabled(Tab.REQUEST));
        assertNull(tabs.reason(Tab.REQUEST));
        assertFalse(tabs.enabled(Tab.CRAFT));
    }

    @Test
    void craftableItemOutOfStockCanStillBeRequested() {
        ItemPopupTabs tabs = ItemPopupTabs.of(0, true, true);
        assertTrue(tabs.enabled(Tab.REQUEST));
        assertTrue(tabs.enabled(Tab.CRAFT));
    }
}
