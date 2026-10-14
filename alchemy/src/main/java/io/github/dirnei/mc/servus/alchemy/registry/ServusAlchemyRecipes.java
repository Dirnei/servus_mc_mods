package io.github.dirnei.mc.servus.alchemy.registry;

import io.github.dirnei.mc.servus.alchemy.CalcinationRecipe;
import io.github.dirnei.mc.servus.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorRecipe;
import io.github.dirnei.mc.servus.alchemy.purifier.PurificationRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusAlchemyRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ServusAlchemyIds.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ServusAlchemyIds.MOD_ID);

    public static final Identifier CALCINATION_ID =
            Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "calcination");
    public static final DeferredHolder<RecipeType<?>, RecipeType<CalcinationRecipe>> CALCINATION =
            RECIPE_TYPES.register("calcination", () -> RecipeType.simple(CALCINATION_ID));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CalcinationRecipe>> CALCINATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("calcination", () -> CalcinationRecipe.SERIALIZER);

    public static final Identifier COAGULATION_ID =
            Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "coagulation");
    public static final DeferredHolder<RecipeType<?>, RecipeType<CoagulationRecipe>> COAGULATION =
            RECIPE_TYPES.register("coagulation", () -> RecipeType.simple(COAGULATION_ID));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CoagulationRecipe>> COAGULATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("coagulation", () -> CoagulationRecipe.SERIALIZER);

    public static final Identifier ATHANOR_ID =
            Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "athanor");
    public static final DeferredHolder<RecipeType<?>, RecipeType<AthanorRecipe>> ATHANOR =
            RECIPE_TYPES.register("athanor", () -> RecipeType.simple(ATHANOR_ID));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AthanorRecipe>> ATHANOR_SERIALIZER =
            RECIPE_SERIALIZERS.register("athanor", () -> AthanorRecipe.SERIALIZER);

    public static final Identifier PURIFICATION_ID =
            Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "purification");
    public static final DeferredHolder<RecipeType<?>, RecipeType<PurificationRecipe>> PURIFICATION =
            RECIPE_TYPES.register("purification", () -> RecipeType.simple(PURIFICATION_ID));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PurificationRecipe>> PURIFICATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("purification", () -> PurificationRecipe.SERIALIZER);

    private ServusAlchemyRecipes() {
    }
}
