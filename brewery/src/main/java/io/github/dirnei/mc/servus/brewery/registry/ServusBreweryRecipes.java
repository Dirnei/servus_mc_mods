package io.github.dirnei.mc.servus.brewery.registry;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.malting.GerminatingRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The recipe types of Servus: Brewery. */
public final class ServusBreweryRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ServusBreweryIds.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ServusBreweryIds.MOD_ID);

    public static final Identifier GERMINATING_ID = Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "germinating");
    public static final DeferredHolder<RecipeType<?>, RecipeType<GerminatingRecipe>> GERMINATING =
            RECIPE_TYPES.register("germinating", () -> RecipeType.simple(GERMINATING_ID));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GerminatingRecipe>> GERMINATING_SERIALIZER =
            RECIPE_SERIALIZERS.register("germinating", () -> GerminatingRecipe.SERIALIZER);

    private ServusBreweryRecipes() {
    }

    /** The germinating recipe that takes {@code stack}, if any; works on both sides since the recipes are synced. */
    public static Optional<GerminatingRecipe> germinating(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return ServusRecipes.first(level, GERMINATING.get(), new SingleRecipeInput(stack)).map(RecipeHolder::value);
    }
}
