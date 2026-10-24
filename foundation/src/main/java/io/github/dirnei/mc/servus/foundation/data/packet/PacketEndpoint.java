package io.github.dirnei.mc.servus.foundation.data.packet;

import net.minecraft.world.item.ItemStack;

/**
 * A data member that answers offers before items are sent to it, so a
 * delivery reserves room instead of trying until something fits. A delivery
 * to a target with an endpoint offers first, sends at most the granted
 * amount, puts items in only through {@link #receive} and closes the grant
 * when it ends. Exposed as the {@code servus_foundation:packet_endpoint}
 * block capability. Server thread only.
 */
public interface PacketEndpoint {

    /** Answers an offer of {@code amount} items like {@code like}: a grant with reserved room, or a refusal. */
    Grant offer(ItemStack like, int amount);

    /** Puts items of an open grant into its reserved room; returns what did not go in. */
    ItemStack receive(Grant grant, ItemStack stack);

    /** The grant ended after {@link Grant#arrived()} items: frees the room no item filled. */
    void close(Grant grant);
}
