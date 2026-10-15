package io.github.dirnei.mc.servus.shuttles.site;

/** How many chunks a site's Anchor Upgrades cover, and whether its span fits. */
public final class SiteCapacity {

    /** Whether a site can wake at all as far as its size goes. */
    public enum State {
        /** No upgrade: nothing to exceed, the site never wakes. */
        NO_UPGRADE,
        /** The upgrades cover every chunk of the span. */
        COVERED,
        /** The span is larger than the upgrades cover: an error, the site never wakes. */
        OVER
    }

    private SiteCapacity() {
    }

    public static int capacity(int upgrades, int chunksPerUpgrade) {
        return Math.max(0, upgrades) * Math.max(0, chunksPerUpgrade);
    }

    public static State state(int span, int upgrades, int chunksPerUpgrade) {
        if (upgrades <= 0) {
            return State.NO_UPGRADE;
        }
        return span > capacity(upgrades, chunksPerUpgrade) ? State.OVER : State.COVERED;
    }
}
