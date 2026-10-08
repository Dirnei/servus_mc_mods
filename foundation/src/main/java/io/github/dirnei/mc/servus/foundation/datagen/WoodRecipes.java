package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.wood.WoodRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;

/** Datagen helper: saves shaped recipes as {@link WoodRecipe}s, so the result remembers the wood of its planks. */
public final class WoodRecipes {

    private WoodRecipes() {
    }

    public static RecipeOutput output(RecipeOutput output) {
        return new RecipeOutput() {
            @Override
            public void accept(ResourceLocation id, Recipe<?> recipe, AdvancementHolder advancement, ICondition... conditions) {
                output.accept(id, new WoodRecipe((ShapedRecipe) recipe), advancement, conditions);
            }

            @Override
            public Advancement.Builder advancement() {
                return output.advancement();
            }
        };
    }
}
