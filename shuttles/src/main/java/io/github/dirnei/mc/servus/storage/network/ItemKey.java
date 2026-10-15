package io.github.dirnei.mc.servus.storage.network;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Identity of a stack for indexing: item plus its component patch. */
public record ItemKey(Item item, DataComponentPatch components) {

    public static ItemKey of(ItemStack stack) {
        return new ItemKey(stack.getItem(), stack.getComponentsPatch());
    }

    /** The counts of the stacks merged by key, in first seen order. */
    public static Map<ItemKey, Integer> counts(List<ItemStack> stacks) {
        Map<ItemKey, Integer> counts = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            counts.merge(of(stack), stack.getCount(), Integer::sum);
        }
        return counts;
    }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == item && stack.getComponentsPatch().equals(components);
    }

    public ItemStack stack(int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.applyComponents(components);
        return stack;
    }
}
