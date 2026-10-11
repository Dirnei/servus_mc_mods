package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinationRecipe;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurificationRecipe;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorRecipe;
import java.util.Optional;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import io.github.dirnei.mc.servus.manufacturing.journal.JournalBookItem;
import net.minecraft.world.item.Items;

final class ServusManufacturingRecipeProvider extends RecipeProvider {

    ServusManufacturingRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        crafting(output);
        calcination(output);
        for (AlchemyRecipeTable.Purify row : AlchemyRecipeTable.purification()) {
            output.accept(Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "purification/" + row.name()),
                    new PurificationRecipe(new SizedIngredient(row.input(), row.count()), row.water(), row.salt(), row.residue(), row.time()), null);
        }
        coagulation(output);
        athanor(output);
    }

    /** Crafts on the Athanor, from the shared table the journal lists. */
    private void athanor(RecipeOutput output) {
        for (AlchemyRecipeTable.AthanorCraft craft : AlchemyRecipeTable.athanor()) {
            Identifier id = Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "athanor/" + craft.name());
            output.accept(id, new AthanorRecipe(new SizedIngredient(craft.input(), 1), craft.principles(), craft.tier(),
                    craft.result(), craft.time()), null);
        }
    }

    /** What the Coagulator makes from the principles, from the shared table the journal lists. */
    private void coagulation(RecipeOutput output) {
        for (AlchemyRecipeTable.Coagulate row : AlchemyRecipeTable.coagulation()) {
            Identifier id = Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "coagulation/" + row.name());
            output.accept(id, new CoagulationRecipe(row.input(), Optional.empty(), row.result(),
                    CoagulationRecipe.DEFAULT_TIME, Optional.empty()), null);
        }
    }

    /** What the Calciner burns common matter into, from the shared table the journal lists. */
    private void calcination(RecipeOutput output) {
        for (AlchemyRecipeTable.Calcine row : AlchemyRecipeTable.calcination()) {
            Identifier id = Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "calcination/" + row.name());
            output.accept(id, new CalcinationRecipe(new SizedIngredient(row.input(), 1), row.output(),
                    CalcinationRecipe.DEFAULT_TIME), null);
        }
    }

    private void crafting(RecipeOutput output) {

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.CALCINER.get())
                .pattern("BIB")
                .pattern("BCB")
                .define('B', Items.BRICKS)
                .define('I', Items.IRON_BARS)
                .define('C', Items.CAULDRON)
                .unlockedBy("has_cauldron", has(Items.CAULDRON))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.PURIFIER.get())
                .pattern("P P")
                .pattern("PCP")
                .pattern("PHP")
                .define('P', ItemTags.PLANKS)
                .define('C', Items.CAULDRON)
                .define('H', Items.HOPPER)
                .unlockedBy("has_cauldron", has(Items.CAULDRON))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.VAT.get())
                .pattern("C C")
                .pattern("CBC")
                .define('C', Items.COPPER_INGOT)
                .define('B', Items.BUCKET)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.CATCH_BASIN.get())
                .pattern("P P")
                .pattern("PIP")
                .define('P', ItemTags.PLANKS)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.RESIDUE_CROCK.get())
                .pattern("B B")
                .pattern("BXB")
                .pattern("BBB")
                .define('B', Items.BRICK)
                .define('X', Items.BARREL)
                .unlockedBy("has_brick", has(Items.BRICK))
                .save(output);

        // The parts of the Calciner structure, all from vanilla materials.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.FIREBOX.get())
                .pattern("BIB")
                .pattern("BFB")
                .define('B', Items.BRICKS)
                .define('I', Items.IRON_BARS)
                .define('F', Items.FURNACE)
                .unlockedBy("has_furnace", has(Items.FURNACE))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.ASH_PAN.get())
                .pattern("B B")
                .pattern("BIB")
                .define('B', Items.BRICKS)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_bricks", has(Items.BRICKS))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.ALEMBIC.get())
                .pattern(" C ")
                .pattern("CGC")
                .pattern("C C")
                .define('C', Items.COPPER_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.RECEIVER.get())
                .pattern(" P ")
                .pattern("GBG")
                .pattern(" C ")
                .define('P', Items.GLASS_PANE)
                .define('G', Items.GLASS)
                .define('B', Items.GLASS_BOTTLE)
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_glass_bottle", has(Items.GLASS_BOTTLE))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.COPPER_DUCT.get(), 3)
                .pattern("CCC")
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.SULFUR_HOOD.get())
                .pattern(" C ")
                .pattern("CBC")
                .define('C', Items.COPPER_INGOT)
                .define('B', Items.BRICKS)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.ATHANOR.get())
                .pattern("BCB")
                .pattern("BFB")
                .pattern("BBB")
                .define('B', Items.BRICKS)
                .define('C', Items.CAULDRON)
                .define('F', Items.BLAST_FURNACE)
                .unlockedBy("has_blast_furnace", has(Items.BLAST_FURNACE))
                .save(output);

        // Paracelsus' Journal: a book and a piece of coal, the first fire.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, JournalBookItem.create())
                .requires(Items.BOOK)
                .requires(ItemTags.COALS)
                .unlockedBy("has_book", has(Items.BOOK))
                .save(output, Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "journal"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.COAGULATOR.get())
                .pattern("IGI")
                .pattern("GCG")
                .pattern("IRI")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .define('C', Items.CAULDRON)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_cauldron", has(Items.CAULDRON))
                .save(output);
    }
}
