package io.github.dirnei.mc.servus.shuttles.network;

import io.github.dirnei.mc.servus.shuttles.lock.LockedSlots;

/** A block shuttles and instant picks deliver into: a terminal's output slots, a dock's feed buffer. */
public interface DeliveryTarget {

    /** The buffer deliveries land in. */
    LockedSlots deliveries();

    /**
     * Locks room in {@link #deliveries()} for a pick of a request before a
     * shuttle is sent: empty slots only by default.
     *
     * @return how many items it found room for
     */
    default int lockDelivery(int jobId, int requestId, ItemKey key, int amount) {
        return deliveries().lockIn(jobId, key, amount, true);
    }
}
