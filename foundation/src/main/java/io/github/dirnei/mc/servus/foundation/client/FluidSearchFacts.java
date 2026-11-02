package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.search.SearchFacts;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The {@link SearchFacts} of one fluid on the client: name, id, mod id and
 * name, and fluid tags. A fluid has no tooltip or creative tab facts. Each
 * fact is built on first use and kept.
 */
public final class FluidSearchFacts implements SearchFacts {

    private final FluidStack stack;
    private final Fluid fluid;
    private List<String> names;
    private List<String> mods;
    private List<String> tags;

    private FluidSearchFacts(FluidStack stack) {
        this.stack = stack;
        this.fluid = stack.getFluid();
    }

    /** Facts of a copy of the stack, named by its hover name. */
    public static FluidSearchFacts of(FluidStack stack) {
        return new FluidSearchFacts(stack.copy());
    }

    @Override
    public List<String> names() {
        if (names == null) {
            names = List.of(ItemSearchFacts.safeString(stack::getHoverName));
        }
        return names;
    }

    @Override
    public List<String> ids() {
        return List.of(id().toString());
    }

    @Override
    public List<String> mods() {
        if (mods == null) {
            mods = ModNames.of(id().getNamespace());
        }
        return mods;
    }

    @Override
    public List<String> tags() {
        if (tags == null) {
            tags = fluid.builtInRegistryHolder().tags().map(tag -> tag.location().toString()).toList();
        }
        return tags;
    }

    private Identifier id() {
        return BuiltInRegistries.FLUID.getKey(fluid);
    }
}
