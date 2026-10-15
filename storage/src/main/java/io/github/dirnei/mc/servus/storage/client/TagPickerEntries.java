package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.foundation.client.ItemPicker;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The entries of a Field Head picker over an item tag: an "Off" entry and every item of the tag, as the client knows the tag. */
final class TagPickerEntries {

    private TagPickerEntries() {
    }

    static List<ItemPicker.Entry> build(TagKey<Item> tag, Component none) {
        List<ItemPicker.Entry> entries = new ArrayList<>();
        entries.add(ItemPicker.Entry.none(none));
        BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> {
            ItemStack stack = new ItemStack(holder.value());
            entries.add(new ItemPicker.Entry(BuiltInRegistries.ITEM.getKey(holder.value()).toString(), stack, stack.getHoverName()));
        });
        return entries;
    }
}
