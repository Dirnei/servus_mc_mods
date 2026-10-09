package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinationRecipe;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principles;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

final class ServusManufacturingRecipeProvider extends RecipeProvider {

    ServusManufacturingRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        crafting(output);
        calcination(output);
    }

    /** What the Calciner burns common matter into; the numbers are datapack examples. */
    private void calcination(RecipeOutput output) {
        calcine(output, "cobblestone", Ingredient.of(Tags.Items.COBBLESTONES), 100, 0, 10);
        calcine(output, "stone", Ingredient.of(Tags.Items.STONES), 100, 0, 10);
        calcine(output, "dirt", Ingredient.of(ItemTags.DIRT), 50, 20, 20);
        calcine(output, "sand", Ingredient.of(Tags.Items.SANDS), 80, 0, 10);
        calcine(output, "gravel", Ingredient.of(Tags.Items.GRAVELS), 80, 0, 10);
        calcine(output, "logs", Ingredient.of(ItemTags.LOGS), 20, 100, 10);
        calcine(output, "planks", Ingredient.of(ItemTags.PLANKS), 5, 25, 0);
        calcine(output, "leaves", Ingredient.of(ItemTags.LEAVES), 0, 40, 20);
        calcine(output, "saplings", Ingredient.of(ItemTags.SAPLINGS), 0, 40, 20);
        calcine(output, "crops", Ingredient.of(Tags.Items.CROPS), 10, 30, 30);
        calcine(output, "seeds", Ingredient.of(Tags.Items.SEEDS), 10, 30, 30);
        calcine(output, "coal", Ingredient.of(ItemTags.COALS), 0, 200, 0);
        calcine(output, "raw_materials", Ingredient.of(Tags.Items.RAW_MATERIALS), 150, 0, 50);
        calcine(output, "redstone", Ingredient.of(Tags.Items.DUSTS_REDSTONE), 0, 0, 100);
        calcine(output, "bones", Ingredient.of(Tags.Items.BONES), 100, 0, 0);
    }

    private static void calcine(RecipeOutput output, String name, Ingredient input, int salt, int sulfur, int mercury) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "calcination/" + name);
        output.accept(id, new CalcinationRecipe(new SizedIngredient(input, 1), new Principles(salt, sulfur, mercury),
                CalcinationRecipe.DEFAULT_TIME), null);
    }

    private void crafting(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.RESEARCH_LAB.get())
                .pattern("IGI")
                .pattern("RBR")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .define('R', Items.REDSTONE)
                .define('B', Items.BOOK)
                .unlockedBy("has_book", has(Items.BOOK))
                .save(output);
    }
}
