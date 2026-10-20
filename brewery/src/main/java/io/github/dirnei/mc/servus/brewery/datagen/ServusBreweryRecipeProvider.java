package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

/** The Brewery crafting recipes: three sticks in a column make two Hop Poles. */
final class ServusBreweryRecipeProvider extends RecipeProvider {

    private ServusBreweryRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    /** The data provider: builds the recipes once the registries are known. */
    static final class Runner extends RecipeProvider.Runner {

        Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ServusBreweryRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Servus Brewery recipes";
        }
    }

    @Override
    protected void buildRecipes() {
        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusBreweryItems.HOP_POLE.get(), 2)
                .pattern("S")
                .pattern("S")
                .pattern("S")
                .define('S', Items.STICK)
                .unlockedBy("has_stick", has(Items.STICK))
                .save(output);
    }
}
