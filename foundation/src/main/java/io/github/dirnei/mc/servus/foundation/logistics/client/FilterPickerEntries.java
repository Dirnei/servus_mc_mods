package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.client.ItemPicker;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

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
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid == Fluids.EMPTY || !fluid.isSource(fluid.defaultFluidState())) {
                continue;
            }
            FluidStack stack = new FluidStack(fluid, 1);
            String id = BuiltInRegistries.FLUID.getKey(fluid).toString();
            list.add(ItemPicker.Entry.fluid(id, stack, fluidName(stack, id)));
        }
        return List.copyOf(list);
    }

    /** The fluid's name, or its id when a mod's fluid has no usable name. */
    private static Component fluidName(FluidStack stack, String id) {
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
