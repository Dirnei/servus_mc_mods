package io.github.dirnei.mc.servus.alchemy.datagen;

import io.github.dirnei.mc.servus.alchemy.CalcinationRecipe;
import io.github.dirnei.mc.servus.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorRecipe;
import io.github.dirnei.mc.servus.alchemy.prelum.PressingRecipe;
import io.github.dirnei.mc.servus.alchemy.purifier.PurificationRecipe;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

final class ServusAlchemyRecipeProvider extends RecipeProvider {

    private ServusAlchemyRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    /** The data provider: builds the recipes once the registries are known. */
    static final class Runner extends RecipeProvider.Runner {

        Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ServusAlchemyRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Servus Alchemy recipes";
        }
    }

    @Override
    protected void buildRecipes() {
        crafting(output);
        calcination(output);
        purification(output);
        coagulation(output);
        athanor(output);
        pressing(output);
    }

    /** What the Prelum presses, from the shared table the journal lists. */
    private void pressing(RecipeOutput output) {
        for (AlchemyRecipeTable.Press row : AlchemyRecipeTable.pressing()) {
            output.accept(key("pressing/" + row.name()), new PressingRecipe(new SizedIngredient(row.input().ingredient(items), row.count()),
                    row.result(), row.time()), null);
        }
    }

    /** What the Purifier leaches, from the shared table the journal lists. */
    private void purification(RecipeOutput output) {
        for (AlchemyRecipeTable.Purify row : AlchemyRecipeTable.purification()) {
            output.accept(key("purification/" + row.name()), new PurificationRecipe(new SizedIngredient(row.input().ingredient(items), row.count()),
                    row.water(), row.salt(), Optional.of(row.residue()), row.time()), null);
        }
    }

    /** Crafts on the Athanor, from the shared table the journal lists. */
    private void athanor(RecipeOutput output) {
        for (AlchemyRecipeTable.AthanorCraft craft : AlchemyRecipeTable.athanor()) {
            output.accept(key("athanor/" + craft.name()), new AthanorRecipe(new SizedIngredient(craft.input().ingredient(items), 1), craft.principles(), craft.tier(),
                    craft.result(), craft.time()), null);
        }
    }

    /** What the Coagulator makes from the principles, from the shared table the journal lists. */
    private void coagulation(RecipeOutput output) {
        for (AlchemyRecipeTable.Coagulate row : AlchemyRecipeTable.coagulation()) {
            output.accept(key("coagulation/" + row.name()), new CoagulationRecipe(row.input(), Optional.empty(), row.result(),
                    CoagulationRecipe.DEFAULT_TIME, Optional.empty()), null);
        }
    }

    /** What the Calciner burns common matter into, from the shared table the journal lists. */
    private void calcination(RecipeOutput output) {
        for (AlchemyRecipeTable.Calcine row : AlchemyRecipeTable.calcination()) {
            output.accept(key("calcination/" + row.name()), new CalcinationRecipe(new SizedIngredient(row.input().ingredient(items), 1), row.output(),
                    CalcinationRecipe.DEFAULT_TIME), null);
        }
    }

    private static ResourceKey<Recipe<?>> key(String path) {
        return ServusRecipes.key(Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, path));
    }

    private void crafting(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusAlchemyItems.SALAMANDER_FORGE.get())
                .pattern("DDD")
                .pattern("DID")
                .pattern("DFD")
                .define('D', Items.DEEPSLATE_BRICKS)
                .define('I', ServusAlchemyItems.IGNIS_PERPETUUS.get())
                .define('F', Items.FURNACE)
                .unlockedBy("has_ignis_perpetuus", has(ServusAlchemyItems.IGNIS_PERPETUUS.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusAlchemyItems.RECEPTACULUM.get())
                .pattern("DCD")
                .pattern("DID")
                .pattern("BBB")
                .define('D', Items.DEEPSLATE_BRICKS)
                .define('C', Items.CHEST)
                .define('I', ServusAlchemyItems.IGNIS_PERPETUUS.get())
                .define('B', Items.BRICKS)
                .unlockedBy("has_ignis_perpetuus", has(ServusAlchemyItems.IGNIS_PERPETUUS.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusAlchemyItems.PRELUM.get())
                .pattern(" S ")
                .pattern("LPL")
                .pattern("DDD")
                .define('S', ServusAlchemyItems.PRELUM_SCREW.get())
                .define('L', Items.STRIPPED_DARK_OAK_LOG)
                .define('P', Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
                .define('D', Items.POLISHED_DEEPSLATE)
                .unlockedBy("has_prelum_screw", has(ServusAlchemyItems.PRELUM_SCREW.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusAlchemyItems.CALCINER.get())
                .pattern("BIB")
                .pattern("BCB")
                .define('B', Items.STONE_BRICKS)
                .define('I', Items.IRON_BARS)
                .define('C', Items.CAULDRON)
                .unlockedBy("has_cauldron", has(Items.CAULDRON))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusAlchemyItems.PURIFIER.get())
                .pattern("P P")
                .pattern("PCP")
                .pattern("PHP")
                .define('P', ItemTags.PLANKS)
                .define('C', Items.CAULDRON)
                .define('H', Items.HOPPER)
                .unlockedBy("has_cauldron", has(Items.CAULDRON))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusAlchemyItems.VAT.get())
                .pattern("C C")
                .pattern("CBC")
                .define('C', Items.COPPER_INGOT)
                .define('B', Items.BUCKET)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusAlchemyItems.CATCH_BASIN.get())
                .pattern("P P")
                .pattern("PIP")
                .define('P', ItemTags.PLANKS)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusAlchemyItems.RESIDUE_CROCK.get())
                .pattern("B B")
                .pattern("BXB")
                .pattern("BBB")
                .define('B', Items.BRICK)
                .define('X', Items.BARREL)
                .unlockedBy("has_brick", has(Items.BRICK))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusAlchemyItems.FIREBOX.get())
                .pattern("BIB")
                .pattern("BFB")
                .define('B', Items.POLISHED_DEEPSLATE)
                .define('I', Items.IRON_BARS)
                .define('F', Items.FURNACE)
                .unlockedBy("has_furnace", has(Items.FURNACE))
                .save(output);
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusAlchemyItems.ASH_PAN.get())
                .pattern("B B")
                .pattern("BIB")
                .define('B', Items.DEEPSLATE_TILES)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_deepslate_tiles", has(Items.DEEPSLATE_TILES))
                .save(output);
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusAlchemyItems.ALEMBIC.get())
                .pattern(" C ")
                .pattern("CGC")
                .pattern("C C")
                .define('C', Items.COPPER_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output);
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusAlchemyItems.RECEIVER.get())
                .pattern(" P ")
                .pattern("GBG")
                .pattern(" C ")
                .define('P', Items.GLASS_PANE)
                .define('G', Items.GLASS)
                .define('B', Items.GLASS_BOTTLE)
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_glass_bottle", has(Items.GLASS_BOTTLE))
                .save(output);
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusAlchemyItems.COPPER_DUCT.get(), 3)
                .pattern("CCC")
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output);
        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusAlchemyItems.SULFUR_HOOD.get())
                .pattern(" C ")
                .pattern("CBC")
                .define('C', Items.COPPER_INGOT)
                .define('B', Items.BRICKS)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusAlchemyItems.ATHANOR.get())
                .pattern("BCB")
                .pattern("BFB")
                .pattern("BBB")
                .define('B', Items.STONE_BRICKS)
                .define('C', Items.CAULDRON)
                .define('F', Items.BLAST_FURNACE)
                .unlockedBy("has_blast_furnace", has(Items.BLAST_FURNACE))
                .save(output);

        ShapelessRecipeBuilder.shapeless(items, RecipeCategory.MISC, ServusAlchemyItems.PARACELSUS_JOURNAL.get())
                .requires(Items.BOOK)
                .requires(ItemTags.COALS)
                .unlockedBy("has_book", has(Items.BOOK))
                .save(output, key("journal"));

        ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ServusAlchemyItems.COAGULATOR.get())
                .pattern("SGS")
                .pattern("GCG")
                .pattern("SRS")
                .define('S', Items.COBBLESTONE)
                .define('G', Items.GLASS)
                .define('C', Items.CAULDRON)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_cauldron", has(Items.CAULDRON))
                .save(output);
    }
}
