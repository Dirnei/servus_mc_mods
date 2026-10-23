package io.github.dirnei.mc.servus.shuttles.network;

import io.github.dirnei.mc.servus.shuttles.lock.LockedSlots;

/** A block shuttles and instant picks deliver into: a terminal's output slots, a dock's feed buffer. */
public interface DeliveryTarget {

    /** The buffer deliveries land in. */
    LockedSlots deliveries();
}
