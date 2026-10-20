package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.beer.FermentingRecipe;
import io.github.dirnei.mc.servus.brewery.beer.TappingRecipe;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewingRecipe;
import io.github.dirnei.mc.servus.brewery.brewhouse.KilningRecipe;
import io.github.dirnei.mc.servus.brewery.malting.GerminatingRecipe;
import io.github.dirnei.mc.servus.brewery.registry.BreweryFluids;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import io.github.dirnei.mc.servus.foundation.machine.mill.MillingRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * The Brewery recipes: Hop Poles from sticks, soaked grain from a water bucket, the Malting Floor, germinating on the
 * floor, milling malt to grist, the Brewhouse parts, brewing wort, kilning malt, Yeast, the glasses, the Fermenting
 * Vat and the Cask, fermenting wort into beer and tapping beer into glasses.
 */
final class ServusBreweryRecipeProvider extends RecipeProvider {

    private static final int GERMINATION_TIME = 24000;
    private static final int TURN_BONUS = 2400;
    private static final int TURN_COOLDOWN = 1200;
    private static final int MALT_MILLING_WORK = 100;
    private static final int BREW_WATER = 1000;
    private static final int BREW_WORT = 1000;
    private static final int BREW_TIME = 400;
    private static final int KILN_TIME = 600;
    private static final int KILN_DARK_TIME = 600;
    private static final int LAGER_TIME = 36000;
    private static final int WEISSBIER_TIME = 24000;
    private static final int GLASS_AMOUNT = 500;

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

        ShapedRecipeBuilder.shaped(items, RecipeCategory.BUILDING_BLOCKS, ServusBreweryItems.BREWHOUSE_BRICKS.get(), 8)
                .pattern("BBB")
                .pattern("BIB")
                .pattern("BBB")
                .define('B', Items.BRICKS)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_bricks", has(Items.BRICKS))
                .save(output);
        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusBreweryItems.BREW_KETTLE.get())
                .pattern("C C")
                .pattern("CCC")
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(output);

        brewing("pale_wort", BreweryFluids.PALE_WORT, sized(ServusBreweryItems.PALE_MALT_GRIST.get(), 4), sized(ServusBreweryItems.HOPS.get(), 1));
        brewing("dark_wort", BreweryFluids.DARK_WORT, sized(ServusBreweryItems.DARK_MALT_GRIST.get(), 4), sized(ServusBreweryItems.HOPS.get(), 1));
        brewing("wheat_wort", BreweryFluids.WHEAT_WORT, sized(ServusBreweryItems.WHEAT_MALT_GRIST.get(), 2),
                sized(ServusBreweryItems.PALE_MALT_GRIST.get(), 2), sized(ServusBreweryItems.HOPS.get(), 1));

        kilning("pale_malt", ServusBreweryItems.GREEN_MALT.get(), ServusBreweryItems.PALE_MALT.get(),
                Optional.of(new KilningRecipe.Dark(new ItemStackTemplate(ServusBreweryItems.DARK_MALT.get()), KILN_DARK_TIME)));
        kilning("wheat_malt", ServusBreweryItems.GREEN_WHEAT_MALT.get(), ServusBreweryItems.WHEAT_MALT.get(), Optional.empty());

        cellar();
    }

    /** Yeast, the glasses, the vat and the Cask; fermenting and tapping. */
    private void cellar() {
        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.FOOD, ServusBreweryItems.YEAST.get())
                .requires(Items.WHEAT)
                .requires(Items.SUGAR)
                .requires(DataComponentIngredient.of(false, DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER), Items.POTION))
                .unlockedBy("has_sugar", has(Items.SUGAR))
                .save(output);
        ShapedRecipeBuilder.shaped(items, RecipeCategory.FOOD, ServusBreweryItems.BEER_GLASS.get(), 2)
                .pattern("G G")
                .pattern("GGG")
                .define('G', Items.GLASS)
                .unlockedBy("has_glass", has(Items.GLASS))
                .save(output);
        ShapedRecipeBuilder.shaped(items, RecipeCategory.FOOD, ServusBreweryItems.WEISSBIER_GLASS.get(), 2)
                .pattern("G G")
                .pattern("G G")
                .pattern(" G ")
                .define('G', Items.GLASS)
                .unlockedBy("has_glass", has(Items.GLASS))
                .save(output);
        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusBreweryItems.FERMENTING_VAT.get())
                .pattern("P P")
                .pattern("IPI")
                .pattern("PPP")
                .define('P', Items.SPRUCE_PLANKS)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_wort", has(BreweryFluids.PALE_WORT.bucket().get()))
                .save(output);
        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusBreweryItems.CASK.get())
                .pattern("PPP")
                .pattern("PGP")
                .pattern("PPP")
                .define('P', ItemTags.PLANKS)
                .define('G', Items.GOLD_INGOT)
                .unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT))
                .save(output);

        fermenting("helles", BreweryFluids.PALE_WORT, BreweryFluids.HELLES, LAGER_TIME);
        fermenting("dunkles", BreweryFluids.DARK_WORT, BreweryFluids.DUNKLES, LAGER_TIME);
        fermenting("weissbier", BreweryFluids.WHEAT_WORT, BreweryFluids.WEISSBIER, WEISSBIER_TIME);

        tapping("helles", ServusBreweryItems.BEER_GLASS.get(), BreweryFluids.HELLES, ServusBreweryItems.HELLES.get());
        tapping("dunkles", ServusBreweryItems.BEER_GLASS.get(), BreweryFluids.DUNKLES, ServusBreweryItems.DUNKLES.get());
        tapping("weissbier", ServusBreweryItems.WEISSBIER_GLASS.get(), BreweryFluids.WEISSBIER, ServusBreweryItems.WEISSBIER.get());
    }

    private void fermenting(String name, BreweryFluids.BrewFluid wort, BreweryFluids.BrewFluid beer, int time) {
        output.accept(ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "fermenting/" + name)),
                new FermentingRecipe(Ingredient.of(ServusBreweryItems.YEAST.get()), FluidIngredient.of(wort.source().get()),
                        beer.source().get(), time), null);
    }

    private void tapping(String name, Item glass, BreweryFluids.BrewFluid beer, Item full) {
        output.accept(ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "tapping/" + name)),
                new TappingRecipe(Ingredient.of(glass), FluidIngredient.of(beer.source().get()), GLASS_AMOUNT, new ItemStackTemplate(full)),
                null);
    }

    private static SizedIngredient sized(Item item, int count) {
        return new SizedIngredient(Ingredient.of(item), count);
    }

    /** Grist and hops with water brewed into a wort. */
    private void brewing(String name, BreweryFluids.BrewFluid wort, SizedIngredient... ingredients) {
        output.accept(ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "brewing/" + name)),
                new BrewingRecipe(List.of(ingredients), SizedFluidIngredient.of(Fluids.WATER, BREW_WATER),
                        new FluidStackTemplate(wort.source().get(), BREW_WORT), BREW_TIME), null);
    }

    private void kilning(String name, Item input, Item result, Optional<KilningRecipe.Dark> dark) {
        output.accept(ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "kilning/" + name)),
                new KilningRecipe(Ingredient.of(input), new ItemStackTemplate(result), KILN_TIME, dark), null);
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
