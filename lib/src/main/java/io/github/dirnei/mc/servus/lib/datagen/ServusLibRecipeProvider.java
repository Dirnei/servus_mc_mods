package io.github.dirnei.mc.servus.lib.datagen;

import io.github.dirnei.mc.servus.lib.registry.ServusLibItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

final class ServusLibRecipeProvider extends RecipeProvider {

    ServusLibRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ServusLibItems.CONFIGURATION_CARROT.get())
                .pattern("R")
                .pattern("I")
                .pattern("C")
                .define('R', Items.REDSTONE)
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CARROT)
                .unlockedBy("has_carrot", has(Items.CARROT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ServusLibItems.GOGGLES.get())
                .pattern("LIL")
                .pattern("G G")
                .define('L', Items.LEATHER)
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy("has_glass", has(Items.GLASS))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ServusLibItems.DATA_CABLE.get(), 4)
                .pattern("CRC")
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output);
    }
}
