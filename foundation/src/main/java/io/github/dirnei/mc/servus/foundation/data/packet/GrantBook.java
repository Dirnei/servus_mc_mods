package io.github.dirnei.mc.servus.foundation.data.packet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The open grants of one endpoint by ticket, with the sweep every endpoint
 * needs: a grant whose delivery no longer runs, or that no delivery took up
 * and that took no items for {@code grant_hold_ticks}, is closed. Server
 * thread only.
 */
public final class GrantBook {

    private final Map<Long, Grant> open = new LinkedHashMap<>();

    public void add(Grant grant) {
        if (grant.granted()) {
            open.put(grant.ticket(), grant);
        }
    }

    /** Forgets a grant; called from the endpoint's close. */
    public void remove(Grant grant) {
        open.remove(grant.ticket());
    }

    /** Whether the grant is one of these and still open. */
    public boolean holds(Grant grant) {
        return grant != null && open.get(grant.ticket()) == grant && !grant.closed();
    }

    public boolean holds(long ticket) {
        return open.containsKey(ticket);
    }

    public List<Grant> grants() {
        return List.copyOf(open.values());
    }

    /** Closes the grants that nobody uses any more, as of game time {@code now}. */
    public void sweep(long now) {
        long hold = PacketLimitsManager.INSTANCE.limits().grantHoldTicks();
        for (Grant grant : new ArrayList<>(open.values())) {
            if (grant.closed() || stale(grant, now, hold)) {
                grant.close();
                open.remove(grant.ticket());
            }
        }
    }

    private static boolean stale(Grant grant, long now, long hold) {
        return grant.bound() ? !DataPackets.running(grant.delivery()) : now - grant.lastUsed() > hold;
    }

    /** Closes every grant, for an endpoint that goes away. */
    public void closeAll() {
        for (Grant grant : new ArrayList<>(open.values())) {
            grant.close();
        }
        open.clear();
    }
}
