package io.github.dirnei.mc.servus.shuttles.cpu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The buffer of a Crafting CPU: per order and item the intermediates that
 * wait for a later step of that order. A stack is one key of one order up to
 * its max stack size; the CPU has room for a number of stacks, shared by all
 * orders first come first served. Items on their way in (an arrival, named by
 * its transfer id) hold their room until the transfer ends; they are credited
 * to their order as they land.
 *
 * @param <K> the item key
 */
public final class CpuBuffer<K> {

    /** What one order holds of one key. */
    public record Entry<K>(long order, K key, int count, int maxStack, boolean leaving) {
    }

    /** Items of one transfer on their way in: {@code landed} of {@code expected} have arrived so far. */
    public record Arrival<K>(long transfer, long order, K key, int expected, int landed, int maxStack) {
    }

    private record Slot<K>(long order, K key) {
    }

    private static final class Held {
        int count;
        int maxStack;
        boolean leaving;

        Held(int maxStack) {
            this.maxStack = Math.max(1, maxStack);
        }
    }

    private static final class Open<K> {
        final long transfer;
        final Slot<K> slot;
        final int expected;
        int landed;

        Open(long transfer, Slot<K> slot, int expected) {
            this.transfer = transfer;
            this.slot = slot;
            this.expected = expected;
        }
    }

    private final Map<Slot<K>, Held> held = new LinkedHashMap<>();
    private final Map<Long, Open<K>> arrivals = new LinkedHashMap<>();
    private int capacity;

    public CpuBuffer(int capacity) {
        this.capacity = Math.max(0, capacity);
    }

