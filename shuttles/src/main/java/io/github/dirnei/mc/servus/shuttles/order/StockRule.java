package io.github.dirnei.mc.servus.shuttles.order;

import io.github.dirnei.mc.servus.shuttles.network.ItemKey;

/**
 * The stock rule of one item: a Make pair that orders when stock runs low, a
 * cap that Inbound Stations refuse above, or both.
 *
 * @param key  the item
 * @param make the Make pair, null when the rule orders nothing
 * @param cap  refuse at Inbound Stations above this, 0 for no cap
 */
public record StockRule(ItemKey key, Make make, int cap) {

    /**
     * Order when the count plus open orders drops below {@code min}, up to {@code max}.
     *
     * @param min reorder below this
     * @param max order up to this
     */
    public record Make(int min, int max) {

        public Make {
            min = Math.max(0, min);
            max = Math.max(min, max);
        }
    }

    public StockRule {
        cap = Math.max(0, cap);
    }

    /** A rule with only a Make pair. */
    public static StockRule make(ItemKey key, int min, int max) {
        return new StockRule(key, new Make(min, max), 0);
    }

    public boolean hasMake() {
        return make != null;
    }

    public boolean hasCap() {
        return cap > 0;
    }

    public StockRule withMake(Make next) {
        return new StockRule(key, next, cap);
    }

    public StockRule withCap(int next) {
        return new StockRule(key, make, next);
    }

    /** Whether the rule still holds a threshold; one without is removed. */
    public boolean empty() {
        return !hasMake() && !hasCap();
    }
}
