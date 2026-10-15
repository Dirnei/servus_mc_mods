package io.github.dirnei.mc.servus.shuttles.site;

/** How a site looks from another site: online, asleep and able to wake, or asleep for good. */
public final class RemoteSite {

    public enum State {
        /** Loaded and online: live stock, orders go straight through. */
        ONLINE,
        /** Asleep with enough Anchor Upgrades: last known stock, an order wakes it. */
        SLEEPING,
        /** Asleep without a way to wake: last known stock, no orders. */
        UNREACHABLE;

        public boolean orderable() {
            return this != UNREACHABLE;
        }
    }

    private RemoteSite() {
    }

    public static State state(boolean online, SiteCapacity.State capacity) {
        if (online) {
            return State.ONLINE;
        }
        return capacity == SiteCapacity.State.COVERED ? State.SLEEPING : State.UNREACHABLE;
    }
}
