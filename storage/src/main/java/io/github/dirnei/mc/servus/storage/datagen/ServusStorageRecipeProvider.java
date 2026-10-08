package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackRecipe;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ICondition;

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

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ServusStorageItems.PRODUCTION_TERMINAL.get())
                .pattern("GGG")
                .pattern("CMC")
                .pattern("IRI")
                .define('G', Items.GLASS_PANE)
                .define('C', Items.COPPER_INGOT)
                .define('M', Items.COMPARATOR)
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_comparator", has(Items.COMPARATOR))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ServusStorageItems.PATTERN_STORAGE.get())
                .pattern("IBI")
                .pattern("RCR")
                .pattern("IBI")
                .define('I', Items.IRON_INGOT)
                .define('B', ServusStorageItems.BLANK_PATTERN.get())
                .define('R', Items.REDSTONE)
                .define('C', Items.CHEST)
                .unlockedBy("has_blank_pattern", has(ServusStorageItems.BLANK_PATTERN.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ServusStorageItems.CRAFTING_CPU.get(), 4)
                .pattern("IRI")
                .pattern("RCR")
                .pattern("IRI")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('C', Items.CRAFTING_TABLE)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
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

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ServusStorageItems.HEAVY_RACK.get())
                .pattern("I I")
                .pattern("PPP")
                .pattern("I I")
                .define('I', Items.IRON_INGOT)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(heavyRack(output));

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ServusStorageItems.CRAFTING_STATION.get())
                .pattern("ITI")
                .pattern("ICI")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('T', Items.CRAFTING_TABLE)
                .define('C', Items.CHEST)
                .unlockedBy("has_crafting_table", has(Items.CRAFTING_TABLE))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ServusStorageItems.OUTBOUND_STATION.get())
                .pattern("ICI")
                .pattern("IDI")
                .pattern("IHI")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CHEST)
                .define('D', Items.DROPPER)
                .define('H', Items.HOPPER)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ServusStorageItems.SHUTTLE_GARAGE.get())
                .pattern("IRI")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('R', ServusStorageItems.RAIL.get())
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_rail", has(ServusStorageItems.RAIL.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ServusStorageItems.INBOUND_STATION.get())
                .pattern("III")
                .pattern("ICI")
                .pattern("IHI")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CHEST)
                .define('H', Items.HOPPER)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ServusStorageItems.ACCESS_POINT.get())
                .pattern("IRI")
                .pattern("OCO")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('O', Items.COPPER_INGOT)
                .define('C', Items.CHEST)
                .unlockedBy("has_chest", has(Items.CHEST))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ServusStorageItems.ZONE_LABEL.get())
                .requires(Items.PAPER)
                .requires(Tags.Items.DYES)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ServusStorageItems.BLANK_PATTERN.get())
                .requires(Items.PAPER)
                .requires(Items.REDSTONE)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(output);

        // Any Pattern alone wipes back to a Blank Pattern; the ingredient ignores the encoded recipe.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ServusStorageItems.BLANK_PATTERN.get())
                .requires(ServusStorageItems.PATTERN.get())
                .unlockedBy("has_pattern", has(ServusStorageItems.PATTERN.get()))
                .save(output, ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "blank_pattern_from_pattern"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ServusStorageItems.SHUTTLE.get())
                .pattern("I I")
                .pattern("RBR")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('B', ServusStorageItems.RAIL.get())
                .unlockedBy("has_rail", has(ServusStorageItems.RAIL.get()))
                .save(output);
    }

    /** Saves a shaped recipe as the rack recipe, which remembers the planks' wood. */
    private static RecipeOutput heavyRack(RecipeOutput output) {
        return new RecipeOutput() {
            @Override
            public void accept(ResourceLocation id, Recipe<?> recipe, AdvancementHolder advancement, ICondition... conditions) {
                output.accept(id, new HeavyRackRecipe((ShapedRecipe) recipe), advancement, conditions);
            }

            @Override
            public Advancement.Builder advancement() {
                return output.advancement();
            }
        };
    }
}
