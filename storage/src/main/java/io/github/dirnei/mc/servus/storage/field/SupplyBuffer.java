package io.github.dirnei.mc.servus.storage.field;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The supply buffer of a Field Head: one part each for seeds, fertilizer and
 * anti-corrosion items, each one item and a count. Counts are plain ints, so
 * a level from data may exceed a stack. Which item a part may hold is the
 * head's decision; the buffer only keeps one item per part.
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

    private final Item[] items = new Item[Part.VALUES.length];
    private final int[] counts = new int[Part.VALUES.length];
    private final Runnable onChange;

    /** @param onChange called after every change, to mark the owner dirty */
    public SupplyBuffer(Runnable onChange) {
        this.onChange = onChange;
    }

    /** The item the part holds, null while it is empty. */
    public Item item(Part part) {
        return items[part.ordinal()];
    }

    public int count(Part part) {
        return counts[part.ordinal()];
    }

    /** Whether the part is empty or holds that item. */
    public boolean accepts(Part part, Item item) {
        return items[part.ordinal()] == null || items[part.ordinal()] == item;
    }

    /**
     * Puts up to count of the item into the part, never above the level and
     * never beside another item. Returns how many went in.
     */
    public int insert(Part part, Item item, int count, int level) {
        int index = part.ordinal();
        if (item == null || count <= 0 || !accepts(part, item)) {
            return 0;
        }
        int added = Math.min(count, Math.max(0, level - counts[index]));
        if (added <= 0) {
            return 0;
        }
        items[index] = item;
        counts[index] += added;
        onChange.run();
        return added;
    }

    /** Takes up to amount out of the part; returns how many came out. */
    public int take(Part part, int amount) {
        int index = part.ordinal();
        int taken = Math.min(Math.max(0, amount), counts[index]);
        if (taken <= 0) {
            return 0;
        }
        counts[index] -= taken;
        if (counts[index] == 0) {
            items[index] = null;
        }
        onChange.run();
        return taken;
    }

    /** Empties the part and returns what it held as stacks of at most a stack each. */
    public List<ItemStack> takeAll(Part part) {
        Item item = items[part.ordinal()];
        int count = take(part, counts[part.ordinal()]);
        return stacks(item, count);
    }

    /** Empties every part, for dropping when the head breaks. */
    public List<ItemStack> takeEverything() {
        List<ItemStack> all = new ArrayList<>();
        for (Part part : Part.VALUES) {
            all.addAll(takeAll(part));
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

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (Part part : Part.VALUES) {
            Item item = items[part.ordinal()];
            if (item != null) {
                CompoundTag entry = new CompoundTag();
                entry.putString("Item", BuiltInRegistries.ITEM.getKey(item).toString());
                entry.putInt("Count", counts[part.ordinal()]);
                tag.put(part.key(), entry);
            }
        }
        return tag;
    }

    /** Reads the parts saved by {@link #save()}; parts with an unknown item stay empty. */
    public void load(CompoundTag tag) {
        for (Part part : Part.VALUES) {
            int index = part.ordinal();
            CompoundTag entry = tag.getCompoundOrEmpty(part.key());
            Identifier id = Identifier.tryParse(entry.getStringOr("Item", ""));
            Optional<Item> item = id == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(id);
            int count = Math.max(0, entry.getIntOr("Count", 0));
            items[index] = item.isPresent() && count > 0 ? item.get() : null;
            counts[index] = items[index] == null ? 0 : count;
        }
    }
}
