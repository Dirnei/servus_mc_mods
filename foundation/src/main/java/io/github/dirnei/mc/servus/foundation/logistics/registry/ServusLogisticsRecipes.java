package io.github.dirnei.mc.servus.foundation.logistics.registry;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.facade.FacadeRecipe;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusLogisticsRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ServusFoundationIds.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<FacadeRecipe>> FACADE =
            RECIPE_SERIALIZERS.register("facade", () -> new SimpleCraftingRecipeSerializer<>(FacadeRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<FluidTankRecipe>> FLUID_TANK =
            RECIPE_SERIALIZERS.register("fluid_tank", () -> new SimpleCraftingRecipeSerializer<>(FluidTankRecipe::new));

    private ServusLogisticsRecipes() {
    }
}
