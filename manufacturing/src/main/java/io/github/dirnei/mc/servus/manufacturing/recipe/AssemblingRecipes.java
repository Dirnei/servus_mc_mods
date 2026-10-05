package io.github.dirnei.mc.servus.manufacturing.recipe;

import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

/** Lookups over assembling recipes shared by block entity, menu and screen. */
public final class AssemblingRecipes {

    private AssemblingRecipes() {
    }

    /** All assembling recipes sorted by id, identical on client and server. */
    public static List<RecipeHolder<AssemblingRecipe>> sorted(Level level) {
        return level.getRecipeManager().getAllRecipesFor(ServusManufacturingRecipes.ASSEMBLING.get()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .toList();
    }

    public static Optional<RecipeHolder<AssemblingRecipe>> byId(Level level, ResourceLocation id) {
        return level.getRecipeManager().byKey(id)
                .filter(holder -> holder.value() instanceof AssemblingRecipe)
                .map(holder -> {
                    @SuppressWarnings("unchecked")
                    RecipeHolder<AssemblingRecipe> typed = (RecipeHolder<AssemblingRecipe>) holder;
                    return typed;
                });
    }

    /** Whether any assembling recipe uses the stack as an ingredient. */
    public static boolean isIngredientOfAny(Level level, ItemStack stack) {
        for (RecipeHolder<AssemblingRecipe> holder : sorted(level)) {
            if (isIngredientOf(holder.value(), stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIngredientOf(AssemblingRecipe recipe, ItemStack stack) {
        return recipe.ingredients().stream().anyMatch(sized -> sized.ingredient().test(stack));
    }
}
