package io.github.dirnei.mc.servus.foundation.datagen;

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
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

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

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusFoundationItems.GOGGLES.get())
                .pattern("LIL")
                .pattern("G G")
                .define('L', Items.LEATHER)
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy("has_glass", has(Items.GLASS))
                .save(output);

        LogisticsDataParts.recipes(output, items, this::has);
        MachineDataParts.recipes(output, items, this::has);

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
                .save(WoodRecipes.output(output, registries));
    }
}
