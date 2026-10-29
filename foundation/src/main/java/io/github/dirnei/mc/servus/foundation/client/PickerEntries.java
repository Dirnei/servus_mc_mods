package io.github.dirnei.mc.servus.foundation.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Entry lists for an {@link ItemPicker} built from the registries, for any
 * mod's screens. Build them once per client session and keep them.
 */
public final class PickerEntries {

    private PickerEntries() {
    }

    /** Every registered source fluid once, each drawn with its still texture, without a none entry. */
    public static List<ItemPicker.Entry> sourceFluids() {
        List<ItemPicker.Entry> list = new ArrayList<>();
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid == Fluids.EMPTY || !fluid.isSource(fluid.defaultFluidState())) {
                continue;
            }
            FluidStack stack = new FluidStack(fluid, 1);
            String id = BuiltInRegistries.FLUID.getKey(fluid).toString();
            list.add(ItemPicker.Entry.fluid(id, stack, nameOr(stack::getHoverName, id)));
        }
        return list;
    }

    /** The name from {@code name}, or the id when a mod's item or fluid has no usable name. */
    public static Component nameOr(Supplier<Component> name, String id) {
        try {
            Component component = name.get();
            if (!component.getString().isBlank()) {
                return component;
            }
        } catch (RuntimeException e) {
            // a broken modded name must not keep the picker from opening
        }
        return Component.literal(id);
    }
}
