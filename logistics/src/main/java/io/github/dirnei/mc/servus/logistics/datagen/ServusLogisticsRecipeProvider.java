package io.github.dirnei.mc.servus.logistics.datagen;

import io.github.dirnei.mc.servus.logistics.facade.FacadeRecipe;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.world.item.Items;

final class ServusLogisticsRecipeProvider extends RecipeProvider {

    ServusLogisticsRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ServusLogisticsItems.ITEM_PIPE.get(), 8)
                .pattern("III")
                .pattern("GCG")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .define('C', Items.CHEST)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ServusLogisticsItems.POWER_CABLE.get(), 8)
                .pattern("CCC")
                .pattern("RGR")
                .pattern("CCC")
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .define('G', Items.GLASS)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusLogisticsItems.FLUID_TANK.get())
                .pattern("IGI")
                .pattern("G G")
                .pattern("IGI")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy("has_glass", has(Items.GLASS))
                .save(output);

        SpecialRecipeBuilder.special(FacadeRecipe::new).save(output, "servus_logistics:facade");
    }
}
