package io.github.dirnei.mc.servus.shuttles.locker;

/**
 * The lamp on a Parcel Locker door as one player sees it: dark while free,
 * ochre for someone else's parcel, amber while reserved or going back to the
 * racks, green for the player's own ready parcel. Pure Java.
 */
public enum LockerLamp {
    OFF,
    TAKEN,
    RESERVED,
    READY;

    /**
     * The lamp of a box held by an order in that state, or of a free box when
     * {@code state} is null.
     *
     * @param mine whether the order belongs to the player looking
     */
    public static LockerLamp of(LockerReturns.State state, boolean mine) {
        if (state == null) {
            return OFF;
        }
        return switch (state) {
            case READY -> mine ? READY : TAKEN;
            case RESERVED, RETURNING -> RESERVED;
        };
    }
}
