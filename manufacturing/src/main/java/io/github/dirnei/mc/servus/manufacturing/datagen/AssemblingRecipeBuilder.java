package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.recipe.AssemblingRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** Datagen builder for assembling recipes, written under {@code recipe/assembling/}. */
final class AssemblingRecipeBuilder {

    private final List<SizedIngredient> ingredients = new ArrayList<>();
    private final ItemStack result;
    private int time = 20;
    private Optional<ResourceLocation> tech = Optional.empty();

    private AssemblingRecipeBuilder(ItemStack result) {
        this.result = result;
    }

    static AssemblingRecipeBuilder assembling(ItemLike result, int count) {
        return new AssemblingRecipeBuilder(new ItemStack(result, count));
    }

    AssemblingRecipeBuilder requires(ItemLike item, int count) {
        ingredients.add(SizedIngredient.of(item, count));
        return this;
    }

    AssemblingRecipeBuilder time(int ticks) {
        this.time = ticks;
        return this;
    }

    /** Requires the tech to be researched before the assembler accepts the recipe. */
    AssemblingRecipeBuilder tech(String techPath) {
        this.tech = Optional.of(ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, techPath));
        return this;
    }

    void save(RecipeOutput output, String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "assembling/" + name);
        output.accept(id, new AssemblingRecipe(List.copyOf(ingredients), result, time, tech), null);
    }
}
