package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The entries of the Field Head's crop picker: "None, harvest only" and every item that plants a crop, mods' crops included. */
final class CropPickerEntries {

    private CropPickerEntries() {
    }

    static List<ItemPicker.Entry> build() {
        List<ItemPicker.Entry> entries = new ArrayList<>();
        entries.add(ItemPicker.Entry.none(Component.translatable("gui.servus_storage.field_head.crop_none")));
        for (Item item : FieldHeadBlockEntity.cropItems()) {
            ItemStack stack = new ItemStack(item);
            entries.add(new ItemPicker.Entry(BuiltInRegistries.ITEM.getKey(item).toString(), stack, stack.getHoverName()));
        }
        return entries;
    }

    /** The menu's stock keyed by id text, as the picker takes it; null stays null. */
    static Map<String, Long> keyed(Map<Identifier, Long> stock) {
        if (stock == null) {
            return null;
        }
        Map<String, Long> keyed = new HashMap<>();
        stock.forEach((id, count) -> keyed.put(id.toString(), count));
        return keyed;
    }
}
