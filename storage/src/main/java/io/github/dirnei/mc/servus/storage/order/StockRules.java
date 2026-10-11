package io.github.dirnei.mc.servus.storage.order;

import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** The stock rules of one warehouse, one per item, kept on the controller. */
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
            tag.put("Item", ValueIo.saveStack(registries, rule.key().stack(1)));
            tag.putInt("Min", rule.min());
            tag.putInt("Max", rule.max());
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
            ItemStack stack = ValueIo.parseStack(registries, tag.getCompoundOrEmpty("Item"));
            if (!stack.isEmpty()) {
                StockRule rule = new StockRule(ItemKey.of(stack), tag.getIntOr("Min", 0), tag.getIntOr("Max", 0));
                rules.put(rule.key(), rule);
            }
        }
    }
}
