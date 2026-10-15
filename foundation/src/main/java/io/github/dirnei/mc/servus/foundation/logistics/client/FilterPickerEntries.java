package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.client.ItemPicker;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The entries of the duct filter picker: a "none" entry and every registered item but air, built once per client session. */
final class FilterPickerEntries {

    private static List<ItemPicker.Entry> entries;

    private FilterPickerEntries() {
    }

    static List<ItemPicker.Entry> get() {
        if (entries == null) {
            entries = build();
        }
        return entries;
    }

    private static List<ItemPicker.Entry> build() {
        List<ItemPicker.Entry> list = new ArrayList<>();
        list.add(ItemPicker.Entry.none(Component.translatable("gui.servus_foundation.filter_none")));
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            list.add(new ItemPicker.Entry(id, stack, nameOf(stack, id)));
        }
        return List.copyOf(list);
    }

    /** The item's name, or its id when a mod's item has no usable name. */
    private static Component nameOf(ItemStack stack, String id) {
        try {
            Component name = stack.getHoverName();
            if (!name.getString().isBlank()) {
                return name;
            }
        } catch (RuntimeException e) {
            // a broken modded name must not keep the picker from opening
        }
        return Component.literal(id);
    }
}
