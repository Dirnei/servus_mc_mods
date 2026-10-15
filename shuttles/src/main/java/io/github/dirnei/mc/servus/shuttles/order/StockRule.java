package io.github.dirnei.mc.servus.shuttles.order;

import io.github.dirnei.mc.servus.shuttles.network.ItemKey;

/**
 * Keep between min and max of an item.
 *
 * @param key the item
 * @param min reorder when the count plus open orders drops below this
 * @param max order up to this
 */
public record StockRule(ItemKey key, int min, int max) {

    public StockRule {
        min = Math.max(0, min);
        max = Math.max(min, max);
    }
}
