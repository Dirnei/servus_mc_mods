package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
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
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.BURNER_EXTRACTOR.get())
                .pattern("III")
                .pattern("IFI")
                .pattern("CCC")
                .define('I', Items.IRON_INGOT)
                .define('F', Items.FURNACE)
                .define('C', Items.COBBLESTONE)
                .unlockedBy("has_furnace", has(Items.FURNACE))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.BURNER_SMELTER.get())
                .pattern("III")
                .pattern("BFB")
                .pattern("BBB")
                .define('I', Items.IRON_INGOT)
                .define('F', Items.FURNACE)
                .define('B', Items.BRICKS)
                .unlockedBy("has_furnace", has(Items.FURNACE))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.COAL_GENERATOR.get())
                .pattern("ICI")
                .pattern("CFC")
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .define('F', Items.FURNACE)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.POWER_CABLE.get(), 8)
                .pattern("ICI")
                .pattern("ICI")
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(output);
    }
}
