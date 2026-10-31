package io.github.dirnei.mc.servus.shuttles.lock;

import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Item slots whose single slots shuttle jobs can lock. A slot holds either
 * one IN lock or OUT locks of jobs that together take no more than it holds.
 * A locked slot refuses every insert and extract, through the item
 * capability, menus and the slot helpers alike, unless a job holding a lock
 * on it is the one acting ({@link #insertAs}, {@link #extractAs}). Only
 * {@link #setStackInSlot} writes past a lock; callers that move items must not
 * use it on locked slots.
 *
 * <p>Counted slots (a Field Head buffer part) reserve instead of closing: a
 * slot holds IN and OUT locks of several jobs side by side, and everyone but
 * a job acting through its own lock sees the slot's room and items less what
 * the other jobs reserved.
 *
 * <p>Top up slots (an Inbound Station buffer) keep a slot that holds only OUT
 * locks open to more of its own item, so a planned put-away no shuttle has
 * loaded yet keeps filling; extracts stay closed to all but the job.
 */
public class LockedSlots extends ItemSlots {

    /** How locks act on their slots. */
    public enum Mode {
        /** A locked slot is closed to everyone but its jobs. */
        CLOSED,
        /** Locks only reserve room and items. */
        COUNTED,
        /** Like closed, but a slot with only OUT locks takes more of its own item. */
        TOP_UP
    }

    private final List<List<SlotLock>> locks = new ArrayList<>();
    /** The job allowed through its own locks right now, 0 for none. */
    private int acting;
    private final boolean counted;
    private final boolean topUp;

    public LockedSlots(int size) {
        this(size, Mode.CLOSED);
    }

    /** @param counted whether locks only reserve room and items instead of closing their slot */
    public LockedSlots(int size, boolean counted) {
        this(size, counted ? Mode.COUNTED : Mode.CLOSED);
    }

    public LockedSlots(int size, Mode mode) {
        super(size);
        this.counted = mode == Mode.COUNTED;
        this.topUp = mode == Mode.TOP_UP;
    }

    /** Whether a slot holding only OUT locks takes more of its own item. */
    public boolean topsUp() {
        return topUp;
    }

    /** Whether a slot holds an IN lock. */
    public boolean hasIn(int slot) {
        return inRange(slot) && at(slot).stream().anyMatch(lock -> lock.kind() == SlotLock.Kind.IN);
    }

    /** Whether a top up slot takes the resource although it is locked: it holds only OUT locks and that item. */
    private boolean takesTopUp(int slot, ItemResource resource) {
        if (!topUp || !inRange(slot) || at(slot).isEmpty() || hasIn(slot)) {
            return false;
        }
        ItemStack stack = getStackInSlot(slot);
        return !stack.isEmpty() && ItemResource.of(stack).equals(resource);
    }

    /** Grows the job's OUT lock on the slot by {@code more}; nothing when the job holds none there. */
    public void growOut(int slot, int jobId, int more) {
        if (!inRange(slot) || more <= 0) {
            return;
        }
        List<SlotLock> slotLocks = at(slot);
        for (int i = 0; i < slotLocks.size(); i++) {
            SlotLock lock = slotLocks.get(i);
            if (lock.jobId() == jobId && lock.kind() == SlotLock.Kind.OUT) {
                slotLocks.set(i, new SlotLock(SlotLock.Kind.OUT, lock.key(), lock.amount() + more, jobId));
                onLocksChanged();
                return;
            }
        }
    }

    /** Whether the job holds an OUT lock on the slot. */
    public boolean holdsOut(int slot, int jobId) {
        return inRange(slot) && at(slot).stream().anyMatch(lock -> lock.jobId() == jobId && lock.kind() == SlotLock.Kind.OUT);
    }

    /** Called after a lock was placed or released. */
    protected void onLocksChanged() {
    }

    /** The job acting through its own locks right now, 0 while none is. */
    protected int actingJob() {
        return acting;
    }

    /** The locks of a slot, growing the table to the slot count first. */
    private List<SlotLock> at(int slot) {
        while (locks.size() < getSlots()) {
            locks.add(new ArrayList<>());
        }
        return locks.get(slot);
    }

    private boolean inRange(int slot) {
        return slot >= 0 && slot < getSlots();
    }

    /** The locks on the slot, empty when it is free. */
    public List<SlotLock> locksOf(int slot) {
        return inRange(slot) ? List.copyOf(at(slot)) : List.of();
    }

    /** The first lock on the slot, or null. */
    public SlotLock lockOf(int slot) {
        return inRange(slot) && !at(slot).isEmpty() ? at(slot).get(0) : null;
    }

    public boolean isLocked(int slot) {
        return inRange(slot) && !at(slot).isEmpty();
    }

    private boolean blocked(int slot) {
        if (!inRange(slot)) {
            return false;
        }
        for (SlotLock lock : at(slot)) {
            if (lock.jobId() == acting) {
                return false;
            }
        }
        return !at(slot).isEmpty();
    }

    /** Places a lock on a slot: an IN lock replaces whatever the slot had, an OUT lock joins other OUT locks. */
    public void lock(int slot, SlotLock lock) {
        if (!inRange(slot)) {
            return;
        }
        List<SlotLock> slotLocks = at(slot);
        if (!counted && (lock.kind() == SlotLock.Kind.IN || slotLocks.stream().anyMatch(other -> other.kind() == SlotLock.Kind.IN))) {
            slotLocks.clear();
        }
        slotLocks.add(lock);
        onLocksChanged();
    }

    /** Releases the job's lock on one slot. */
    public void release(int slot, int jobId) {
        if (inRange(slot) && at(slot).removeIf(lock -> lock.jobId() == jobId)) {
            onLocksChanged();
        }
    }

    /** How many items the slot's locks of that kind reserve for jobs other than {@code exceptJob}. */
    public int reserved(int slot, SlotLock.Kind kind, int exceptJob) {
        int sum = 0;
        if (inRange(slot)) {
            for (SlotLock lock : at(slot)) {
                if (lock.kind() == kind && lock.jobId() != exceptJob) {
                    sum += lock.amount();
                }
            }
        }
        return sum;
    }

    /** Releases every lock of the job; true when it held one here. */
    public boolean release(int jobId) {
        return release(jobId, null);
    }

    private boolean release(int jobId, SlotLock.Kind kind) {
        boolean any = false;
        for (int slot = 0; slot < getSlots(); slot++) {
            any |= at(slot).removeIf(lock -> lock.jobId() == jobId && (kind == null || lock.kind() == kind));
        }
        if (any) {
            onLocksChanged();
        }
        return any;
    }

    /** Whether the job holds a lock here. */
    public boolean holds(int jobId) {
        for (int slot = 0; slot < getSlots(); slot++) {
            for (SlotLock lock : at(slot)) {
                if (lock.jobId() == jobId) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Every lock with its slot, in slot order. */
    public List<Locked> locked() {
        List<Locked> list = new ArrayList<>();
        for (int slot = 0; slot < getSlots(); slot++) {
            for (SlotLock lock : at(slot)) {
                list.add(new Locked(slot, lock));
            }
        }
        return list;
    }

    /** A locked slot and one of its locks. */
    public record Locked(int slot, SlotLock lock) {
    }

    /** How many items OUT locks reserve here, per item. */
    public Map<ItemKey, Integer> outLocked() {
        Map<ItemKey, Integer> amounts = new HashMap<>();
        for (Locked entry : locked()) {
            if (entry.lock().kind() == SlotLock.Kind.OUT) {
                amounts.merge(entry.lock().key(), entry.lock().amount(), Integer::sum);
            }
        }
        return amounts;
    }

    /** The slots as the planner sees them. */
    public List<SlotFill.Slot<ItemKey>> states() {
        List<SlotFill.Slot<ItemKey>> states = new ArrayList<>(getSlots());
        for (int slot = 0; slot < getSlots(); slot++) {
            ItemStack stack = getStackInSlot(slot);
            int reserved = 0;
            for (SlotLock lock : at(slot)) {
                if (lock.kind() == SlotLock.Kind.OUT) {
                    reserved += lock.amount();
                }
            }
            states.add(new SlotFill.Slot<>(stack.isEmpty() ? null : ItemKey.of(stack), stack.getCount(), getSlotLimit(slot),
                    isLocked(slot), reserved));
        }
        return states;
    }

    /**
     * Locks room for {@code amount} of the key for the job: partial stacks of
     * it first, then empty slots, or only empty slots with {@code emptyOnly}.
     * Returns how many it found room for.
     */
    public int lockIn(int jobId, ItemKey key, int amount, boolean emptyOnly) {
        int max = key.stack(1).getMaxStackSize();
        List<SlotFill.Part> parts = emptyOnly ? SlotFill.inEmpty(states(), amount, max) : SlotFill.in(states(), key, amount, max);
        return place(parts, SlotLock.Kind.IN, key, jobId);
    }

    /**
     * Reserves room for {@code amount} more of the key for a job that already
     * holds IN locks here: its own locks of the key grow as far as their slots
     * take, then free room is locked as by {@link #lockIn}. Returns how many it found room for.
     */
    public int extendIn(int jobId, ItemKey key, int amount) {
        int max = key.stack(1).getMaxStackSize();
        int left = amount;
        boolean grown = false;
        for (int slot = 0; slot < getSlots() && left > 0; slot++) {
            List<SlotLock> slotLocks = at(slot);
            if (slotLocks.size() != 1) {
                continue;
            }
            SlotLock lock = slotLocks.get(0);
            if (lock.jobId() != jobId || lock.kind() != SlotLock.Kind.IN || !lock.key().equals(key)) {
                continue;
            }
            int room = Math.min(max, getSlotLimit(slot)) - getStackInSlot(slot).getCount() - lock.amount();
            if (room > 0) {
                int more = Math.min(room, left);
                slotLocks.set(0, new SlotLock(SlotLock.Kind.IN, key, lock.amount() + more, jobId));
                left -= more;
                grown = true;
            }
        }
        if (grown) {
            onLocksChanged();
        }
        return amount - left + (left > 0 ? lockIn(jobId, key, left, false) : 0);
    }

    /** How many items the job's locks of that kind reserve here. */
    public int lockedAmount(int jobId, SlotLock.Kind kind) {
        int sum = 0;
        for (Locked entry : locked()) {
            if (entry.lock().jobId() == jobId && entry.lock().kind() == kind) {
                sum += entry.lock().amount();
            }
        }
        return sum;
    }

    /** Locks items of the key that no job has reserved yet for the job; returns how many it found. */
    public int lockOut(int jobId, ItemKey key, int amount) {
        return place(SlotFill.out(states(), key, amount), SlotLock.Kind.OUT, key, jobId);
    }

    private int place(List<SlotFill.Part> parts, SlotLock.Kind kind, ItemKey key, int jobId) {
        if (parts.isEmpty()) {
            return 0;
        }
        for (SlotFill.Part part : parts) {
            at(part.slot()).add(new SlotLock(kind, key, part.count(), jobId));
        }
        onLocksChanged();
        return SlotFill.total(parts);
    }

    /** How many of the key free slots take, a stack per slot. */
    public int room(ItemKey key) {
        return SlotFill.room(states(), key, key.stack(1).getMaxStackSize());
    }

    /** How many of the key the slots hold that no job has reserved. */
    public int available(ItemKey key) {
        return SlotFill.available(states(), key);
    }

    /** Unlocked empty slots. */
    public int freeSlots() {
        return SlotFill.freeSlots(states());
    }

    /** Runs {@code action} with the job allowed through its own locks. */
    private <T> T asJob(int jobId, Supplier<T> action) {
        int before = acting;
        acting = jobId;
        try {
            return action.get();
        } finally {
            acting = before;
        }
    }

    /**
     * Takes what the job's OUT locks reserve, one stack per item, and
     * releases them; for jobs that reserved several items here.
     */
    public List<ItemStack> extractAllAs(int jobId) {
        List<ItemStack> taken = asJob(jobId, () -> {
            List<ItemStack> stacks = new ArrayList<>();
            for (Locked entry : locked()) {
                SlotLock lock = entry.lock();
                if (lock.jobId() != jobId || lock.kind() != SlotLock.Kind.OUT || !lock.key().matches(getStackInSlot(entry.slot()))) {
                    continue;
                }
                merge(stacks, extractItem(entry.slot(), lock.amount(), false));
            }
            return stacks;
        });
        release(jobId, SlotLock.Kind.OUT);
        return taken;
    }

    private static void merge(List<ItemStack> stacks, ItemStack part) {
        if (part.isEmpty()) {
            return;
        }
        for (ItemStack stack : stacks) {
            if (ItemStack.isSameItemSameComponents(stack, part)) {
                stack.grow(part.getCount());
                return;
            }
        }
        stacks.add(part);
    }

    /**
     * Puts each stack into the job's IN locked slots of its own item and
     * releases the job's IN locks once all went in; returns what did not fit,
     * one entry per stack given.
     */
    public List<ItemStack> insertAllAs(int jobId, List<ItemStack> stacks) {
        List<ItemStack> left = asJob(jobId, () -> {
            List<ItemStack> rests = new ArrayList<>(stacks.size());
            for (ItemStack stack : stacks) {
                ItemStack rest = stack;
                for (Locked entry : locked()) {
                    if (rest.isEmpty()) {
                        break;
                    }
                    SlotLock lock = entry.lock();
                    if (lock.jobId() == jobId && lock.kind() == SlotLock.Kind.IN && lock.key().matches(rest)) {
                        rest = insertItem(entry.slot(), rest, false);
                    }
                }
                rests.add(rest);
            }
            return rests;
        });
        release(jobId, SlotLock.Kind.IN);
        return left;
    }

    /** Puts the stack into one slot as the job and releases the job's IN lock there; returns what did not fit. */
    public ItemStack insertAs(int jobId, int slot, ItemStack stack) {
        ItemStack left = asJob(jobId, () -> insertItem(slot, stack, false));
        release(slot, jobId, SlotLock.Kind.IN);
        return left;
    }

    /** Takes up to {@code amount} out of one slot as the job and releases the job's OUT lock there. */
    public ItemStack extractAs(int jobId, int slot, int amount) {
        ItemStack taken = asJob(jobId, () -> extractItem(slot, amount, false));
        release(slot, jobId, SlotLock.Kind.OUT);
        return taken;
    }

    private void release(int slot, int jobId, SlotLock.Kind kind) {
        if (inRange(slot) && at(slot).removeIf(lock -> lock.jobId() == jobId && lock.kind() == kind)) {
            onLocksChanged();
        }
    }

    /** Takes what the job's OUT locks reserve and releases them. */
    public ItemStack extractAs(int jobId) {
        ItemStack result = asJob(jobId, () -> {
            ItemStack taken = ItemStack.EMPTY;
            for (Locked entry : locked()) {
                SlotLock lock = entry.lock();
                if (lock.jobId() != jobId || lock.kind() != SlotLock.Kind.OUT || !lock.key().matches(getStackInSlot(entry.slot()))) {
                    continue;
                }
                ItemStack part = extractItem(entry.slot(), lock.amount(), false);
                if (taken.isEmpty()) {
                    taken = part;
                } else if (!part.isEmpty()) {
                    taken.grow(part.getCount());
                }
            }
            return taken;
        });
        release(jobId, SlotLock.Kind.OUT);
        return result;
    }

    /** Puts the stack into the job's IN locked slots and releases them; returns what did not fit. */
    public ItemStack insertAs(int jobId, ItemStack stack) {
        ItemStack left = asJob(jobId, () -> {
            ItemStack rest = stack;
            for (Locked entry : locked()) {
                if (rest.isEmpty()) {
                    break;
                }
                if (entry.lock().jobId() == jobId && entry.lock().kind() == SlotLock.Kind.IN) {
                    rest = insertItem(entry.slot(), rest, false);
                }
            }
            return rest;
        });
        release(jobId, SlotLock.Kind.IN);
        return left;
    }

    /**
     * Puts the stack into the job's IN locked slots and keeps the locks, for a
     * job that fills its slots in several parts; returns what did not fit.
     */
    public ItemStack fillAs(int jobId, ItemStack stack) {
        return asJob(jobId, () -> {
            ItemStack rest = stack;
            for (Locked entry : locked()) {
                if (rest.isEmpty()) {
                    break;
                }
                if (entry.lock().jobId() == jobId && entry.lock().kind() == SlotLock.Kind.IN) {
                    rest = insertItem(entry.slot(), rest, false);
                }
            }
            return rest;
        });
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (counted) {
            int room = inRange(index) ? SlotFill.sharedRoom(getCapacity(index, resource), getStackInSlot(index).getCount(),
                    reserved(index, SlotLock.Kind.IN, acting)) : 0;
            return room <= 0 ? 0 : super.insert(index, resource, Math.min(amount, room), transaction);
        }
        return blocked(index) && !takesTopUp(index, resource) ? 0 : super.insert(index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (counted) {
            int available = inRange(index) ? SlotFill.sharedAvailable(getStackInSlot(index).getCount(),
                    reserved(index, SlotLock.Kind.OUT, acting)) : 0;
            return available <= 0 ? 0 : super.extract(index, resource, Math.min(amount, available), transaction);
        }
        return blocked(index) ? 0 : super.extract(index, resource, amount, transaction);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return (counted || !blocked(index) || takesTopUp(index, resource)) && super.isValid(index, resource);
    }

    /** The locks as a list tag; empty when nothing is locked. */
    public ListTag saveLocks(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Locked entry : locked()) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Slot", entry.slot());
            tag.putString("Kind", entry.lock().kind().name());
            tag.put("Item", ValueIo.saveStack(registries, entry.lock().key().stack(1)));
            tag.putInt("Amount", entry.lock().amount());
            tag.putInt("Job", entry.lock().jobId());
            list.add(tag);
        }
        return list;
    }

    /** Replaces the locks with those of a tag from {@link #saveLocks}; unreadable entries are skipped. */
    public void loadLocks(HolderLookup.Provider registries, ListTag list) {
        List<Locked> read = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompoundOrEmpty(i);
            int job = tag.getIntOr("Job", 0);
            ItemStack item = ValueIo.parseStack(registries, tag.getCompoundOrEmpty("Item"));
            SlotLock.Kind kind = kindOf(tag.getStringOr("Kind", ""));
            if (kind != null && job > 0 && !item.isEmpty()) {
                read.add(new Locked(tag.getIntOr("Slot", -1), new SlotLock(kind, ItemKey.of(item), tag.getIntOr("Amount", 0), job)));
            }
        }
        copyLocks(read);
        onLocksChanged();
    }

    private static SlotLock.Kind kindOf(String name) {
        for (SlotLock.Kind kind : SlotLock.Kind.values()) {
            if (kind.name().equals(name)) {
                return kind;
            }
        }
        return null;
    }

    /** Replaces the locks with these, for the client copy a menu shows. */
    public void copyLocks(List<Locked> from) {
        for (int slot = 0; slot < getSlots(); slot++) {
            at(slot).clear();
        }
        for (Locked entry : from) {
            if (inRange(entry.slot())) {
                at(entry.slot()).add(entry.lock());
            }
        }
    }
}
