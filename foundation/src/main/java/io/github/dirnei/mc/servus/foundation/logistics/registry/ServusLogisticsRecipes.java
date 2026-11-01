package io.github.dirnei.mc.servus.foundation.logistics.registry;

import com.mojang.serialization.MapCodec;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.facade.FacadeRecipe;
import io.github.dirnei.mc.servus.foundation.logistics.facade.HollowFacadeRecipe;
import io.github.dirnei.mc.servus.foundation.logistics.recipe.FloorBlockIngredient;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ServusLogisticsRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ServusFoundationIds.MOD_ID);
    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, ServusFoundationIds.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FacadeRecipe>> FACADE =
            RECIPE_SERIALIZERS.register("facade", () -> FacadeRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HollowFacadeRecipe>> HOLLOW_FACADE =
            RECIPE_SERIALIZERS.register("hollow_facade", () -> HollowFacadeRecipe.SERIALIZER);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FluidTankRecipe>> FLUID_TANK =
            RECIPE_SERIALIZERS.register("fluid_tank", () -> FluidTankRecipe.SERIALIZER);

    public static final DeferredHolder<IngredientType<?>, IngredientType<FloorBlockIngredient>> FLOOR_BLOCK =
            INGREDIENT_TYPES.register("floor_block", () -> new IngredientType<>(
                    MapCodec.unit(FloorBlockIngredient.INSTANCE), StreamCodec.unit(FloorBlockIngredient.INSTANCE)));

    private ServusLogisticsRecipes() {
    }
}
