package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.logistics.datagen.LogisticsDataParts;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

final class ServusFoundationRecipeProvider extends RecipeProvider {

    ServusFoundationRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ServusFoundationItems.CONFIGURATION_CARROT.get())
                .pattern("R")
                .pattern("I")
                .pattern("C")
                .define('R', Items.REDSTONE)
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CARROT)
                .unlockedBy("has_carrot", has(Items.CARROT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ServusFoundationItems.GOGGLES.get())
                .pattern("LIL")
                .pattern("G G")
                .define('L', Items.LEATHER)
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy("has_glass", has(Items.GLASS))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ServusFoundationItems.DATA_CABLE.get(), 4)
                .pattern("CRC")
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output);

        LogisticsDataParts.recipes(output, RecipeProvider::has);
    }
}
