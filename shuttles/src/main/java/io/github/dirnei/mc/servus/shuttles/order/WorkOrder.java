package io.github.dirnei.mc.servus.shuttles.order;

import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.pattern.EncodedPattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

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
    /** The Storage Terminal whose request this order serves, null when the results go to storage. */
    private BlockPos deliverTo;
    /** How many of the order's item were sent straight to {@link #deliverTo}. */
    private int directSent;
    /** The pattern that makes the order's item when the order was started from a grid, null otherwise. */
    private EncodedPattern oneOff;
    /** Intermediates later steps still needed that went to storage because the CPU's buffer was full, per key. */
    private final Map<ItemKey, Integer> overflowed = new LinkedHashMap<>();

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

    public BlockPos deliverTo() {
        return deliverTo;
    }

    public void deliverTo(BlockPos terminal) {
        this.deliverTo = terminal;
    }

    /** The pattern this order runs whether or not the line keeps it, or null. */
    public EncodedPattern oneOff() {
        return oneOff;
    }

    public void setOneOff(EncodedPattern pattern) {
        this.oneOff = pattern;
    }

    public int directSent() {
        return directSent;
    }

    public void sentDirect(int count) {
        directSent += count;
    }

    /** Intermediates that went to storage because the buffer was full, per key. */
    public Map<ItemKey, Integer> overflowed() {
        return Collections.unmodifiableMap(overflowed);
    }

    public void overflowed(ItemKey key, int count) {
        if (count > 0) {
            overflowed.merge(key, count, Integer::sum);
        }
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
    public boolean occupies(BlockPos producer, OrderStep except) {
        if (!open()) {
            return false;
        }
        for (OrderStep step : steps) {
            if (step != except && step.producer().equals(producer) && step.active()) {
                return true;
            }
        }
        return false;
    }

    CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Id", id);
        tag.put("Item", Saving.key(key, registries));
        tag.putInt("Amount", amount);
        tag.putInt("Request", requestId);
        tag.putString("State", state.name());
        tag.putString("Reason", reason);
        tag.putLong("DoneAt", doneAt);
        if (deliverTo != null) {
            tag.putLong("DeliverTo", deliverTo.asLong());
        }
        tag.putInt("Direct", directSent);
        tag.put("Reserved", Saving.counts(reserved, registries));
        tag.put("Missing", Saving.counts(missing, registries));
        if (!overflowed.isEmpty()) {
            tag.put("Overflowed", Saving.counts(overflowed, registries));
        }
        if (oneOff != null) {
            EncodedPattern.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), oneOff)
                    .result().ifPresent(saved -> tag.put("OneOff", saved));
        }
        ListTag list = new ListTag();
        for (OrderStep step : steps) {
            list.add(step.save(registries));
        }
        tag.put("Steps", list);
        return tag;
    }

    /** The saved order, or null when its item no longer exists. */
    static WorkOrder load(CompoundTag tag, HolderLookup.Provider registries) {
        ItemKey key = Saving.key(tag.getCompoundOrEmpty("Item"), registries).orElse(null);
        if (key == null) {
            return null;
        }
        List<OrderStep> steps = new ArrayList<>();
        for (Tag raw : tag.getListOrEmpty("Steps")) {
            steps.add(OrderStep.load((CompoundTag) raw, registries));
        }
        WorkOrder order = new WorkOrder(tag.getIntOr("Id", 0), key, tag.getIntOr("Amount", 0), tag.getIntOr("Request", 0), steps,
                Saving.counts(tag.getListOrEmpty("Reserved"), registries),
                Saving.counts(tag.getListOrEmpty("Missing"), registries));
        order.state = Saving.state(State.class, tag.getStringOr("State", ""), State.RUNNING);
        order.reason = tag.getStringOr("Reason", "");
        order.doneAt = tag.getLongOr("DoneAt", 0L);
        if (tag.contains("DeliverTo")) {
            order.deliverTo = BlockPos.of(tag.getLongOr("DeliverTo", 0L));
        }
        order.directSent = tag.getIntOr("Direct", 0);
        order.overflowed.putAll(Saving.counts(tag.getListOrEmpty("Overflowed"), registries));
        if (tag.contains("OneOff")) {
            order.oneOff = EncodedPattern.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("OneOff"))
                    .result().orElse(null);
        }
        return order;
    }
}
