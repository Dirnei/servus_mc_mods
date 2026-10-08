package io.github.dirnei.mc.servus.storage.order;

import io.github.dirnei.mc.servus.storage.network.ItemKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** NBT helpers for saving work orders and their steps. */
final class Saving {

    private Saving() {
    }

    static CompoundTag key(ItemKey key, HolderLookup.Provider registries) {
        return (CompoundTag) key.stack(1).save(registries);
    }

    static Optional<ItemKey> key(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack stack = ItemStack.parseOptional(registries, tag);
        return stack.isEmpty() ? Optional.empty() : Optional.of(ItemKey.of(stack));
    }

    static ListTag stacks(List<ItemStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                list.add(stack.save(registries));
            }
        }
        return list;
    }

    static List<ItemStack> stacks(ListTag list, HolderLookup.Provider registries) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Tag raw : list) {
            ItemStack stack = ItemStack.parseOptional(registries, (CompoundTag) raw);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    static ListTag counts(Map<ItemKey, Integer> counts, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        counts.forEach((key, count) -> {
            CompoundTag entry = new CompoundTag();
            entry.put("Item", key(key, registries));
            entry.putInt("Count", count);
            list.add(entry);
        });
        return list;
    }

    static Map<ItemKey, Integer> counts(ListTag list, HolderLookup.Provider registries) {
        Map<ItemKey, Integer> counts = new LinkedHashMap<>();
        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;
            key(entry.getCompound("Item"), registries).ifPresent(key -> counts.merge(key, entry.getInt("Count"), Integer::sum));
        }
        return counts;
    }

    static <E extends Enum<E>> E state(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
