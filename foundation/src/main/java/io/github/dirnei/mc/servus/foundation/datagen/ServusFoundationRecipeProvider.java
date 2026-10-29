package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.material.MaterialKind;
import io.github.dirnei.mc.servus.foundation.adapter.AdapterContent;
import io.github.dirnei.mc.servus.foundation.logistics.datagen.LogisticsDataParts;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import io.github.dirnei.mc.servus.foundation.filter.FilterTags;

final class ServusFoundationRecipeProvider extends RecipeProvider {

    private ServusFoundationRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    /** The data provider: builds the recipes once the registries are known. */
    static final class Runner extends RecipeProvider.Runner {

        Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ServusFoundationRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Servus Foundation recipes";
        }
    }

    @Override
    protected void buildRecipes() {
        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusFoundationItems.CONFIGURATION_CARROT.get())
                .pattern("R")
                .pattern("I")
                .pattern("C")
                .define('R', Items.REDSTONE)
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CARROT)
                .unlockedBy("has_carrot", has(Items.CARROT))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusFoundationItems.DATA_POTATO.get())
                .pattern("R")
                .pattern("D")
                .pattern("P")
                .define('R', Items.REDSTONE)
                .define('D', ServusLogisticsItems.DATA_CABLE.get())
                .define('P', Items.POTATO)
                .unlockedBy("has_data_cable", has(ServusLogisticsItems.DATA_CABLE.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusFoundationItems.SPEED_UPGRADE.get())
                .pattern("RGR")
                .pattern("GCG")
                .pattern("RGR")
                .define('R', Items.REDSTONE)
                .define('G', Items.GOLD_INGOT)
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_gold", has(Items.GOLD_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusFoundationItems.CAPACITY_UPGRADE.get())
                .pattern("IGI")
                .pattern("GBG")
                .pattern("IGI")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .define('B', Items.BUCKET)
                .unlockedBy("has_bucket", has(Items.BUCKET))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusFoundationItems.HEATING_ELEMENT.get())
                .pattern("CCC")
                .pattern("RIR")
                .pattern("CCC")
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusFoundationItems.GOGGLES.get())
                .pattern("LIL")
                .pattern("G G")
                .define('L', Items.LEATHER)
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy("has_glass", has(Items.GLASS))
                .save(output);

        filterRecipes();

        LogisticsDataParts.recipes(output, items, this::has);
        MachineDataParts.recipes(output, registries, items, this::has);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.REDSTONE, AdapterContent.ITEM.get())
                .pattern("CHC")
                .pattern("RDR")
                .pattern("PPP")
                .define('C', Items.COPPER_INGOT)
                .define('H', Items.HOPPER)
                .define('R', Items.REDSTONE)
                .define('D', ServusLogisticsItems.DATA_CABLE.get())
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_data_cable", has(ServusLogisticsItems.DATA_CABLE.get()))
                .save(MaterialRecipes.output(output, registries, MaterialKind.WOOD));
    }

    /** Charcoal Powder activated in the Blast Furnace only, and the Carbon Filter bound with starch. */
    private void filterRecipes() {
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ServusFoundationItems.CHARCOAL_POWDER.get()), RecipeCategory.MISC,
                        CookingBookCategory.MISC, ServusFoundationItems.ACTIVATED_CHARCOAL.get(), 0.1F, 100)
                .unlockedBy("has_charcoal_powder", has(ServusFoundationItems.CHARCOAL_POWDER.get()))
                .save(output, ServusFoundationIds.MOD_ID + ":activated_charcoal_from_blasting");

        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.MISC, ServusFoundationItems.CARBON_FILTER.get())
                .requires(ServusFoundationItems.ACTIVATED_CHARCOAL.get(), 2)
                .requires(FilterTags.FILTER_BINDERS)
                .requires(Items.PAPER)
                .unlockedBy("has_activated_charcoal", has(ServusFoundationItems.ACTIVATED_CHARCOAL.get()))
                .save(output);
    }
}
