package io.github.dirnei.mc.servus.foundation.data.packet;

import net.minecraft.world.item.ItemStack;

/**
 * A packet endpoint's answer to an offer: room for {@link #amount()} items
 * reserved under a ticket, or a refusal with a lang key suffix as reason. A
 * delivery started with a grant puts items only into that room and closes the
 * grant when it ends; a sender holding a grant without a delivery closes it
 * itself. Server thread only.
 */
public final class Grant {

    private static final long UNBOUND = -1;

    private final PacketEndpoint endpoint;
    private final long ticket;
    private final int amount;
    private final String refusal;
    private int arrived;
    private long delivery = UNBOUND;
    private long lastUsed;
    private boolean closed;

    private Grant(PacketEndpoint endpoint, long ticket, int amount, String refusal, long now) {
        this.endpoint = endpoint;
        this.ticket = ticket;
        this.amount = amount;
        this.refusal = refusal;
        this.lastUsed = now;
        this.closed = endpoint == null;
    }

    /** Room for {@code amount} items at {@code endpoint}, reserved under {@code ticket} at game time {@code now}. */
    public static Grant of(PacketEndpoint endpoint, long ticket, int amount, long now) {
        return new Grant(endpoint, ticket, Math.max(0, amount), "", now);
    }

    /** No room; {@code reason} is a lang key suffix saying why. */
    public static Grant refused(String reason) {
        return new Grant(null, 0, 0, reason == null ? "" : reason, 0);
    }

    /** Whether room was granted. */
    public boolean granted() {
        return endpoint != null && amount > 0;
    }

    /** The endpoint's id for the reserved room. */
    public long ticket() {
        return ticket;
    }

    public int amount() {
        return amount;
    }

    /** Why the offer was refused, empty when it was granted. */
    public String refusal() {
        return refusal;
    }

    public int arrived() {
        return arrived;
    }

    /** How many more items the grant takes. */
    public int left() {
        return closed ? 0 : Math.max(0, amount - arrived);
    }

    public boolean closed() {
        return closed;
    }

    /** Whether a delivery uses the grant. */
    public boolean bound() {
        return delivery != UNBOUND;
    }

    /** The id of the delivery that uses the grant, -1 while none does. */
    public long delivery() {
        return delivery;
    }

    /** The game time the grant was made or last took items. */
    public long lastUsed() {
        return lastUsed;
    }

    void bind(long deliveryId) {
        delivery = deliveryId;
    }

    /**
     * Puts items into the reserved room through the endpoint, at most what the
     * grant still takes; returns what did not go in.
     */
    public ItemStack deliver(ItemStack stack, long now) {
        if (stack.isEmpty() || left() <= 0) {
            return stack;
        }
        int offered = Math.min(stack.getCount(), left());
        ItemStack part = stack.copyWithCount(offered);
        ItemStack rest = endpoint.receive(this, part);
        int taken = offered - rest.getCount();
        arrived += taken;
        if (taken > 0) {
            lastUsed = now;
        }
        ItemStack left = stack.copyWithCount(stack.getCount() - taken);
        return left.isEmpty() ? ItemStack.EMPTY : left;
    }

    /** Ends the grant once; the endpoint frees the room no item filled. */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        endpoint.close(this);
    }
}
