package io.github.dirnei.mc.servus.storage.network;

import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;

/** A block shuttles and instant picks deliver into: a terminal's output slots, a dock's feed buffer. */
public interface DeliveryTarget {

    /** The buffer deliveries land in. */
    ItemSlots deliveries();
}
