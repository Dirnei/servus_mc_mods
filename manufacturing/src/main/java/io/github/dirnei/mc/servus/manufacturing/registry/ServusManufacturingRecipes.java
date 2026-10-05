package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.recipe.AssemblingRecipe;
import io.github.dirnei.mc.servus.manufacturing.recipe.AssemblingRecipeSerializer;
import io.github.dirnei.mc.servus.manufacturing.recipe.CrushingRecipe;
import io.github.dirnei.mc.servus.manufacturing.recipe.CrushingRecipeSerializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusManufacturingRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ServusManufacturingIds.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ServusManufacturingIds.MOD_ID);

    public static final ResourceLocation ASSEMBLING_ID =
            ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "assembling");

    public static final DeferredHolder<RecipeType<?>, RecipeType<AssemblingRecipe>> ASSEMBLING =
            RECIPE_TYPES.register("assembling", () -> RecipeType.simple(ASSEMBLING_ID));

    public static final DeferredHolder<RecipeSerializer<?>, AssemblingRecipeSerializer> ASSEMBLING_SERIALIZER =
            RECIPE_SERIALIZERS.register("assembling", AssemblingRecipeSerializer::new);

    public static final ResourceLocation CRUSHING_ID =
            ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "crushing");

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrushingRecipe>> CRUSHING =
            RECIPE_TYPES.register("crushing", () -> RecipeType.simple(CRUSHING_ID));

    public static final DeferredHolder<RecipeSerializer<?>, CrushingRecipeSerializer> CRUSHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("crushing", CrushingRecipeSerializer::new);

    private ServusManufacturingRecipes() {
    }
}
