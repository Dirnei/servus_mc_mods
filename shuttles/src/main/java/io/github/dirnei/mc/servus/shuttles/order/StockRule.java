package io.github.dirnei.mc.servus.shuttles.order;

import io.github.dirnei.mc.servus.shuttles.network.ItemKey;

/**
 * The stock rule of one item: a Make pair that orders when stock runs low, a
 * cap that Inbound Stations refuse above, an export threshold whose surplus
 * goes to an Outbound Station, or any of them together.
 *
 * @param key    the item
 * @param make   the Make pair, null when the rule orders nothing
 * @param cap    refuse at Inbound Stations above this, 0 for no cap
 * @param export ship the surplus above this to the station that claims it, -1 for no export
 */
public record StockRule(ItemKey key, Make make, int cap, int export) {

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
        export = Math.max(-1, export);
    }

    /** A rule without an export. */
    public StockRule(ItemKey key, Make make, int cap) {
        this(key, make, cap, -1);
    }

    /** A rule with only a Make pair. */
    public static StockRule make(ItemKey key, int min, int max) {
        return new StockRule(key, new Make(min, max), 0);
    }

    public boolean hasExport() {
        return export >= 0;
    }

    public boolean hasMake() {
        return make != null;
    }

    public boolean hasCap() {
        return cap > 0;
    }

    public StockRule withMake(Make next) {
        return new StockRule(key, next, cap, export);
    }

    public StockRule withCap(int next) {
        return new StockRule(key, make, next, export);
    }

    /** Whether the rule still holds a threshold; one without is removed. */
    public boolean empty() {
        return !hasMake() && !hasCap() && !hasExport();
    }
}