    /** The room in stacks. */
    public int capacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = Math.max(0, capacity);
    }

    /** Stacks in use, counting items on their way in. */
    public int used() {
        int used = 0;
        for (Map.Entry<Slot<K>, Held> entry : held.entrySet()) {
            used += stacks(reserved(entry.getKey(), entry.getValue()), entry.getValue().maxStack);
        }
        return used;
    }

    /** How many more of the key the order can put in: the free space of its last stack plus whole free stacks. */
    public int room(long order, K key, int maxStack) {
        Slot<K> slot = new Slot<>(order, key);
        Held entry = held.get(slot);
        int stackSize = entry != null ? entry.maxStack : Math.max(1, maxStack);
        int reserved = entry != null ? reserved(slot, entry) : 0;
        int rest = reserved % stackSize;
        int lastFree = rest == 0 ? 0 : stackSize - rest;
        long freeStacks = Math.max(0, capacity - used());
        return (int) Math.min(Integer.MAX_VALUE, lastFree + freeStacks * stackSize);
    }

    /** Items of the key the order holds, without those on their way in. */
    public int held(long order, K key) {
        Held entry = held.get(new Slot<>(order, key));
        return entry == null ? 0 : entry.count;
    }

    /** Items of the key on their way in for the order. */
    public int incoming(long order, K key) {
        Slot<K> slot = new Slot<>(order, key);
        int sum = 0;
        for (Open<K> open : arrivals.values()) {
            if (open.slot.equals(slot)) {
                sum += open.expected - open.landed;
            }
        }
        return sum;
    }

    /** Books items of a transfer on their way in; they hold their room from now on. */
    public void expect(long transfer, long order, K key, int count, int maxStack) {
        if (count <= 0) {
            return;
        }
        Slot<K> slot = new Slot<>(order, key);
        held.computeIfAbsent(slot, s -> new Held(maxStack));
        arrivals.put(transfer, new Open<>(transfer, slot, count));
    }

    /** Items of the key that open arrivals still expect; what the CPU's inlet takes. */
    public int expected(K key) {
        int sum = 0;
        for (Open<K> open : arrivals.values()) {
            if (open.slot.key().equals(key)) {
                sum += open.expected - open.landed;
            }
        }
        return sum;
    }

    /**
     * Items of the key landed in the CPU: credited to the open arrivals of
     * that key, oldest first.
     *
     * @return how many were taken in, at most {@link #expected(Object)}
     */
    public int land(K key, int count) {
        int left = Math.max(0, count);
        for (Open<K> open : arrivals.values()) {
            if (left <= 0) {
                break;
            }
            if (!open.slot.key().equals(key)) {
                continue;
            }
            int part = Math.min(left, open.expected - open.landed);
            if (part <= 0) {
                continue;
            }
            open.landed += part;
            held.get(open.slot).count += part;
            left -= part;
        }
        return Math.max(0, count) - left;
    }

    /** The transfer ended: what did not land releases its room. */
    public void arrived(long transfer) {
        Open<K> open = arrivals.remove(transfer);
        if (open != null) {
            dropIfEmpty(open.slot);
        }
    }

    /** Whether a transfer into the buffer is still open. */
    public boolean expects(long transfer) {
        return arrivals.containsKey(transfer);
    }

    /** Takes up to {@code count} of the order's items of the key out at once. */
    public int take(long order, K key, int count) {
        Slot<K> slot = new Slot<>(order, key);
        Held entry = held.get(slot);
        if (entry == null || count <= 0) {
            return 0;
        }
        int taken = Math.min(count, entry.count);
        entry.count -= taken;
        dropIfEmpty(slot);
        return taken;
    }

    /** The order is done or cancelled: what it holds becomes leftovers that keep their room until taken. */
    public void leave(long order) {
        for (Map.Entry<Slot<K>, Held> entry : held.entrySet()) {
            if (entry.getKey().order() == order) {
                entry.getValue().leaving = true;
            }
        }
    }

    /** Every entry that holds items, oldest first. */
    public List<Entry<K>> entries() {
        List<Entry<K>> list = new ArrayList<>();
        for (Map.Entry<Slot<K>, Held> entry : held.entrySet()) {
            Held value = entry.getValue();
            if (value.count > 0) {
                list.add(new Entry<>(entry.getKey().order(), entry.getKey().key(), value.count, value.maxStack, value.leaving));
            }
        }
        return list;
    }

    /** Entries of orders that are done or cancelled. */
    public List<Entry<K>> leftovers() {
        List<Entry<K>> list = new ArrayList<>();
        for (Entry<K> entry : entries()) {
            if (entry.leaving()) {
                list.add(entry);
            }
        }
        return list;
    }

    public List<Arrival<K>> arrivals() {
        List<Arrival<K>> list = new ArrayList<>();
        for (Open<K> open : arrivals.values()) {
            list.add(new Arrival<>(open.transfer, open.slot.order(), open.slot.key(), open.expected, open.landed,
                    held.get(open.slot).maxStack));
        }
        return list;
    }

    /** Puts back a saved entry. */
    public void restore(Entry<K> entry) {
        if (entry.count() <= 0) {
            return;
        }
        Held value = held.computeIfAbsent(new Slot<>(entry.order(), entry.key()), s -> new Held(entry.maxStack()));
        value.count += entry.count();
        value.leaving |= entry.leaving();
    }

    /** Puts back a saved arrival. */
    public void restore(Arrival<K> arrival) {
        Slot<K> slot = new Slot<>(arrival.order(), arrival.key());
        held.computeIfAbsent(slot, s -> new Held(arrival.maxStack()));
        Open<K> open = new Open<>(arrival.transfer(), slot, arrival.expected());
        open.landed = Math.min(arrival.expected(), Math.max(0, arrival.landed()));
        arrivals.put(arrival.transfer(), open);
    }

    /** Takes every item out and forgets every arrival; what was held is returned. */
    public List<Entry<K>> clear() {
        List<Entry<K>> all = entries();
        held.clear();
        arrivals.clear();
        return all;
    }

    public boolean isEmpty() {
        return held.isEmpty() && arrivals.isEmpty();
    }

    private int reserved(Slot<K> slot, Held entry) {
        int reserved = entry.count;
        for (Open<K> open : arrivals.values()) {
            if (open.slot.equals(slot)) {
                reserved += open.expected - open.landed;
            }
        }
        return reserved;
    }

    private void dropIfEmpty(Slot<K> slot) {
        Held entry = held.get(slot);
        if (entry == null || entry.count > 0) {
            return;
        }
        for (Open<K> open : arrivals.values()) {
            if (open.slot.equals(slot)) {
                return;
            }
        }
        held.remove(slot);
    }

    private static int stacks(int count, int maxStack) {
        return count <= 0 ? 0 : (count + maxStack - 1) / maxStack;
    }

    @Override
    public String toString() {
        return "CpuBuffer[" + used() + "/" + capacity + " " + entries() + " " + arrivals() + "]";
    }
}
