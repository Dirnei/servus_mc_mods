package io.github.dirnei.mc.servus.foundation.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Open upkeep requests per member. Arrivals are counted from the member's
 * own count: every rise since the last check is credited against the open
 * request, which ends once complete or timed out.
 *
 * @param <K> member key
 */
public final class UpkeepBook<K> {

    private static final class Request {
        int remaining;
        int lastCount;
        long until;
    }

    private final Map<K, Request> open = new HashMap<>();

    /**
     * Updates the member's open request from its count, then answers how
     * much to ask for: nothing while a request is open or nothing is missing.
     */
    public int ask(K member, int count, int level, long now) {
        Request request = open.get(member);
        if (request != null) {
            if (count > request.lastCount) {
                request.remaining -= count - request.lastCount;
            }
            request.lastCount = count;
            if (request.remaining > 0 && now <= request.until) {
                return 0;
            }
            open.remove(member);
        }
        return Math.max(0, level - count);
    }

    /** Opens a request for {@code amount} that ends at {@code now + timeout} at the latest. */
    public void asked(K member, int amount, int count, long now, long timeout) {
        Request request = new Request();
        request.remaining = amount;
        request.lastCount = count;
        request.until = now + timeout;
        open.put(member, request);
    }

    /** Whether the member has a request on its way. */
    public boolean isOpen(K member) {
        return open.containsKey(member);
    }

    /** Forgets every member not in {@code members}. */
    public void retain(Collection<K> members) {
        open.keySet().retainAll(members);
    }
}
