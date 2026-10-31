package io.github.dirnei.mc.servus.shuttles.locker;

/** When an uncollected parcel goes back to the racks. Pure Java. */
public final class LockerReturns {

    /** Where an order stands: boxes reserved while shuttles deliver, a parcel ready to collect, or one going back to the racks. */
    public enum State { RESERVED, READY, RETURNING }

    private LockerReturns() {
    }

    /** Whether a parcel ready since {@code readySince} has waited {@code returnTicks} by {@code now}; only ready parcels expire. */
    public static boolean expired(State state, long readySince, long now, long returnTicks) {
        return state == State.READY && now - readySince >= returnTicks;
    }

    /** The ticks left until a ready parcel returns, 0 once it is due. */
    public static long ticksLeft(long readySince, long now, long returnTicks) {
        return Math.max(0, readySince + returnTicks - now);
    }
}
