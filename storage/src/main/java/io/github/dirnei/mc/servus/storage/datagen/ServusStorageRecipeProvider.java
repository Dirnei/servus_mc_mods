package io.github.dirnei.mc.servus.storage.datagen;

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
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jspecify.annotations.Nullable;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ICondition;
import io.github.dirnei.mc.servus.foundation.datagen.WoodRecipes;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;

final class ServusStorageRecipeProvider extends RecipeProvider {

    private ServusStorageRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    /** The data provider: builds the recipes once the registries are known. */
    static final class Runner extends RecipeProvider.Runner {

        Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ServusStorageRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Servus Storage recipes";
        }
    }

    @Override
    protected void buildRecipes() {
        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.WAREHOUSE_CONTROLLER.get())
                .pattern("III")
                .pattern("RCR")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('C', Items.CHEST)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(WoodRecipes.output(output, registries));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.STORAGE_TERMINAL.get())
                .pattern("GGG")
                .pattern("ICI")
                .pattern("PPP")
                .define('G', Items.GLASS)
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CHEST)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_glass", has(Items.GLASS))
                .save(WoodRecipes.output(output, registries));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.PRODUCTION_TERMINAL.get())
                .pattern("GGG")
                .pattern("CMC")
                .pattern("PRP")
                .define('G', Items.GLASS_PANE)
                .define('C', Items.COPPER_INGOT)
                .define('M', Items.COMPARATOR)
                .define('R', Items.REDSTONE)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_comparator", has(Items.COMPARATOR))
                .save(WoodRecipes.output(output, registries));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.PATTERN_STORAGE.get())
                .pattern("IAI")
                .pattern("RCR")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('A', Items.PAPER)
                .define('R', Items.REDSTONE)
                .define('C', Items.CHEST)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(WoodRecipes.output(output, registries));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.CRAFTING_CPU.get(), 4)
                .pattern("IRI")
                .pattern("RCR")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('C', Items.CRAFTING_TABLE)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(WoodRecipes.output(output, registries));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.CPU_BUFFER.get(), 4)
                .pattern("IRI")
                .pattern("RCR")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('C', Items.CHEST)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(WoodRecipes.output(output, registries));

        // Steel heads on sleepers in the wood of the planks.
        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.RAIL.get(), 16)
                .pattern("I I")
                .pattern("IPI")
                .pattern("I I")
                .define('I', Items.IRON_INGOT)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(WoodRecipes.output(output, registries));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.ELEVATOR.get(), 4)
                .pattern("III")
                .pattern("IRI")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('R', ServusStorageItems.RAIL.get())
                .unlockedBy("has_rail", has(ServusStorageItems.RAIL.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.HEAVY_RACK.get())
                .pattern("I I")
                .pattern("PPP")
                .pattern("I I")
                .define('I', Items.IRON_INGOT)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(heavyRack(output, registries));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.CRAFTING_STATION.get())
                .pattern("ITI")
                .pattern("ICI")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('T', Items.CRAFTING_TABLE)
                .define('C', Items.CHEST)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_crafting_table", has(Items.CRAFTING_TABLE))
                .save(WoodRecipes.output(output, registries));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.OUTBOUND_STATION.get())
                .pattern("ICI")
                .pattern("IDI")
                .pattern("PHP")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CHEST)
                .define('D', Items.DROPPER)
                .define('H', Items.HOPPER)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(WoodRecipes.output(output, registries));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.SHUTTLE_GARAGE.get())
                .pattern("IRI")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('R', ServusStorageItems.RAIL.get())
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_rail", has(ServusStorageItems.RAIL.get()))
                .save(WoodRecipes.output(output, registries));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.INBOUND_STATION.get())
                .pattern("III")
                .pattern("ICI")
                .pattern("PHP")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CHEST)
                .define('H', Items.HOPPER)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(WoodRecipes.output(output, registries));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.PACKING_STATION.get())
                .pattern("IRI")
                .pattern("OCO")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('O', Items.COPPER_INGOT)
                .define('C', Items.CHEST)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_chest", has(Items.CHEST))
                .save(WoodRecipes.output(output, registries));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.WIRELESS_PACKING_STATION.get())
                .pattern(" E ")
                .pattern("IXI")
                .pattern("PRP")
                .define('E', Items.ENDER_EYE)
                .define('I', Items.IRON_INGOT)
                .define('X', ServusStorageItems.PACKING_STATION.get())
                .define('R', Items.REDSTONE)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_packing_station", has(ServusStorageItems.PACKING_STATION.get()))
                .save(WoodRecipes.output(output, registries));

        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.TOOLS, ServusStorageItems.ZONE_LABEL.get())
                .requires(Items.PAPER)
                .requires(Tags.Items.DYES)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusStorageItems.SHUTTLE_MK1.get())
                .pattern("I I")
                .pattern("RBR")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('B', ServusStorageItems.RAIL.get())
                .unlockedBy("has_rail", has(ServusStorageItems.RAIL.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.SHUTTLE_WORKSHOP.get())
                .pattern("III")
                .pattern("PCP")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CRAFTING_TABLE)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_shuttle", has(ServusStorageItems.SHUTTLE_MK1.get()))
                .save(output);

        // Copper heads on sleepers in the wood of the planks.
        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.FARM_RAIL.get(), 16)
                .pattern("C C")
                .pattern("CPC")
                .pattern("C C")
                .define('C', Items.COPPER_INGOT)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(WoodRecipes.output(output, registries));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusStorageItems.FIELD_HEAD.get())
                .pattern("IBI")
                .pattern("PCP")
                .pattern("PRP")
                .define('I', Items.IRON_INGOT)
                .define('B', ServusLogisticsItems.FLUID_TANK.get())
                .define('C', Items.COMPOSTER)
                .define('P', ItemTags.PLANKS)
                .define('R', ServusStorageItems.FARM_RAIL.get())
                .unlockedBy("has_farm_rail", has(ServusStorageItems.FARM_RAIL.get()))
                .save(WoodRecipes.output(output, registries));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusStorageItems.HANDLING_UPGRADE.get())
                .pattern("CRC")
                .pattern("RPR")
                .pattern("CRC")
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .define('P', Items.PISTON)
                .unlockedBy("has_shuttle", has(ServusStorageItems.SHUTTLE_MK1.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusStorageItems.CHUNK_LOADER_UPGRADE.get())
                .pattern(" E ")
                .pattern("ICI")
                .pattern(" R ")
                .define('E', Items.ENDER_PEARL)
                .define('I', Items.IRON_INGOT)
                .define('C', io.github.dirnei.mc.servus.foundation.machine.MachineContent.CHUNK_LOADER_ITEM.get())
                .define('R', Items.REDSTONE)
                .unlockedBy("has_chunk_loader", has(io.github.dirnei.mc.servus.foundation.machine.MachineContent.CHUNK_LOADER_ITEM.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusStorageItems.WIRELESS_UPGRADE.get())
                .pattern(" C ")
                .pattern("IEI")
                .pattern("RIR")
                .define('C', Items.COPPER_INGOT)
                .define('I', Items.IRON_INGOT)
                .define('E', Items.ENDER_EYE)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_ender_eye", has(Items.ENDER_EYE))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusStorageItems.RANGE_UPGRADE.get())
                .pattern("ISI")
                .pattern("WCW")
                .pattern("ISI")
                .define('I', Items.COPPER_INGOT)
                .define('S', Items.WHEAT_SEEDS)
                .define('W', Items.BONE_MEAL)
                .define('C', Items.COMPOSTER)
                .unlockedBy("has_field_head", has(ServusStorageItems.FIELD_HEAD.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusStorageItems.HARVESTER.get())
                .pattern("ISI")
                .pattern("IHI")
                .pattern(" P ")
                .define('I', Items.IRON_INGOT)
                .define('S', Items.SHEARS)
                .define('H', Items.IRON_HOE)
                .define('P', Items.HOPPER)
                .unlockedBy("has_shuttle", has(ServusStorageItems.SHUTTLE_MK1.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusStorageItems.SPREADER.get())
                .pattern("IHI")
                .pattern("IDI")
                .pattern(" B ")
                .define('I', Items.IRON_INGOT)
                .define('H', Items.HOPPER)
                .define('D', Items.DISPENSER)
                .define('B', Items.BONE_BLOCK)
                .unlockedBy("has_shuttle", has(ServusStorageItems.SHUTTLE_MK1.get()))
                .save(output);
    }

    /** Saves a shaped recipe as the rack recipe, which remembers the planks' wood. */
    private static RecipeOutput heavyRack(RecipeOutput output, HolderLookup.Provider registries) {
        return new RecipeOutput() {
            @Override
            public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement,
                               ICondition... conditions) {
                ShapedRecipe shaped = (ShapedRecipe) recipe;
                ShapedCraftingRecipeDisplay display = (ShapedCraftingRecipeDisplay) shaped.display().getFirst();
                ItemStackTemplate result = ((SlotDisplay.ItemStackSlotDisplay) display.result()).stack();
                output.accept(id, new HeavyRackRecipe(new Recipe.CommonInfo(shaped.showNotification()),
                        new CraftingRecipe.CraftingBookInfo(shaped.category(), shaped.group()), shaped.pattern, result),
                        advancement, conditions);
            }

            @Override
            public Advancement.Builder advancement() {
                return output.advancement();
            }

            @Override
            public void includeRootAdvancement() {
                output.includeRootAdvancement();
            }
        };
    }
}
