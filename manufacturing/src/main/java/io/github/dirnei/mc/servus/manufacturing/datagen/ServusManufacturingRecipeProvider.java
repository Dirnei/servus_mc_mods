package io.github.dirnei.mc.servus.manufacturing.datagen;

import static io.github.dirnei.mc.servus.manufacturing.datagen.AssemblingRecipeBuilder.assembling;
import static io.github.dirnei.mc.servus.manufacturing.datagen.CrushingRecipeBuilder.crushing;

import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;

final class ServusManufacturingRecipeProvider extends RecipeProvider {

    ServusManufacturingRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        crafting(output);
        sinkRecipes(output);
        assemblingRecipes(output);
        crushingRecipes(output);
        smeltingRecipes(output);
    }

    private void crafting(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.BURNER_EXTRACTOR.get())
                .pattern("III")
                .pattern("IFI")
                .pattern("CCC")
                .define('I', Items.IRON_INGOT)
                .define('F', Items.FURNACE)
                .define('C', Items.COBBLESTONE)
                .unlockedBy("has_furnace", has(Items.FURNACE))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.BURNER_PUMP.get())
                .pattern("IBI")
                .pattern("IFI")
                .pattern("CCC")
                .define('I', Items.IRON_INGOT)
                .define('B', Items.BUCKET)
                .define('F', Items.FURNACE)
                .define('C', Items.COBBLESTONE)
                .unlockedBy("has_bucket", has(Items.BUCKET))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.BURNER_SMELTER.get())
                .pattern("III")
                .pattern("BFB")
                .pattern("BBB")
                .define('I', Items.IRON_INGOT)
                .define('F', Items.FURNACE)
                .define('B', Items.BRICKS)
                .unlockedBy("has_furnace", has(Items.FURNACE))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.COAL_GENERATOR.get())
                .pattern("ICI")
                .pattern("CFC")
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .define('F', Items.FURNACE)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.RESEARCH_LAB.get())
                .pattern("IGI")
                .pattern("RBR")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .define('R', Items.REDSTONE)
                .define('B', Items.BOOK)
                .unlockedBy("has_book", has(Items.BOOK))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.ASSEMBLER.get())
                .pattern("ICI")
                .pattern("RTR")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .define('T', Items.CRAFTING_TABLE)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(output);
    }

    /** The Sink and its Chimney: hand crafted from bricks and iron, no research. */
    private void sinkRecipes(RecipeOutput output) {
        // Eight Sink blocks build one 2x2x2 Sink.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.SINK.get(), 8)
                .pattern("BBB")
                .pattern("BRB")
                .pattern("BBB")
                .define('B', Items.BRICKS)
                .define('R', Items.IRON_BARS)
                .unlockedBy("has_bricks", has(Items.BRICKS))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.CHIMNEY.get(), 2)
                .pattern("B B")
                .pattern("B B")
                .pattern("B B")
                .define('B', Items.BRICK)
                .unlockedBy("has_brick", has(Items.BRICK))
                .save(output);
    }

    private void assemblingRecipes(RecipeOutput output) {
        assembling(ServusManufacturingItems.IRON_GEAR.get(), 1)
                .requires(Items.IRON_INGOT, 2)
                .time(20)
                .save(output, "iron_gear");

        assembling(ServusManufacturingItems.COPPER_WIRE.get(), 2)
                .requires(Items.COPPER_INGOT, 1)
                .time(10)
                .save(output, "copper_wire");

        assembling(ServusManufacturingItems.ELECTRIC_EXTRACTOR.get(), 1)
                .requires(ServusManufacturingItems.BURNER_EXTRACTOR.get(), 1)
                .requires(ServusManufacturingItems.IRON_GEAR.get(), 4)
                .requires(ServusManufacturingItems.COPPER_WIRE.get(), 6)
                .time(200)
                .tech("electric_extractor")
                .save(output, "electric_extractor");

        assembling(ServusManufacturingItems.ELECTRIC_PUMP.get(), 1)
                .requires(ServusManufacturingItems.BURNER_PUMP.get(), 1)
                .requires(ServusManufacturingItems.IRON_GEAR.get(), 4)
                .requires(ServusManufacturingItems.COPPER_WIRE.get(), 4)
                .time(200)
                .tech("electric_extractor")
                .save(output, "electric_pump");

        assembling(ServusManufacturingItems.CRUSHER.get(), 1)
                .requires(ServusManufacturingItems.ELECTRIC_EXTRACTOR.get(), 1)
                .requires(ServusManufacturingItems.IRON_GEAR.get(), 4)
                .requires(ServusManufacturingItems.COPPER_WIRE.get(), 6)
                .time(200)
                .tech("crusher")
                .save(output, "crusher");

        assembling(Items.STONE, 2)
                .requires(Items.COBBLESTONE, 2)
                .time(20)
                .tech("stone_processing")
                .save(output, "stone");

        assembling(Items.STONE_BRICKS, 2)
                .requires(Items.STONE, 2)
                .time(20)
                .tech("stone_processing")
                .save(output, "stone_bricks");

        assembling(Items.GLASS, 1)
                .requires(Items.SAND, 1)
                .time(40)
                .tech("stone_processing")
                .save(output, "glass");

        assembling(ServusManufacturingItems.CHUNK_LOADER.get(), 1)
                .requires(ServusManufacturingItems.IRON_GEAR.get(), 4)
                .requires(ServusManufacturingItems.COPPER_WIRE.get(), 8)
                .requires(Items.ENDER_PEARL, 1)
                .time(200)
                .save(output, "chunk_loader");

        assembling(ServusManufacturingItems.RED_PACK.get(), 1)
                .requires(ServusManufacturingItems.IRON_GEAR.get(), 1)
                .requires(ServusManufacturingItems.COPPER_WIRE.get(), 2)
                .time(100)
                .save(output, "red_pack");
    }

    private void crushingRecipes(RecipeOutput output) {
        crushing(Items.RAW_IRON, 1, ServusManufacturingItems.IRON_GRIT.get(), 2).time(60).save(output, "raw_iron");
        crushing(Items.RAW_COPPER, 1, ServusManufacturingItems.COPPER_GRIT.get(), 2).time(60).save(output, "raw_copper");
        crushing(Items.COBBLESTONE, 1, Items.GRAVEL, 1).time(40).save(output, "cobblestone");
        crushing(Items.GRAVEL, 1, Items.SAND, 1).time(40).save(output, "gravel");
    }

    private void smeltingRecipes(RecipeOutput output) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ServusManufacturingItems.IRON_GRIT.get()), RecipeCategory.MISC, Items.IRON_INGOT, 0.7F, 200)
                .unlockedBy("has_iron_grit", has(ServusManufacturingItems.IRON_GRIT.get()))
                .save(output, "servus_manufacturing:iron_ingot_from_grit_smelting");
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ServusManufacturingItems.IRON_GRIT.get()), RecipeCategory.MISC, Items.IRON_INGOT, 0.7F, 100)
                .unlockedBy("has_iron_grit", has(ServusManufacturingItems.IRON_GRIT.get()))
                .save(output, "servus_manufacturing:iron_ingot_from_grit_blasting");
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ServusManufacturingItems.COPPER_GRIT.get()), RecipeCategory.MISC, Items.COPPER_INGOT, 0.7F, 200)
                .unlockedBy("has_copper_grit", has(ServusManufacturingItems.COPPER_GRIT.get()))
                .save(output, "servus_manufacturing:copper_ingot_from_grit_smelting");
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ServusManufacturingItems.COPPER_GRIT.get()), RecipeCategory.MISC, Items.COPPER_INGOT, 0.7F, 100)
                .unlockedBy("has_copper_grit", has(ServusManufacturingItems.COPPER_GRIT.get()))
                .save(output, "servus_manufacturing:copper_ingot_from_grit_blasting");
    }
}
