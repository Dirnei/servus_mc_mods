package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.recipe.CrushingRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** Datagen builder for crushing recipes, written under {@code recipe/crushing/}. */
final class CrushingRecipeBuilder {

    private final SizedIngredient input;
    private final ItemStack result;
    private int time = 60;

    private CrushingRecipeBuilder(SizedIngredient input, ItemStack result) {
        this.input = input;
        this.result = result;
    }

    static CrushingRecipeBuilder crushing(ItemLike input, int inputCount, ItemLike result, int resultCount) {
        return new CrushingRecipeBuilder(SizedIngredient.of(input, inputCount), new ItemStack(result, resultCount));
    }

    CrushingRecipeBuilder time(int ticks) {
        this.time = ticks;
        return this;
    }

    void save(RecipeOutput output, String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "crushing/" + name);
        output.accept(id, new CrushingRecipe(input, result, time), null);
    }
}
