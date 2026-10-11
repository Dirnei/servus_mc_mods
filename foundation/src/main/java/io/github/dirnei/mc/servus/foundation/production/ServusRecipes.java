package io.github.dirnei.mc.servus.foundation.production;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

/**
 * Recipe lookups that the 1.21.1 recipe manager offered on every level. Since
 * 1.21.2 only the server knows the recipes, so every lookup here answers
 * empty on a client level.
 */
public final class ServusRecipes {

    private ServusRecipes() {
    }

    public static ResourceKey<Recipe<?>> key(Identifier id) {
        return ResourceKey.create(Registries.RECIPE, id);
    }

    /** The recipe with that id; empty on the client. */
    public static Optional<RecipeHolder<?>> byId(Level level, Identifier id) {
        if (level instanceof ServerLevel server) {
            return Optional.ofNullable(server.recipeAccess().byKey(key(id)).orElse(null));
        }
        return Optional.empty();
    }

    /** The crafting recipe with that id; empty on the client or for other recipe types. */
    @SuppressWarnings("unchecked")
    public static Optional<RecipeHolder<CraftingRecipe>> crafting(Level level, Identifier id) {
        return byId(level, id).filter(holder -> holder.value() instanceof CraftingRecipe)
                .map(holder -> (RecipeHolder<CraftingRecipe>) holder);
    }

    /** The crafting recipe a grid matches; empty on the client. */
    public static Optional<RecipeHolder<CraftingRecipe>> craftingFor(Level level, CraftingInput input) {
        if (level instanceof ServerLevel server) {
            return server.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, server);
        }
        return Optional.empty();
    }

    /** The result a recipe shows (its first display), empty when it shows none. */
    public static ItemStack displayResult(Level level, Recipe<?> recipe) {
        List<RecipeDisplay> displays = recipe.display();
        if (displays.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return displays.getFirst().result().resolveForFirstStack(SlotDisplayContext.fromLevel(level));
    }

    /**
     * The ingredients of a crafting recipe as laid out in the grid: shaped
     * recipes keep their pattern positions (an empty cell is an empty
     * ingredient), shapeless ones list their ingredients in order.
     */
    public static List<Optional<Ingredient>> gridIngredients(CraftingRecipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.getIngredients();
        }
        List<Optional<Ingredient>> ingredients = new ArrayList<>();
        if (!recipe.placementInfo().isImpossibleToPlace()) {
            recipe.placementInfo().ingredients().forEach(ingredient -> ingredients.add(Optional.of(ingredient)));
        }
        return ingredients;
    }

    /** One item of an ingredient for display, empty when it lists none. */
    public static ItemStack firstItem(Ingredient ingredient) {
        return ingredient.items().findFirst().map(ItemStack::new).orElse(ItemStack.EMPTY);
    }
}
