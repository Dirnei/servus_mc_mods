package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

final class ServusStorageRecipeProvider extends RecipeProvider {

    ServusStorageRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ServusStorageItems.CRATE.get())
                .pattern("PPP")
                .pattern("PCP")
                .pattern("PPP")
                .define('P', ItemTags.PLANKS)
                .define('C', Items.CHEST)
                .unlockedBy("has_chest", has(Items.CHEST))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, ServusStorageItems.DOUBLE_CRATE.get())
                .requires(ServusStorageItems.CRATE.get())
                .requires(Items.STICK)
                .unlockedBy("has_crate", has(ServusStorageItems.CRATE.get()))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, ServusStorageItems.QUAD_CRATE.get())
                .requires(ServusStorageItems.CRATE.get())
                .requires(Items.STICK, 2)
                .unlockedBy("has_crate", has(ServusStorageItems.CRATE.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ServusStorageItems.WAREHOUSE_CONTROLLER.get())
                .pattern("III")
                .pattern("RCR")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('C', Items.CHEST)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ServusStorageItems.COUNTER_TERMINAL.get())
                .pattern("GGG")
                .pattern("ICI")
                .pattern("PPP")
                .define('G', Items.GLASS)
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CHEST)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_glass", has(Items.GLASS))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ServusStorageItems.RAIL.get(), 8)
                .pattern("III")
                .pattern(" S ")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ServusStorageItems.ELEVATOR.get(), 4)
                .pattern("III")
                .pattern("IRI")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('R', ServusStorageItems.RAIL.get())
                .unlockedBy("has_rail", has(ServusStorageItems.RAIL.get()))
                .save(output);
    }
}
