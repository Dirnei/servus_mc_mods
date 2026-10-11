package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.field.FieldHeadBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The entries of the Field Head's fertilizer picker: "Off" and every item of the fertilizer tag, as the client knows the tag. */
final class FertilizerPickerEntries {

    private FertilizerPickerEntries() {
    }

    static List<ItemPicker.Entry> build() {
        List<ItemPicker.Entry> entries = new ArrayList<>();
        entries.add(ItemPicker.Entry.none(Component.translatable("gui.servus_storage.field_head.fertilizer_none")));
        BuiltInRegistries.ITEM.getTagOrEmpty(FieldHeadBlockEntity.FERTILIZER).forEach(holder -> {
            ItemStack stack = new ItemStack(holder.value());
            entries.add(new ItemPicker.Entry(BuiltInRegistries.ITEM.getKey(holder.value()).toString(), stack, stack.getHoverName()));
        });
        return entries;
    }
}
