package io.github.dirnei.mc.servus.shuttles.network;

import io.github.dirnei.mc.servus.shuttles.lock.LockedSlots;
import io.github.dirnei.mc.servus.shuttles.zone.PutAwayPlan;

/**
 * A block whose buffer the warehouse puts away into its racks by the zone
 * rules, with plain shuttles that fetch at the rails it docks at: an Inbound
 * or Packing Station, a Field Head, or a Parcel Locker's returning parcels.
 * Server thread only.
 */
public interface PutAwaySource {

    /** The slots put-aways take from; each planned stack holds an OUT lock. */
    LockedSlots putAwayBuffer();

    /** Whether the slot of the buffer is put away now; every slot by default. */
    default boolean drains(int slot) {
        return true;
    }

    /** Records the first stack that has nowhere to go and why. */
    void setBlocked(PutAwayPlan.Reason reason, ItemKey key, String zoneDisplay);

    /** Nothing in the buffer is stuck. */
    void clearBlocked();
}
