package io.github.dirnei.mc.servus.foundation.production;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

/**
 * Production recipes from every recipe of a type that has exactly one
 * ingredient: one recipe per item the ingredient accepts, that item in, the
 * result out. Used by machines that process one item at a time, such as
 * furnaces behind a Machine Adapter or the Burner Smelter.
 */
public final class SingleInputRecipes {

    private RecipeManager cachedManager;
    private RecipeType<?> cachedType;
    private List<ProductionRecipe> cached = List.of();

    /**
     * The production recipes of {@code type}, cached until the recipe manager
     * or the type changes.
     *
     * @param ticks time per batch of a recipe
     */
    public List<ProductionRecipe> list(ServerLevel level, RecipeType<?> type, ToIntFunction<Recipe<?>> ticks) {
        RecipeManager manager = level.recipeAccess();
        if (manager != cachedManager || type != cachedType) {
            cachedManager = manager;
            cachedType = type;
            cached = build(level, manager, type, ticks);
        }
        return cached;
    }

    /** The recipe type with that id, or null. */
    public static RecipeType<?> type(String id) {
        Identifier location = Identifier.tryParse(id);
        return location == null ? null : BuiltInRegistries.RECIPE_TYPE.getValue(location);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<ProductionRecipe> build(ServerLevel level, RecipeManager manager, RecipeType<?> type,
                                                ToIntFunction<Recipe<?>> ticks) {
        if (type == null) {
            return List.of();
        }
        List<ProductionRecipe> recipes = new ArrayList<>();
        Set<Identifier> ids = new HashSet<>();
        for (Object entry : manager.recipeMap().byType((RecipeType) type)) {
            RecipeHolder<?> holder = (RecipeHolder<?>) entry;
            Recipe<?> recipe = holder.value();
            // Recipes are not synced in 26.1; on the server the placement info lists the ingredients.
            List<Ingredient> ingredients = recipe.placementInfo().isImpossibleToPlace() ? List.of()
                    : recipe.placementInfo().ingredients().stream().filter(ingredient -> !ingredient.isEmpty()).toList();
            if (ingredients.size() != 1) {
                continue;
            }
            int time = Math.max(1, ticks.applyAsInt(recipe));
            ingredients.get(0).items().forEach(holderItem -> {
                ItemStack input = new ItemStack(holderItem);
                ItemStack result = resultFor(recipe, input);
                if (result.isEmpty()) {
                    return;
                }
                Identifier item = BuiltInRegistries.ITEM.getKey(input.getItem());
                Identifier recipeId = holder.id().identifier().withSuffix("/" + item.getNamespace() + "/" + item.getPath());
                if (ids.add(recipeId)) {
                    recipes.add(new ProductionRecipe(recipeId, List.of(input.copyWithCount(1)), List.of(result.copy()), time));
                }
            });
        }
        recipes.sort(Comparator.comparing(recipe -> recipe.id().toString()));
        return List.copyOf(recipes);
    }

    /**
     * The result of a one item recipe for {@code input}. Recipes no longer have a fixed result
     * item, so the recipe is assembled; recipes that do not take a single item give nothing.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ItemStack resultFor(Recipe<?> recipe, ItemStack input) {
        try {
            return ((Recipe) recipe).assemble(new SingleRecipeInput(input));
        } catch (ClassCastException e) {
            return ItemStack.EMPTY;
        }
    }
}
