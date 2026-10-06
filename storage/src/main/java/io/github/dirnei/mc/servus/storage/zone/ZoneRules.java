package io.github.dirnei.mc.servus.storage.zone;

import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The put-away rules of one warehouse, keyed by zone key: filter items, item
 * tags and the overflow flag. Lives on the controller; hands a pure snapshot
 * to {@link PutAwayPlan}.
 */
public final class ZoneRules {

    public static final int FILTER_SLOTS = 9;

    /** One zone's editable rule. */
    public final class Entry {
        private final String key;
        private String display;
        private final ItemStackHandler filter = new ItemStackHandler(FILTER_SLOTS) {
            @Override
            protected void onContentsChanged(int slot) {
                changed();
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
        private final LinkedHashSet<String> tags = new LinkedHashSet<>();
        private boolean overflow;

        private Entry(String key, String display) {
            this.key = key;
            this.display = display;
        }

        public String key() {
            return key;
        }

        public String display() {
            return display;
        }

        public void setDisplay(String display) {
            if (!display.isEmpty() && !display.equals(this.display)) {
                this.display = display;
                changed();
            }
        }

        /** Ghost item slots; counts are always one. */
        public IItemHandlerModifiable filter() {
            return filter;
        }

        public List<String> tags() {
            return List.copyOf(tags);
        }

        /** Tags as one comma separated line for the GUI. */
        public String tagLine() {
            return String.join(", ", tags);
        }

        public boolean overflow() {
            return overflow;
        }

        /** Whether this rule says nothing: no items, no tags, not overflow. */
        public boolean isEmpty() {
            if (overflow || !tags.isEmpty()) {
                return false;
            }
            for (int i = 0; i < filter.getSlots(); i++) {
                if (!filter.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        private PutAwayPlan.Rule<ItemKey> rule() {
            Set<ItemKey> items = new HashSet<>();
            for (int i = 0; i < filter.getSlots(); i++) {
                ItemStack stack = filter.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    items.add(ItemKey.of(stack));
                }
            }
            return new PutAwayPlan.Rule<>(items, Set.copyOf(tags), overflow);
        }
    }

    private final Map<String, Entry> entries = new TreeMap<>();
    private Runnable listener = () -> { };
    private Map<String, PutAwayPlan.Rule<ItemKey>> snapshot;

    /** Called after every edit, for persistence and for waking the network. */
    public void onChanged(Runnable listener) {
        this.listener = listener;
    }

    private void changed() {
        snapshot = null;
        listener.run();
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    /** The rule of a zone, or null when none was set. */
    public Entry get(String key) {
        return entries.get(key);
    }

    /** The rule of a zone, created empty when missing. */
    public Entry entry(String key, String display) {
        String zoneKey = ZoneNames.key(key);
        Entry entry = entries.get(zoneKey);
        if (entry == null) {
            entry = new Entry(zoneKey, display.isEmpty() ? zoneKey : ZoneNames.display(display));
            entries.put(zoneKey, entry);
            changed();
        }
        return entry;
    }

    public void remove(String key) {
        if (entries.remove(ZoneNames.key(key)) != null) {
            changed();
        }
    }

    /** Flags one zone as the overflow zone, unflagging every other, or clears the flag. */
    public void setOverflow(String key, boolean flag) {
        Entry target = entry(key, "");
        boolean any = false;
        for (Entry entry : entries.values()) {
            boolean wanted = flag && entry == target;
            if (entry.overflow != wanted) {
                entry.overflow = wanted;
                any = true;
            }
        }
        if (any) {
            changed();
        }
    }

    /** The overflow zone key, or null. */
    public String overflowKey() {
        for (Entry entry : entries.values()) {
            if (entry.overflow) {
                return entry.key;
            }
        }
        return null;
    }

    /**
     * Sets a zone's tags from a comma separated line. Entries that are not
     * resource locations are dropped; a leading '#' is allowed.
     */
    public void setTags(String key, String line) {
        Entry entry = entry(key, "");
        LinkedHashSet<String> parsed = parseTags(line);
        if (!parsed.equals(entry.tags)) {
            entry.tags.clear();
            entry.tags.addAll(parsed);
            changed();
        }
    }

    public static LinkedHashSet<String> parseTags(String line) {
        LinkedHashSet<String> parsed = new LinkedHashSet<>();
        if (line == null) {
            return parsed;
        }
        for (String part : line.split(",")) {
            String text = part.trim();
            if (text.startsWith("#")) {
                text = text.substring(1);
            }
            if (text.isEmpty()) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(text);
            if (id != null) {
                parsed.add(id.toString());
            }
        }
        return parsed;
    }

    /** The pure rules for routing, rebuilt after edits. Entries that say nothing are left out. */
    public Map<String, PutAwayPlan.Rule<ItemKey>> snapshot() {
        if (snapshot == null) {
            Map<String, PutAwayPlan.Rule<ItemKey>> map = new HashMap<>();
            for (Entry entry : entries.values()) {
                if (!entry.isEmpty()) {
                    map.put(entry.key, entry.rule());
                }
            }
            snapshot = Collections.unmodifiableMap(map);
        }
        return snapshot;
    }

    // Persistence

    public CompoundTag save(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : entries.values()) {
            if (entry.isEmpty()) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            tag.putString("Name", entry.display);
            tag.put("Items", entry.filter.serializeNBT(registries));
            ListTag tags = new ListTag();
            for (String tagId : entry.tags) {
                tags.add(StringTag.valueOf(tagId));
            }
            tag.put("Tags", tags);
            tag.putBoolean("Overflow", entry.overflow);
            list.add(tag);
        }
        CompoundTag root = new CompoundTag();
        root.put("Zones", list);
        return root;
    }

    public void load(CompoundTag root, HolderLookup.Provider registries) {
        entries.clear();
        for (Tag element : root.getList("Zones", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) element;
            String display = ZoneNames.display(tag.getString("Name"));
            String key = ZoneNames.key(display);
            if (key.isEmpty()) {
                continue;
            }
            Entry entry = new Entry(key, display);
            if (tag.contains("Items")) {
                ItemStackHandler saved = new ItemStackHandler();
                saved.deserializeNBT(registries, tag.getCompound("Items"));
                for (int i = 0; i < Math.min(saved.getSlots(), FILTER_SLOTS); i++) {
                    entry.filter.setStackInSlot(i, saved.getStackInSlot(i));
                }
            }
            List<String> tags = new ArrayList<>();
            for (Tag tagId : tag.getList("Tags", Tag.TAG_STRING)) {
                tags.add(tagId.getAsString());
            }
            entry.tags.addAll(tags);
            entry.overflow = tag.getBoolean("Overflow");
            entries.put(key, entry);
        }
        snapshot = null;
    }
}
