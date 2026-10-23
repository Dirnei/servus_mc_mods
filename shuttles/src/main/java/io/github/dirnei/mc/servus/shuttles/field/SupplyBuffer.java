package io.github.dirnei.mc.servus.shuttles.field;

import io.github.dirnei.mc.servus.shuttles.lock.LockedSlots;
import io.github.dirnei.mc.servus.shuttles.lock.SlotLock;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * The supply buffer of a Field Head: one part each for seeds, fertilizer and
 * anti-corrosion items, each one item and a count. Counts may exceed a stack,
 * since a level from data may. Which item a part may hold is the head's
 * decision; the buffer only keeps one item per part.
 *
 * <p>Each part is a counted slot of {@link #slots()}: shuttle jobs reserve
 * room (IN) and items (OUT) in it, and everyone else sees the part less what
 * jobs reserved.
 */
public final class SupplyBuffer {

    /** The parts of the buffer. */
    public enum Part {
        SEEDS, FERTILIZER, ANTI_CORROSION;

        public static final Part[] VALUES = values();

        String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final Runnable onChange;
    private boolean quiet;
    private final LockedSlots slots = new LockedSlots(Part.VALUES.length, true) {
        @Override
        protected void onContentsChanged(int slot) {
            changed();
        }

        @Override
        protected void onLocksChanged() {
            changed();
        }

        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return Integer.MAX_VALUE;
        }
    };

    /** @param onChange called after every change, to mark the owner dirty */
    public SupplyBuffer(Runnable onChange) {
        this.onChange = onChange;
    }

    private void changed() {
        if (!quiet) {
            onChange.run();
        }
    }

    /** The parts as lockable slots, in {@link Part} order. */
    public LockedSlots slots() {
        return slots;
    }

    /** The item the part holds, null while it is empty. */
    public Item item(Part part) {
        ItemStack stack = slots.getStackInSlot(part.ordinal());
        return stack.isEmpty() ? null : stack.getItem();
    }

    public int count(Part part) {
        return slots.getStackInSlot(part.ordinal()).getCount();
    }

    /** Whether the part is empty or holds that item. */
    public boolean accepts(Part part, Item item) {
        Item held = item(part);
        return held == null || held == item;
    }

    /** Room jobs reserved in the part, leaving out the job {@code exceptJob}. */
    public int reservedIn(Part part, int exceptJob) {
        return slots.reserved(part.ordinal(), SlotLock.Kind.IN, exceptJob);
    }

    /** Items of the part no job reserved. */
    public int available(Part part) {
        return Math.max(0, count(part) - slots.reserved(part.ordinal(), SlotLock.Kind.OUT, 0));
    }

    /**
     * Puts up to count of the item into the part, never above the level less
     * the room jobs reserved and never beside another item. Returns how many went in.
     */
    public int insert(Part part, Item item, int count, int level) {
        return insert(0, part, item, count, level);
    }

    /** As {@link #insert}, for a job that reserved room here: its own room counts, and its IN lock on the part is released. */
    public int insertAs(int jobId, Part part, Item item, int count, int level) {
        int added = insert(jobId, part, item, count, level);
        slots.release(part.ordinal(), jobId);
        return added;
    }

    private int insert(int jobId, Part part, Item item, int count, int level) {
        if (item == null || count <= 0 || !accepts(part, item)) {
            return 0;
        }
        int amount = Math.min(count, Math.max(0, level - count(part) - reservedIn(part, jobId)));
        if (amount <= 0) {
            return 0;
        }
        ItemStack stack = new ItemStack(item, amount);
        ItemStack left = jobId == 0 ? slots.insertItem(part.ordinal(), stack, false) : slots.insertAs(jobId, part.ordinal(), stack);
        return amount - left.getCount();
    }

    /** Takes up to amount of the items no job reserved out of the part; returns how many came out. */
    public int take(Part part, int amount) {
        return amount <= 0 ? 0 : slots.extractItem(part.ordinal(), amount, false).getCount();
    }

    /** Takes up to amount out of the part for a job that reserved items here, and releases its OUT lock on the part. */
    public int takeAs(int jobId, Part part, int amount) {
        return slots.extractAs(jobId, part.ordinal(), Math.max(0, amount)).getCount();
    }

    /** Reserves room for {@code amount} of the item in the part for the job. */
    public void lockIn(int jobId, Part part, Item item, int amount) {
        if (item != null && amount > 0) {
            slots.lock(part.ordinal(), new SlotLock(SlotLock.Kind.IN, ItemKey.of(new ItemStack(item)), amount, jobId));
        }
    }

    /** Reserves up to {@code amount} of the part's items no job reserved for the job; returns how many it reserved. */
    public int lockOut(int jobId, Part part, int amount) {
        Item item = item(part);
        int reserved = Math.min(amount, available(part));
        if (item == null || reserved <= 0) {
            return 0;
        }
        slots.lock(part.ordinal(), new SlotLock(SlotLock.Kind.OUT, ItemKey.of(new ItemStack(item)), reserved, jobId));
        return reserved;
    }

    /** Releases every lock of the job on the buffer. */
    public void release(int jobId) {
        slots.release(jobId);
    }

    /** Empties the part of what no job reserved and returns it as stacks of at most a stack each. */
    public List<ItemStack> takeAll(Part part) {
        Item item = item(part);
        return stacks(item, take(part, count(part)));
    }

    /** Empties every part, locks or not, for dropping when the head breaks. */
    public List<ItemStack> takeEverything() {
        List<ItemStack> all = new ArrayList<>();
        for (Part part : Part.VALUES) {
            all.addAll(stacks(item(part), count(part)));
            slots.setStackInSlot(part.ordinal(), ItemStack.EMPTY);
        }
        return all;
    }

    private static List<ItemStack> stacks(Item item, int count) {
        List<ItemStack> stacks = new ArrayList<>();
        if (item == null) {
            return stacks;
        }
        int max = Math.max(1, new ItemStack(item).getMaxStackSize());
        for (int left = count; left > 0; left -= max) {
            stacks.add(new ItemStack(item, Math.min(max, left)));
        }
        return stacks;
    }

    /** The parts by item id and count, and the locks under {@code Locks} while there are any. */
    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        for (Part part : Part.VALUES) {
            Item item = item(part);
            if (item != null) {
                CompoundTag entry = new CompoundTag();
                entry.putString("Item", BuiltInRegistries.ITEM.getKey(item).toString());
                entry.putInt("Count", count(part));
                tag.put(part.key(), entry);
            }
        }
        ListTag locks = slots.saveLocks(registries);
        if (!locks.isEmpty()) {
            tag.put("Locks", locks);
        }
        return tag;
    }

    /** Reads the parts saved by {@link #save}; parts with an unknown item stay empty, a tag without locks loads unlocked. */
    public void load(HolderLookup.Provider registries, CompoundTag tag) {
        quiet = true;
        try {
            for (Part part : Part.VALUES) {
                CompoundTag entry = tag.getCompoundOrEmpty(part.key());
                Identifier id = Identifier.tryParse(entry.getStringOr("Item", ""));
                Optional<Item> item = id == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(id);
                int count = Math.max(0, entry.getIntOr("Count", 0));
                slots.setStackInSlot(part.ordinal(), item.isPresent() && count > 0 ? new ItemStack(item.get(), count) : ItemStack.EMPTY);
            }
            slots.loadLocks(registries, tag.getListOrEmpty("Locks"));
        } finally {
            quiet = false;
        }
    }
}
