package io.github.dirnei.mc.servus.shuttles.datagen;

import io.github.dirnei.mc.servus.foundation.material.MaterialKind;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackRecipe;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackRecipe;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
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
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ICondition;
import io.github.dirnei.mc.servus.foundation.datagen.MaterialRecipes;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;

final class ServusShuttlesRecipeProvider extends RecipeProvider {

    private ServusShuttlesRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    /** The data provider: builds the recipes once the registries are known. */
    static final class Runner extends RecipeProvider.Runner {

        Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ServusShuttlesRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Servus Shuttles recipes";
        }
    }

    @Override
    protected void buildRecipes() {
        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.WAREHOUSE_CONTROLLER.get())
                .pattern("III")
                .pattern("RCR")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('C', Items.CHEST)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(MaterialRecipes.output(output, registries, MaterialKind.WOOD));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.STORAGE_TERMINAL.get())
                .pattern("GGG")
                .pattern("ICI")
                .pattern("PPP")
                .define('G', Items.GLASS)
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CHEST)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_glass", has(Items.GLASS))
                .save(MaterialRecipes.output(output, registries, MaterialKind.WOOD));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.PRODUCTION_TERMINAL.get())
                .pattern("GGG")
                .pattern("CMC")
                .pattern("PRP")
                .define('G', Items.GLASS_PANE)
                .define('C', Items.COPPER_INGOT)
                .define('M', Items.COMPARATOR)
                .define('R', Items.REDSTONE)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_comparator", has(Items.COMPARATOR))
                .save(MaterialRecipes.output(output, registries, MaterialKind.WOOD));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.PATTERN_STORAGE.get())
                .pattern("IAI")
                .pattern("RCR")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('A', Items.PAPER)
                .define('R', Items.REDSTONE)
                .define('C', Items.CHEST)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(MaterialRecipes.output(output, registries, MaterialKind.WOOD));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.CRAFTING_CPU.get(), 4)
                .pattern("IRI")
                .pattern("RCR")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('C', Items.CRAFTING_TABLE)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(MaterialRecipes.output(output, registries, MaterialKind.WOOD));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.CPU_BUFFER.get(), 4)
                .pattern("IRI")
                .pattern("RCR")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('C', Items.CHEST)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(MaterialRecipes.output(output, registries, MaterialKind.WOOD));

        // Steel heads on sleepers in the wood of the planks.
        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.RAIL.get(), 16)
                .pattern("I I")
                .pattern("IPI")
                .pattern("I I")
                .define('I', Items.IRON_INGOT)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(MaterialRecipes.output(output, registries, MaterialKind.WOOD));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.ELEVATOR.get(), 4)
                .pattern("III")
                .pattern("IRI")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('R', ServusShuttlesItems.RAIL.get())
                .unlockedBy("has_rail", has(ServusShuttlesItems.RAIL.get()))
                .save(MaterialRecipes.output(output, registries, MaterialKind.WOOD));

        // The Fluid Inbound: a Fluid Tank for its buffer, iron for the steel body, copper for the port, cyan dye for the band.
        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.FLUID_INBOUND.get())
                .pattern("IDI")
                .pattern("ITI")
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('D', Items.CYAN_DYE)
                .define('C', Items.COPPER_INGOT)
                .define('T', ServusLogisticsItems.FLUID_TANK.get())
                .unlockedBy("has_fluid_tank", has(ServusLogisticsItems.FLUID_TANK.get()))
                .save(output);

        // The Fluid Outbound: the Fluid Inbound's pattern with orange dye for its amber band.
        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.FLUID_OUTBOUND.get())
                .pattern("IDI")
                .pattern("ITI")
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('D', Items.ORANGE_DYE)
                .define('C', Items.COPPER_INGOT)
                .define('T', ServusLogisticsItems.FLUID_TANK.get())
                .unlockedBy("has_fluid_tank", has(ServusLogisticsItems.FLUID_TANK.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.HEAVY_RACK.get(), 3)
                .pattern("I I")
                .pattern("PPP")
                .pattern("I I")
                .define('I', Items.IRON_INGOT)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(woodRecipe(output, HeavyRackRecipe::new));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.FLUID_RACK.get(), 2)
                .pattern("IGI")
                .pattern("GCG")
                .pattern("IPI")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .define('C', Items.COPPER_INGOT)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_glass", has(Items.GLASS))
                .save(woodRecipe(output, FluidRackRecipe::new));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.CRAFTING_STATION.get())
                .pattern("ITI")
                .pattern("ICI")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('T', Items.CRAFTING_TABLE)
                .define('C', Items.CHEST)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_crafting_table", has(Items.CRAFTING_TABLE))
                .save(MaterialRecipes.output(output, registries, MaterialKind.WOOD));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.OUTBOUND_STATION.get())
                .pattern("ICI")
                .pattern("IDI")
                .pattern("PHP")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CHEST)
                .define('D', Items.DROPPER)
                .define('H', Items.HOPPER)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(MaterialRecipes.output(output, registries, MaterialKind.WOOD));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.SHUTTLE_GARAGE.get())
                .pattern("IRI")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('R', ServusShuttlesItems.RAIL.get())
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_rail", has(ServusShuttlesItems.RAIL.get()))
                .save(MaterialRecipes.output(output, registries, MaterialKind.WOOD));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.INBOUND_STATION.get())
                .pattern("III")
                .pattern("ICI")
                .pattern("PHP")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CHEST)
                .define('H', Items.HOPPER)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(MaterialRecipes.output(output, registries, MaterialKind.WOOD));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.PACKING_STATION.get())
                .pattern("IRI")
                .pattern("OCO")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('O', Items.COPPER_INGOT)
                .define('C', Items.CHEST)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_chest", has(Items.CHEST))
                .save(MaterialRecipes.output(output, registries, MaterialKind.WOOD));

        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.TOOLS, ServusShuttlesItems.ZONE_LABEL.get())
                .requires(Items.PAPER)
                .requires(Tags.Items.DYES)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusShuttlesItems.SHUTTLE_MK1.get())
                .pattern("I I")
                .pattern("RBR")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('B', ServusShuttlesItems.RAIL.get())
                .unlockedBy("has_rail", has(ServusShuttlesItems.RAIL.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.SHUTTLE_WORKSHOP.get())
                .pattern("III")
                .pattern("PCP")
                .pattern("PPP")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CRAFTING_TABLE)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_shuttle", has(ServusShuttlesItems.SHUTTLE_MK1.get()))
                .save(MaterialRecipes.output(output, registries, MaterialKind.WOOD));

        // Copper heads on sleepers in the wood of the planks.
        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.FARM_RAIL.get(), 16)
                .pattern("C C")
                .pattern("CPC")
                .pattern("C C")
                .define('C', Items.COPPER_INGOT)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(MaterialRecipes.output(output, registries, MaterialKind.WOOD));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ServusShuttlesItems.FIELD_HEAD.get())
                .pattern("IBI")
                .pattern("PCP")
                .pattern("PRP")
                .define('I', Items.IRON_INGOT)
                .define('B', ServusLogisticsItems.FLUID_TANK.get())
                .define('C', Items.COMPOSTER)
                .define('P', ItemTags.PLANKS)
                .define('R', ServusShuttlesItems.FARM_RAIL.get())
                .unlockedBy("has_farm_rail", has(ServusShuttlesItems.FARM_RAIL.get()))
                .save(MaterialRecipes.output(output, registries, MaterialKind.WOOD));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusShuttlesItems.HANDLING_UPGRADE.get())
                .pattern("CRC")
                .pattern("RPR")
                .pattern("CRC")
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .define('P', Items.PISTON)
                .unlockedBy("has_shuttle", has(ServusShuttlesItems.SHUTTLE_MK1.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusShuttlesItems.WIRELESS_UPGRADE.get())
                .pattern(" C ")
                .pattern("IEI")
                .pattern("RIR")
                .define('C', Items.COPPER_INGOT)
                .define('I', Items.IRON_INGOT)
                .define('E', Items.ENDER_EYE)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_ender_eye", has(Items.ENDER_EYE))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusShuttlesItems.RANGE_UPGRADE.get())
                .pattern("ISI")
                .pattern("WCW")
                .pattern("ISI")
                .define('I', Items.COPPER_INGOT)
                .define('S', Items.WHEAT_SEEDS)
                .define('W', Items.BONE_MEAL)
                .define('C', Items.COMPOSTER)
                .unlockedBy("has_field_head", has(ServusShuttlesItems.FIELD_HEAD.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusShuttlesItems.HARVESTER.get())
                .pattern("ISI")
                .pattern("IHI")
                .pattern(" P ")
                .define('I', Items.IRON_INGOT)
                .define('S', Items.SHEARS)
                .define('H', Items.IRON_HOE)
                .define('P', Items.HOPPER)
                .unlockedBy("has_shuttle", has(ServusShuttlesItems.SHUTTLE_MK1.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusShuttlesItems.SPREADER.get())
                .pattern("IHI")
                .pattern("IDI")
                .pattern(" B ")
                .define('I', Items.IRON_INGOT)
                .define('H', Items.HOPPER)
                .define('D', Items.DISPENSER)
                .define('B', Items.BONE_BLOCK)
                .unlockedBy("has_shuttle", has(ServusShuttlesItems.SHUTTLE_MK1.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusShuttlesItems.LUMBER.get())
                .pattern("IAI")
                .pattern("ISI")
                .pattern(" P ")
                .define('I', Items.IRON_INGOT)
                .define('A', Items.IRON_AXE)
                .define('S', Items.STONECUTTER)
                .define('P', Items.PISTON)
                .unlockedBy("has_shuttle", has(ServusShuttlesItems.SHUTTLE_MK1.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.TOOLS, ServusShuttlesItems.TANKER.get())
                .pattern("IBI")
                .pattern("ITI")
                .pattern("IBI")
                .define('I', Items.IRON_INGOT)
                .define('B', Items.BUCKET)
                .define('T', ServusLogisticsItems.FLUID_TANK.get())
                .unlockedBy("has_shuttle", has(ServusShuttlesItems.SHUTTLE_MK1.get()))
                .save(output);
    }

    /** Builds a rack recipe from the parts of a shaped one. */
    private interface WoodRecipeFactory {
        Recipe<?> create(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern,
                         ItemStackTemplate result);
    }

    /** Saves a shaped recipe as a rack recipe, which remembers the planks' wood. */
    private static RecipeOutput woodRecipe(RecipeOutput output, WoodRecipeFactory factory) {
        return new RecipeOutput() {
            @Override
            public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement,
                               ICondition... conditions) {
                ShapedRecipe shaped = (ShapedRecipe) recipe;
                ShapedCraftingRecipeDisplay display = (ShapedCraftingRecipeDisplay) shaped.display().getFirst();
                ItemStackTemplate result = ((SlotDisplay.ItemStackSlotDisplay) display.result()).stack();
                output.accept(id, factory.create(new Recipe.CommonInfo(shaped.showNotification()),
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
