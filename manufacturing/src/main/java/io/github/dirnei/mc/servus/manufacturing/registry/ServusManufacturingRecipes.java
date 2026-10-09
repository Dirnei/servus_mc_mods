package io.github.dirnei.mc.servus.manufacturing.registry;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinationRecipe;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorRecipe;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulationRecipe;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusManufacturingRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ServusManufacturingIds.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ServusManufacturingIds.MOD_ID);

    public static final ResourceLocation CALCINATION_ID =
            ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "calcination");
    public static final DeferredHolder<RecipeType<?>, RecipeType<CalcinationRecipe>> CALCINATION =
            RECIPE_TYPES.register("calcination", () -> RecipeType.simple(CALCINATION_ID));
    public static final DeferredHolder<RecipeSerializer<?>, CalcinationRecipe.Serializer> CALCINATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("calcination", CalcinationRecipe.Serializer::new);

    public static final ResourceLocation COAGULATION_ID =
            ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "coagulation");
    public static final DeferredHolder<RecipeType<?>, RecipeType<CoagulationRecipe>> COAGULATION =
            RECIPE_TYPES.register("coagulation", () -> RecipeType.simple(COAGULATION_ID));
    public static final DeferredHolder<RecipeSerializer<?>, CoagulationRecipe.Serializer> COAGULATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("coagulation", CoagulationRecipe.Serializer::new);

    public static final ResourceLocation ATHANOR_ID =
            ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "athanor");
    public static final DeferredHolder<RecipeType<?>, RecipeType<AthanorRecipe>> ATHANOR =
            RECIPE_TYPES.register("athanor", () -> RecipeType.simple(ATHANOR_ID));
    public static final DeferredHolder<RecipeSerializer<?>, AthanorRecipe.Serializer> ATHANOR_SERIALIZER =
            RECIPE_SERIALIZERS.register("athanor", AthanorRecipe.Serializer::new);

    private ServusManufacturingRecipes() {
    }
}
