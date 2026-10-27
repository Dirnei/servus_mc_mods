package io.github.dirnei.mc.servus.shuttles.registry;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackRecipe;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusShuttlesRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ServusShuttlesIds.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HeavyRackRecipe>> HEAVY_RACK =
            RECIPE_SERIALIZERS.register("heavy_rack", () -> HeavyRackRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FluidRackRecipe>> FLUID_RACK =
            RECIPE_SERIALIZERS.register("fluid_rack", () -> FluidRackRecipe.SERIALIZER);

    private ServusShuttlesRecipes() {
    }
}
