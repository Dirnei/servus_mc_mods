package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.ItemPicker;
import io.github.dirnei.mc.servus.foundation.picker.PickerList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The entries of a Fluid Gantry lock picker: Any, Lock to content and every
 * registered source fluid that has a bucket, mods' fluids included, each
 * with its bucket as the icon.
 */
final class FluidLockEntries {

    /** The id of the Lock to content entry; no fluid id has this form. */
    static final String CONTENT_ID = "#content";

    private FluidLockEntries() {
    }

    static List<ItemPicker.Entry> build() {
        List<ItemPicker.Entry> entries = new ArrayList<>();
        entries.add(ItemPicker.Entry.none(Component.translatable("gui.servus_shuttles.fluid_gantry.lock_any")));
        entries.add(new ItemPicker.Entry(CONTENT_ID, new ItemStack(Items.BUCKET), Component.translatable("gui.servus_shuttles.fluid_gantry.lock_content")));
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid != Fluids.EMPTY && fluid.isSource(fluid.defaultFluidState()) && fluid.getBucket() != Items.AIR) {
                entries.add(new ItemPicker.Entry(BuiltInRegistries.FLUID.getKey(fluid).toString(), new ItemStack(fluid.getBucket()),
                        new FluidStack(fluid, 1).getHoverName()));
            }
        }
        return entries;
    }

    /** The entry id of a lock: the fluid's id, or the none id for Any. */
    static String idOf(Fluid lock) {
        return lock == null ? PickerList.NONE_ID : BuiltInRegistries.FLUID.getKey(lock).toString();
    }
}
