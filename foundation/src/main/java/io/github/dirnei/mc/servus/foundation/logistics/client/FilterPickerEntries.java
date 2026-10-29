package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.client.ItemPicker;
import io.github.dirnei.mc.servus.foundation.client.PickerEntries;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The entries of the duct filter pickers, built once per client session: a "none" entry and every registered
 * item but air, or a "none" entry and every registered source fluid, each fluid once.
 */
final class FilterPickerEntries {

    private static List<ItemPicker.Entry> entries;
    private static List<ItemPicker.Entry> fluidEntries;

    private FilterPickerEntries() {
    }

    static List<ItemPicker.Entry> get() {
        if (entries == null) {
            entries = build();
        }
        return entries;
    }

    static List<ItemPicker.Entry> fluids() {
        if (fluidEntries == null) {
            fluidEntries = buildFluids();
        }
        return fluidEntries;
    }

    private static List<ItemPicker.Entry> buildFluids() {
        List<ItemPicker.Entry> list = new ArrayList<>();
        list.add(ItemPicker.Entry.none(Component.translatable("gui.servus_foundation.filter_none")));
        list.addAll(PickerEntries.sourceFluids());
        return List.copyOf(list);
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
            list.add(new ItemPicker.Entry(id, stack, PickerEntries.nameOr(stack::getHoverName, id)));
        }
        return List.copyOf(list);
    }
}
