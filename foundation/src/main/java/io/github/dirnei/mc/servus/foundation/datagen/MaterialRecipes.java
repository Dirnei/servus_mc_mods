package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.material.MaterialKind;
import io.github.dirnei.mc.servus.foundation.material.MaterialRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jspecify.annotations.Nullable;

/** Datagen helper: saves shaped recipes as {@link MaterialRecipe}s, so the result remembers the material of its grid. */
public final class MaterialRecipes {

    private MaterialRecipes() {
    }

    /** Wraps {@code output} so every shaped recipe saved to it is written as a {@link MaterialRecipe} of {@code kind}. */
    public static RecipeOutput output(RecipeOutput output, HolderLookup.Provider registries, MaterialKind kind) {
        return new RecipeOutput() {
            @Override
            public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement,
                               ICondition... conditions) {
                output.accept(id, toMaterial((ShapedRecipe) recipe, kind), advancement, conditions);
            }

            @Override
            public Advancement.Builder advancement() {
                return output.advancement();
            }

            @Override
            public void includeRootAdvancement() {
                output.includeRootAdvancement();
            }
        };
    }

    /** The same recipe as a material recipe, built from the shaped recipe's public data. */
    private static MaterialRecipe toMaterial(ShapedRecipe recipe, MaterialKind kind) {
        ShapedCraftingRecipeDisplay display = (ShapedCraftingRecipeDisplay) recipe.display().getFirst();
        ItemStackTemplate result = ((SlotDisplay.ItemStackSlotDisplay) display.result()).stack();
        return new MaterialRecipe(new Recipe.CommonInfo(recipe.showNotification()),
                new CraftingRecipe.CraftingBookInfo(recipe.category(), recipe.group()), recipe.pattern, result, kind);
    }
}
