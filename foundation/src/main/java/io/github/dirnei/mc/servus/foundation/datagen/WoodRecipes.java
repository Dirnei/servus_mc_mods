package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.wood.WoodRecipe;
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

/** Datagen helper: saves shaped recipes as {@link WoodRecipe}s, so the result remembers the wood of its planks. */
public final class WoodRecipes {

    private WoodRecipes() {
    }

    /** Wraps {@code output} so every shaped recipe saved to it is written as a {@link WoodRecipe}. */
    public static RecipeOutput output(RecipeOutput output, HolderLookup.Provider registries) {
        return new RecipeOutput() {
            @Override
            public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement,
                               ICondition... conditions) {
                output.accept(id, toWood((ShapedRecipe) recipe), advancement, conditions);
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

    /** The same recipe as a wood recipe, built from the shaped recipe's public data. */
    private static WoodRecipe toWood(ShapedRecipe recipe) {
        ShapedCraftingRecipeDisplay display = (ShapedCraftingRecipeDisplay) recipe.display().getFirst();
        ItemStackTemplate result = ((SlotDisplay.ItemStackSlotDisplay) display.result()).stack();
        return new WoodRecipe(new Recipe.CommonInfo(recipe.showNotification()),
                new CraftingRecipe.CraftingBookInfo(recipe.category(), recipe.group()), recipe.pattern, result);
    }
}
