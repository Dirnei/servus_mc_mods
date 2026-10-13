package io.github.dirnei.mc.servus.foundation.logistics.registry;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.facade.FacadeRecipe;
import io.github.dirnei.mc.servus.foundation.logistics.facade.HollowFacadeRecipe;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusLogisticsRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ServusFoundationIds.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FacadeRecipe>> FACADE =
            RECIPE_SERIALIZERS.register("facade", () -> unit(FacadeRecipe.INSTANCE));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HollowFacadeRecipe>> HOLLOW_FACADE =
            RECIPE_SERIALIZERS.register("hollow_facade", () -> unit(HollowFacadeRecipe.INSTANCE));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FluidTankRecipe>> FLUID_TANK =
            RECIPE_SERIALIZERS.register("fluid_tank", () -> unit(FluidTankRecipe.INSTANCE));

    private ServusLogisticsRecipes() {
    }

    /** Serializer of a recipe without settings: every recipe file decodes to the one instance. */
    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeSerializer<T> unit(T instance) {
        return new RecipeSerializer<>(MapCodec.unit(instance), StreamCodec.unit(instance));
    }
}
