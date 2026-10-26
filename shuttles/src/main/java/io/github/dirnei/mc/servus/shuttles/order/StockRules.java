package io.github.dirnei.mc.servus.shuttles.order;

import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * The stock rules of one warehouse, one per item, kept on the controller, and
 * the Outbound Station and keep slot holding each export. Min and Max are
 * saved only for a Make pair, Cap and Export only when set.
 */
public final class StockRules {

    private final Map<ItemKey, StockRule> rules = new LinkedHashMap<>();
    /** Which Outbound Station and keep slot holds each export. */
    private final ExportBook<ItemKey, BlockPos> exports = new ExportBook<>();
    private Runnable listener = () -> { };

    public void onChanged(Runnable listener) {
        this.listener = listener;
    }

    public List<StockRule> all() {
        return Collections.unmodifiableList(new ArrayList<>(rules.values()));
    }

    public StockRule get(ItemKey key) {
        return rules.get(key);
    }

    /** Sets the rule of its item; a rule without an export frees the station that held it. */
    public void set(StockRule rule) {
        rules.put(rule.key(), rule);
        if (!rule.hasExport()) {
            exports.clear(rule.key());
        }
        listener.run();
    }

    /** Removes the rule and frees the station that held its export. */
    public boolean remove(ItemKey key) {
        boolean removed = rules.remove(key) != null;
        exports.clear(key);
        if (removed) {
            listener.run();
        }
        return removed;
    }

    /** The station and keep slot holding the item's export, empty when none does. */
    public Optional<ExportBook.Claim<BlockPos>> exportHolder(ItemKey key) {
        return exports.holder(key);
    }

    /**
     * Gives the item's export to the station's keep slot, taking it from a
     * station that held it. Refused without an export or when another export
     * occupies the slot.
     */
    public boolean claimExport(ItemKey key, BlockPos station, int slot) {
        StockRule rule = rules.get(key);
        if (rule == null || !rule.hasExport() || !exports.claim(key, station.immutable(), slot)) {
            return false;
        }
        listener.run();
        return true;
    }

    /** Frees a station's keep slot; returns the export that held it. */
    public Optional<ItemKey> releaseExport(BlockPos station, int slot) {
        Optional<ItemKey> released = exports.releaseSlot(station, slot);
        released.ifPresent(key -> listener.run());
        return released;
    }

    /** Frees every keep slot of a station, as when it leaves the warehouse. */
    public void releaseStation(BlockPos station) {
        if (!exports.releaseStation(station).isEmpty()) {
            listener.run();
        }
    }

    /** The exports a station holds, by keep slot. */
    public Map<Integer, ItemKey> exportsAt(BlockPos station) {
        return exports.slotsOf(station);
    }

    /** The stations holding exports, with the claims. */
    public Map<ItemKey, ExportBook.Claim<BlockPos>> exportClaims() {
        return exports.all();
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (StockRule rule : rules.values()) {
            CompoundTag tag = new CompoundTag();
            tag.put("Item", Saving.key(rule.key(), registries));
            if (rule.hasMake()) {
                tag.putInt("Min", rule.make().min());
                tag.putInt("Max", rule.make().max());
            }
            if (rule.hasCap()) {
                tag.putInt("Cap", rule.cap());
            }
            if (rule.hasExport()) {
                tag.putInt("Export", rule.export());
                exports.holder(rule.key()).ifPresent(claim -> {
                    tag.putLong("ExportStation", claim.station().asLong());
                    tag.putInt("ExportSlot", claim.slot());
                });
            }
            list.add(tag);
        }
        CompoundTag root = new CompoundTag();
        root.put("Rules", list);
        return root;
    }

    public void load(CompoundTag root, HolderLookup.Provider registries) {
        rules.clear();
        for (ItemKey key : List.copyOf(exports.all().keySet())) {
            exports.clear(key);
        }
        for (Tag element : root.getListOrEmpty("Rules")) {
            CompoundTag tag = (CompoundTag) element;
            Saving.key(tag.getCompoundOrEmpty("Item"), registries).ifPresent(key -> {
                StockRule.Make make = tag.contains("Max") ? new StockRule.Make(tag.getIntOr("Min", 0), tag.getIntOr("Max", 0)) : null;
                StockRule rule = new StockRule(key, make, tag.getIntOr("Cap", 0), tag.getIntOr("Export", -1));
                if (rule.empty()) {
                    return;
                }
                rules.put(key, rule);
                if (rule.hasExport() && tag.contains("ExportStation")) {
                    exports.claim(key, BlockPos.of(tag.getLongOr("ExportStation", 0)), tag.getIntOr("ExportSlot", 0));
                }
            });
        }
    }
}
