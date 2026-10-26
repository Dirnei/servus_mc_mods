package io.github.dirnei.mc.servus.shuttles.order;

import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** The stock rules of one warehouse, one per item, kept on the controller. Min and Max are saved only for a Make pair, Cap only when set. */
public final class StockRules {

    private final Map<ItemKey, StockRule> rules = new LinkedHashMap<>();
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

    public void set(StockRule rule) {
        rules.put(rule.key(), rule);
        listener.run();
    }

    public boolean remove(ItemKey key) {
        boolean removed = rules.remove(key) != null;
        if (removed) {
            listener.run();
        }
        return removed;
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
            list.add(tag);
        }
        CompoundTag root = new CompoundTag();
        root.put("Rules", list);
        return root;
    }

    public void load(CompoundTag root, HolderLookup.Provider registries) {
        rules.clear();
        for (Tag element : root.getListOrEmpty("Rules")) {
            CompoundTag tag = (CompoundTag) element;
            Saving.key(tag.getCompoundOrEmpty("Item"), registries).ifPresent(key -> {
                StockRule.Make make = tag.contains("Max") ? new StockRule.Make(tag.getIntOr("Min", 0), tag.getIntOr("Max", 0)) : null;
                StockRule rule = new StockRule(key, make, tag.getIntOr("Cap", 0));
                if (!rule.empty()) {
                    rules.put(key, rule);
                }
            });
        }
    }
}
