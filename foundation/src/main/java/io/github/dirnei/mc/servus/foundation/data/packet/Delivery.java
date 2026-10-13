package io.github.dirnei.mc.servus.foundation.data.packet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The state of one delivery: how many items were asked for and arrived, and
 * whether it is running, done or failed. Listeners run once when it ends.
 */
public final class Delivery {

    public enum State {
        RUNNING,
        DONE,
        FAILED
    }

    private final long id;
    private final int requested;
    private int delivered;
    private State state = State.RUNNING;
    private DeliveryReason reason;
    private final List<Consumer<Delivery>> listeners = new ArrayList<>();

    public Delivery(long id, int requested) {
        this.id = id;
        this.requested = requested;
    }

    public long id() {
        return id;
    }

    public int requested() {
        return requested;
    }

    public int delivered() {
        return delivered;
    }

    public State state() {
        return state;
    }

    /** Why it failed, or null while running or when done. */
    public DeliveryReason reason() {
        return reason;
    }

    public boolean ended() {
        return state != State.RUNNING;
    }

    public void addDelivered(int count) {
        delivered += count;
    }

    public void done() {
        end(State.DONE, null);
    }

    public void fail(DeliveryReason why) {
        end(State.FAILED, why);
    }

    /** Runs the listener when the delivery ends, or right away when it already has. */
    public void onEnd(Consumer<Delivery> listener) {
        if (ended()) {
            listener.accept(this);
        } else {
            listeners.add(listener);
        }
    }

    private void end(State to, DeliveryReason why) {
        if (ended()) {
            return;
        }
        state = to;
        reason = why;
        List<Consumer<Delivery>> run = new ArrayList<>(listeners);
        listeners.clear();
        run.forEach(listener -> listener.accept(this));
    }

    @Override
    public String toString() {
        return "Delivery[" + id + " " + state + (reason == null ? "" : " " + reason) + " " + delivered + "/" + requested + "]";
    }
}
