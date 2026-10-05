package io.github.dirnei.mc.servus.lib.network.core;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Event queue for one network. Any thread may post; the owner drains it on the
 * server thread once per tick. A drain handles only the events that were
 * present when it started, so events posted while handling wait for the next
 * tick. That keeps a network from starving the tick by posting to itself.
 *
 * @param <E> event type
 */
public final class Mailbox<E> {

    private final ConcurrentLinkedQueue<E> queue = new ConcurrentLinkedQueue<>();

    public void post(E event) {
        queue.add(event);
    }

    /**
     * Handles the events present at the start of the call, in posting order.
     *
     * @return number of events handled
     */
    public int drain(Consumer<? super E> handler) {
        int budget = queue.size();
        int handled = 0;
        while (handled < budget) {
            E event = queue.poll();
            if (event == null) {
                break;
            }
            handler.accept(event);
            handled++;
        }
        return handled;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public void clear() {
        queue.clear();
    }
}
