package io.github.dirnei.mc.servus.foundation.production;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * A block that sends items to a data member on request, by its own means (a
 * warehouse ships them as packets). Exposed as {@code servus_foundation:supplier}.
 */
public interface ItemSupplier {

    /**
     * The answer to a request.
     *
     * @param amount how many will be sent, zero when none can
     * @param ticket names the request for {@link #open} and {@link #cancel}; meaningless when the amount is zero
     */
    record Supply(int amount, long ticket) {
        public static final Supply NONE = new Supply(0, 0L);
    }

    /** Asks for up to {@code amount} items like the stack to be sent to the data member at {@code target}. */
    Supply supply(BlockPos target, ItemStack like, int amount);

    /** Whether the request with the ticket is still on its way; false for a ticket this supplier does not know. */
    boolean open(long ticket);

    /** Gives up the request with the ticket and takes back what it had not sent yet; does nothing for an unknown ticket. */
    void cancel(long ticket);
}
