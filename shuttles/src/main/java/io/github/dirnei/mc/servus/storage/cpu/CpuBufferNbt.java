package io.github.dirnei.mc.servus.storage.cpu;

import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.order.Saving;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Saves and loads a CPU's buffer and turns its entries into item stacks. */
final class CpuBufferNbt {

    private CpuBufferNbt() {
    }

    static CompoundTag save(CpuBuffer<ItemKey> buffer, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ListTag entries = new ListTag();
        for (CpuBuffer.Entry<ItemKey> entry : buffer.entries()) {
            CompoundTag item = new CompoundTag();
            item.putLong("Order", entry.order());
            item.put("Item", Saving.key(entry.key(), registries));
            item.putInt("Count", entry.count());
            item.putInt("MaxStack", entry.maxStack());
            item.putBoolean("Leaving", entry.leaving());
            entries.add(item);
        }
        ListTag arrivals = new ListTag();
        for (CpuBuffer.Arrival<ItemKey> arrival : buffer.arrivals()) {
            CompoundTag item = new CompoundTag();
            item.putLong("Transfer", arrival.transfer());
            item.putLong("Order", arrival.order());
            item.put("Item", Saving.key(arrival.key(), registries));
            item.putInt("Expected", arrival.expected());
            item.putInt("Landed", arrival.landed());
            item.putInt("MaxStack", arrival.maxStack());
            arrivals.add(item);
        }
        tag.put("Entries", entries);
        tag.put("Arrivals", arrivals);
        return tag;
    }

    static CpuBuffer<ItemKey> load(CompoundTag tag, int capacity, HolderLookup.Provider registries) {
        CpuBuffer<ItemKey> loaded = new CpuBuffer<>(capacity);
        for (Tag raw : tag.getListOrEmpty("Entries")) {
            CompoundTag item = (CompoundTag) raw;
            Saving.key(item.getCompoundOrEmpty("Item"), registries).ifPresent(key -> loaded.restore(new CpuBuffer.Entry<>(
                    item.getLongOr("Order", 0), key, item.getIntOr("Count", 0), item.getIntOr("MaxStack", 64),
                    item.getBooleanOr("Leaving", false))));
        }
        for (Tag raw : tag.getListOrEmpty("Arrivals")) {
            CompoundTag item = (CompoundTag) raw;
            Saving.key(item.getCompoundOrEmpty("Item"), registries).ifPresent(key -> loaded.restore(new CpuBuffer.Arrival<>(
                    item.getLongOr("Transfer", 0), item.getLongOr("Order", 0), key, item.getIntOr("Expected", 0),
                    item.getIntOr("Landed", 0), item.getIntOr("MaxStack", 64))));
        }
        return loaded;
    }

    /** The items of buffer entries as stacks of at most their max stack size. */
    static List<ItemStack> stacks(List<CpuBuffer.Entry<ItemKey>> entries) {
        List<ItemStack> list = new ArrayList<>();
        for (CpuBuffer.Entry<ItemKey> entry : entries) {
            int left = entry.count();
            while (left > 0) {
                int count = Math.min(left, Math.max(1, entry.maxStack()));
                list.add(entry.key().stack(count));
                left -= count;
            }
        }
        return list;
    }
}
