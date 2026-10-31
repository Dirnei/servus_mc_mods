package io.github.dirnei.mc.servus.shuttles.lock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Plans which exact slots a job fills or empties, looking only at slots no
 * job has locked. Pure Java.
 */
public final class SlotFill {

    private SlotFill() {
    }

    /**
     * One slot as the planner sees it.
     *
     * @param key      what it holds, null when empty
     * @param count    how many it holds
     * @param limit    how many of the key the slot can hold at most
     * @param locked   whether a job has locked it
     * @param reserved how many of its items OUT locks reserve; a locked slot reserving none is IN locked
     */
    public record Slot<K>(K key, int count, int limit, boolean locked, int reserved) {

        /** A slot no OUT lock reserves anything of. */
        public Slot(K key, int count, int limit, boolean locked) {
            this(key, count, limit, locked, 0);
        }

        boolean empty() {
            return key == null || count <= 0;
        }

        /** Items no job reserved yet; none for an IN locked slot. */
        int unreserved() {
            return locked && reserved == 0 ? 0 : Math.max(0, count - reserved);
        }
    }

    /** A part of a job's amount on one slot. */
    public record Part(int slot, int count) {
    }

    /** Room an IN lock reserves on a slot for a job on its way. */
    public record Reservation<K>(K key, int amount) {
    }

    /**
     * A counted slot as incoming picks see it: what it holds plus what IN
     * locks already bring count as its item, and it is never locked, so
     * {@link #in} joins picks on their way. A slot whose stack and
     * reservations name different items is seen as locked and takes nothing.
     *
     * @param key   the item the slot holds, null when empty
     * @param count how many it holds
     * @param limit how many the slot can hold at most
     */
    public static <K> Slot<K> incoming(K key, int count, int limit, List<Reservation<K>> reservations) {
        K seen = count > 0 ? key : null;
        int total = Math.max(0, count);
        boolean mixed = false;
        for (Reservation<K> reservation : reservations) {
            if (seen == null) {
                seen = reservation.key();
            } else if (!Objects.equals(seen, reservation.key())) {
                mixed = true;
            }
            total += Math.max(0, reservation.amount());
        }
        return new Slot<>(seen, total, limit, mixed);
    }

    /**
     * Where {@code amount} of the key goes: first on top of unlocked partial
     * stacks of the key, then into unlocked empty slots, each up to {@code max}
     * per slot. The parts may add up to less than the amount when room runs out.
     */
    public static <K> List<Part> in(List<Slot<K>> slots, K key, int amount, int max) {
        List<Part> parts = new ArrayList<>();
        int left = amount;
        for (int i = 0; i < slots.size() && left > 0; i++) {
            Slot<K> slot = slots.get(i);
            if (!slot.locked() && !slot.empty() && Objects.equals(slot.key(), key)) {
                int room = Math.min(max, slot.limit()) - slot.count();
                if (room > 0) {
                    int count = Math.min(room, left);
                    parts.add(new Part(i, count));
                    left -= count;
                }
            }
        }
        return fillEmpty(slots, parts, left, max);
    }

    /** Where {@code amount} goes when only whole empty unlocked slots may take it, up to {@code max} per slot. */
    public static <K> List<Part> inEmpty(List<Slot<K>> slots, int amount, int max) {
        return fillEmpty(slots, new ArrayList<>(), amount, max);
    }

    private static <K> List<Part> fillEmpty(List<Slot<K>> slots, List<Part> parts, int amount, int max) {
        int left = amount;
        for (int i = 0; i < slots.size() && left > 0; i++) {
            Slot<K> slot = slots.get(i);
            if (!slot.locked() && slot.empty()) {
                int count = Math.min(Math.min(max, slot.limit()), left);
                if (count > 0) {
                    parts.add(new Part(i, count));
                    left -= count;
                }
            }
        }
        return parts;
    }

    /**
     * Which slots give {@code amount} of the key: slots holding items of it no
     * job reserved yet, the fewest first so slots free up, the earlier slot on
     * equal counts. Several jobs may take from one slot.
     */
    public static <K> List<Part> out(List<Slot<K>> slots, K key, int amount) {
        List<Integer> holding = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            Slot<K> slot = slots.get(i);
            if (slot.unreserved() > 0 && Objects.equals(slot.key(), key)) {
                holding.add(i);
            }
        }
        holding.sort(Comparator.comparingInt((Integer i) -> slots.get(i).unreserved()).thenComparingInt(i -> i));
        List<Part> parts = new ArrayList<>();
        int left = amount;
        for (int i : holding) {
            if (left <= 0) {
                break;
            }
            int count = Math.min(slots.get(i).unreserved(), left);
            parts.add(new Part(i, count));
            left -= count;
        }
        return parts;
    }

    /** How many of the key the unlocked slots still take, {@code max} per slot. */
    public static <K> int room(List<Slot<K>> slots, K key, int max) {
        return total(in(slots, key, Integer.MAX_VALUE, max));
    }

    /** How many items of the key no job reserved yet. */
    public static <K> int available(List<Slot<K>> slots, K key) {
        return total(out(slots, key, Integer.MAX_VALUE));
    }

    /** Unlocked empty slots. */
    public static <K> int freeSlots(List<Slot<K>> slots) {
        int free = 0;
        for (Slot<K> slot : slots) {
            if (!slot.locked() && slot.empty()) {
                free++;
            }
        }
        return free;
    }

    /** Slots in use: holding items or locked by a job, so room a job reserved counts as taken. */
    public static <K> int inUse(List<Slot<K>> slots) {
        int used = 0;
        for (Slot<K> slot : slots) {
            if (slot.locked() || !slot.empty()) {
                used++;
            }
        }
        return used;
    }

    /** Whether an unlocked slot holds a partial stack of the key. */
    public static <K> boolean partial(List<Slot<K>> slots, K key, int max) {
        for (Slot<K> slot : slots) {
            if (!slot.locked() && !slot.empty() && Objects.equals(slot.key(), key) && slot.count() < Math.min(max, slot.limit())) {
                return true;
            }
        }
        return false;
    }

    /** Whether any slot, locked or not, holds the key. */
    public static <K> boolean holds(List<Slot<K>> slots, K key) {
        for (Slot<K> slot : slots) {
            if (!slot.empty() && Objects.equals(slot.key(), key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Room in a counted slot for anyone but the jobs that reserved it: the
     * limit less what the slot holds and less the room other jobs' IN locks
     * reserve, never below 0.
     */
    public static int sharedRoom(long limit, int count, int reservedIn) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, limit - Math.max(0, count) - Math.max(0, reservedIn)));
    }

    /** Items of a counted slot that other jobs' OUT locks do not reserve, never below 0. */
    public static int sharedAvailable(int count, int reservedOut) {
        return Math.max(0, count - Math.max(0, reservedOut));
    }

    /** Items of a slot that no OUT lock reserves yet, never below 0. */
    public static int unreserved(int count, int reservedOut) {
        return Math.max(0, count - Math.max(0, reservedOut));
    }

    /** The sum of the parts' counts. */
    public static int total(List<Part> parts) {
        int sum = 0;
        for (Part part : parts) {
            sum += part.count();
        }
        return sum;
    }
}
