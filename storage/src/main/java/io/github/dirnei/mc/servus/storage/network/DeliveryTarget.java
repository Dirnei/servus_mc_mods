package io.github.dirnei.mc.servus.storage.network;

import net.neoforged.neoforge.items.ItemStackHandler;

/** A block shuttles and instant picks deliver into: a terminal's output slots, a dock's feed buffer. */
public interface DeliveryTarget {

    /** The buffer deliveries land in. */
    ItemStackHandler deliveries();
}
