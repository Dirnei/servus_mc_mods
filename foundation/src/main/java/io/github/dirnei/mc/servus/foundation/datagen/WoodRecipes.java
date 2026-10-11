package io.github.dirnei.mc.servus.foundation.datagen;

import com.mojang.serialization.JsonOps;
import io.github.dirnei.mc.servus.foundation.wood.WoodRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jspecify.annotations.Nullable;

/** Datagen helper: saves shaped recipes as {@link WoodRecipe}s, so the result remembers the wood of its planks. */
public final class WoodRecipes {

    private WoodRecipes() {
    }

    public static RecipeOutput output(RecipeOutput output, HolderLookup.Provider registries) {
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        return new RecipeOutput() {
            @Override
            public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement,
                               ICondition... conditions) {
                output.accept(id, toWood((ShapedRecipe) recipe, ops), advancement, conditions);
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

    /** The same recipe as a wood recipe: both codecs have the same fields. */
    private static WoodRecipe toWood(ShapedRecipe recipe, RegistryOps<com.google.gson.JsonElement> ops) {
        com.google.gson.JsonElement json = ShapedRecipe.MAP_CODEC.codec().encodeStart(ops, recipe).getOrThrow();
        return WoodRecipe.MAP_CODEC.codec().parse(ops, json).getOrThrow();
    }
}
