package io.github.dirnei.mc.servus.alchemy.calciner;

import io.github.dirnei.mc.servus.alchemy.CalcinationRecipe;
import io.github.dirnei.mc.servus.alchemy.calciner.RecipeSpecificity.Candidate;
import io.github.dirnei.mc.servus.alchemy.calciner.RecipeSpecificity.Rank;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyRecipes;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Which calcination recipe burns an item: of every recipe that accepts it, the most specific one
 * ({@link RecipeSpecificity}). Works on both sides, so the Calciner, JEI and the journal agree.
 */
public final class CalcinationRecipes {

    /** Ranks per recipe, kept per recipe map so a reload or a recipe sync starts afresh. */
    private static final Map<RecipeMap, Map<ResourceKey<Recipe<?>>, Rank>> RANKS = new WeakHashMap<>();

    private CalcinationRecipes() {
    }

    /** The recipe the Calciner burns this item with, whatever the stack's count; empty when none accepts it. */
    public static Optional<RecipeHolder<CalcinationRecipe>> forItem(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        RecipeMap map = ServusRecipes.recipeMap(level);
        List<Candidate<RecipeHolder<CalcinationRecipe>>> candidates = new ArrayList<>();
        for (RecipeHolder<CalcinationRecipe> holder : map.byType(ServusAlchemyRecipes.CALCINATION.get())) {
            if (holder.value().accepts(stack)) {
                candidates.add(new Candidate<>(holder, rank(map, holder)));
            }
        }
        return RecipeSpecificity.best(candidates);
    }

    /** The items of the recipe's ingredient that it really burns, at the recipe's count: no more specific recipe takes them. */
    public static List<ItemStack> burns(Level level, RecipeHolder<CalcinationRecipe> holder) {
        SizedIngredient input = holder.value().input();
        return input.ingredient().items()
                .map(item -> new ItemStack(item, input.count()))
                .filter(stack -> forItem(level, stack).map(RecipeHolder::id).filter(holder.id()::equals).isPresent())
                .toList();
    }

    private static synchronized Rank rank(RecipeMap map, RecipeHolder<CalcinationRecipe> holder) {
        return RANKS.computeIfAbsent(map, ignored -> new HashMap<>())
                .computeIfAbsent(holder.id(), id -> rank(holder.value().input().ingredient(), id.identifier().toString()));
    }

    private static Rank rank(Ingredient ingredient, String id) {
        boolean tag = !ingredient.isCustom() && ingredient.getValues().unwrapKey().isPresent();
        return new Rank(tag, (int) ingredient.items().count(), id);
    }
}
