package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.malting.GerminatingRecipe;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import io.github.dirnei.mc.servus.foundation.machine.mill.MillingRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import net.minecraft.resources.Identifier;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * The Brewery recipes: Hop Poles from sticks, soaked grain from a water bucket, the Malting Floor, germinating on the
 * floor and milling malt to grist.
 */
final class ServusBreweryRecipeProvider extends RecipeProvider {

    private static final int GERMINATION_TIME = 24000;
    private static final int TURN_BONUS = 2400;
    private static final int TURN_COOLDOWN = 1200;
    private static final int MALT_MILLING_WORK = 100;

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

        soaking(ServusBreweryItems.BARLEY.get(), ServusBreweryItems.SOAKED_BARLEY.get(), "has_barley");
        soaking(Items.WHEAT, ServusBreweryItems.SOAKED_WHEAT.get(), "has_wheat");

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusBreweryItems.MALTING_FLOOR.get())
                .pattern("S S")
                .pattern("SSS")
                .define('S', Items.SPRUCE_SLAB)
                .unlockedBy("has_soaked_barley", has(ServusBreweryItems.SOAKED_BARLEY.get()))
                .save(output);

        germinating(ServusBreweryItems.SOAKED_BARLEY.get(), ServusBreweryItems.GREEN_MALT.get(), "green_malt");
        germinating(ServusBreweryItems.SOAKED_WHEAT.get(), ServusBreweryItems.GREEN_WHEAT_MALT.get(), "green_wheat_malt");

        milling(ServusBreweryItems.PALE_MALT.get(), ServusBreweryItems.PALE_MALT_GRIST.get(), "pale_malt_grist");
        milling(ServusBreweryItems.DARK_MALT.get(), ServusBreweryItems.DARK_MALT_GRIST.get(), "dark_malt_grist");
        milling(ServusBreweryItems.WHEAT_MALT.get(), ServusBreweryItems.WHEAT_MALT_GRIST.get(), "wheat_malt_grist");
    }

    /** Eight grain around a Water Bucket make eight soaked grain; the bucket stays as its crafting remainder. */
    private void soaking(Item grain, Item soaked, String criterion) {
        ShapedRecipeBuilder.shaped(items, RecipeCategory.FOOD, soaked, 8)
                .pattern("GGG")
                .pattern("GWG")
                .pattern("GGG")
                .define('G', grain)
                .define('W', Items.WATER_BUCKET)
                .unlockedBy(criterion, has(grain))
                .save(output);
    }

    private void germinating(Item soaked, Item green, String name) {
        output.accept(ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "germinating/" + name)),
                new GerminatingRecipe(Ingredient.of(soaked), new ItemStackTemplate(green), GERMINATION_TIME, TURN_BONUS, TURN_COOLDOWN),
                null);
    }

    private void milling(Item malt, Item grist, String name) {
        output.accept(ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "milling/" + name)),
                new MillingRecipe(new SizedIngredient(Ingredient.of(malt), 1), new ItemStackTemplate(grist), MALT_MILLING_WORK), null);
    }
}
