package io.github.dirnei.mc.servus.storage.order;

import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Make this many of an item: the steps, the stock reserved for them and how it is going. */
public final class WorkOrder {

    public enum State { WAITING, RUNNING, BLOCKED, DONE, CANCELLED }

    private final int id;
    private final ItemKey key;
    private final int amount;
    private final int requestId;
    private final List<OrderStep> steps;
    private final Map<ItemKey, Integer> reserved;
    private final Map<ItemKey, Integer> missing;
    private State state = State.RUNNING;
    private String reason = "";
    private long doneAt = -1;

    public WorkOrder(int id, ItemKey key, int amount, int requestId, List<OrderStep> steps,
                     Map<ItemKey, Integer> reserved, Map<ItemKey, Integer> missing) {
        this.id = id;
        this.key = key;
        this.amount = amount;
        this.requestId = requestId;
        this.steps = List.copyOf(steps);
        this.reserved = new HashMap<>(reserved);
        this.missing = new HashMap<>(missing);
    }

    public int id() {
        return id;
    }

    public ItemKey key() {
        return key;
    }

    public int amount() {
        return amount;
    }

    /** The terminal request this order serves, 0 for a manual order. */
    public int requestId() {
        return requestId;
    }

    public List<OrderStep> steps() {
        return steps;
    }

    /** Stock on hand this order still counts on, per key. */
    public Map<ItemKey, Integer> reserved() {
        return Collections.unmodifiableMap(reserved);
    }

    void useReserved(ItemKey key, int count) {
        int left = reserved.getOrDefault(key, 0) - count;
        if (left <= 0) {
            reserved.remove(key);
        } else {
            reserved.put(key, left);
        }
    }

    void clearReserved() {
        reserved.clear();
    }

    /** Raw materials the plan could not get, per key. */
    public Map<ItemKey, Integer> missing() {
        return Collections.unmodifiableMap(missing);
    }

    public State state() {
        return state;
    }

    public String reason() {
        return reason;
    }

    void setState(State state, String reason) {
        this.state = state;
        this.reason = reason == null ? "" : reason;
    }

    public long doneAt() {
        return doneAt;
    }

    void setDoneAt(long doneAt) {
        this.doneAt = doneAt;
    }

    public boolean open() {
        return state == State.WAITING || state == State.RUNNING || state == State.BLOCKED;
    }

    /** Whether a step of this order is feeding or producing on the producer. */
    public boolean occupies(net.minecraft.core.BlockPos producer, OrderStep except) {
        if (!open()) {
            return false;
        }
        for (OrderStep step : steps) {
            if (step != except && step.producer().equals(producer)
                    && (step.state() == OrderStep.State.DELIVERING || step.state() == OrderStep.State.PRODUCING)) {
                return true;
            }
        }
        return false;
    }
}
