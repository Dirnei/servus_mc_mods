package io.github.dirnei.mc.servus.shuttles.terminal;

import java.util.EnumMap;
import java.util.Map;

/**
 * Which tabs of the Storage Terminal's item popup can act for an item, and
 * why the others are greyed out. Pure, no Minecraft imports.
 */
public final class ItemPopupTabs {

    /** The tabs of the item popup, left to right. */
    public enum Tab { REQUEST, CRAFT, RULE }

    /** Nothing of the item is requestable and nothing makes it. */
    public static final String OUT_OF_STOCK = "out_of_stock";
    /** No producer of the line makes the item. */
    public static final String NOT_CRAFTABLE = "not_craftable";
    /** The terminal reaches no warehouse. */
    public static final String NO_WAREHOUSE = "no_warehouse";

    private final Map<Tab, String> reasons = new EnumMap<>(Tab.class);

    private ItemPopupTabs() {
    }

    /**
     * @param requestable how many of the item the warehouse can ship
     * @param craftable   whether a producer of the line makes the item
     * @param warehouse   whether the terminal reaches a warehouse
     */
    public static ItemPopupTabs of(long requestable, boolean craftable, boolean warehouse) {
        ItemPopupTabs tabs = new ItemPopupTabs();
        if (!warehouse) {
            for (Tab tab : Tab.values()) {
                tabs.reasons.put(tab, NO_WAREHOUSE);
            }
            return tabs;
        }
        if (requestable <= 0 && !craftable) {
            tabs.reasons.put(Tab.REQUEST, OUT_OF_STOCK);
        }
        if (!craftable) {
            tabs.reasons.put(Tab.CRAFT, NOT_CRAFTABLE);
        }
        return tabs;
    }

    public boolean enabled(Tab tab) {
        return !reasons.containsKey(tab);
    }

    /** The reason key a greyed out tab shows, null for an enabled one. */
    public String reason(Tab tab) {
        return reasons.get(tab);
    }
}
