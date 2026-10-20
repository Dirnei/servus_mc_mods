package io.github.dirnei.mc.servus.delicacies.registry;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.board.CuttingRecipe;
import io.github.dirnei.mc.servus.delicacies.cooking.PanRecipe;
import io.github.dirnei.mc.servus.delicacies.cooking.PotRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The recipe types of the kitchen: pot, pan and cutting. */
public final class ServusDelicaciesRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ServusDelicaciesIds.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ServusDelicaciesIds.MOD_ID);

    public static final Identifier POT_ID = Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, "pot");
    public static final DeferredHolder<RecipeType<?>, RecipeType<PotRecipe>> POT =
            RECIPE_TYPES.register("pot", () -> RecipeType.simple(POT_ID));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PotRecipe>> POT_SERIALIZER =
            RECIPE_SERIALIZERS.register("pot", () -> PotRecipe.SERIALIZER);

    public static final Identifier PAN_ID = Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, "pan");
    public static final DeferredHolder<RecipeType<?>, RecipeType<PanRecipe>> PAN =
            RECIPE_TYPES.register("pan", () -> RecipeType.simple(PAN_ID));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PanRecipe>> PAN_SERIALIZER =
            RECIPE_SERIALIZERS.register("pan", () -> PanRecipe.SERIALIZER);

    public static final Identifier CUTTING_ID = Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, "cutting");
    public static final DeferredHolder<RecipeType<?>, RecipeType<CuttingRecipe>> CUTTING =
            RECIPE_TYPES.register("cutting", () -> RecipeType.simple(CUTTING_ID));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CuttingRecipe>> CUTTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("cutting", () -> CuttingRecipe.SERIALIZER);

    private ServusDelicaciesRecipes() {
    }

    /** Sends the kitchen recipes to clients, for JEI and the client's insertion checks. */
    public static void syncToClients() {
        ServusRecipes.syncToClients(POT);
        ServusRecipes.syncToClients(PAN);
        ServusRecipes.syncToClients(CUTTING);
    }
}
