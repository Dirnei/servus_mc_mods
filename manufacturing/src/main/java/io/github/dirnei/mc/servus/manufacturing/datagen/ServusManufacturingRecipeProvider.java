package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinationRecipe;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulationRecipe;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorRecipe;
import io.github.dirnei.mc.servus.manufacturing.alchemy.sight.ElementalSight;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principles;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
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
        coagulation(output);
        athanor(output);
    }

    /** Crafts on the Athanor; the numbers are datapack examples. */
    private void athanor(RecipeOutput output) {
        athanorCraft(output, "blaze_powder", Ingredient.of(Items.GUNPOWDER), new Principles(0, 200, 0), 0,
                new ItemStack(Items.BLAZE_POWDER));
        athanorCraft(output, "experience_bottle", Ingredient.of(Items.GLASS_BOTTLE), new Principles(50, 0, 100), 1,
                new ItemStack(Items.EXPERIENCE_BOTTLE));
        output.accept(ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "athanor/elixir_of_sight"),
                new AthanorRecipe(new SizedIngredient(Ingredient.of(ServusManufacturingItems.QUICKSILVER.get()), 1),
                        new Principles(100, 100, 250), 1, new ItemStack(ElementalSight.ELIXIR.get()), 200), null);
    }

    static void athanorCraft(RecipeOutput output, String name, Ingredient input, Principles principles, int tier, ItemStack result) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "athanor/" + name);
        output.accept(id, new AthanorRecipe(new SizedIngredient(input, 1), principles, tier, result, AthanorRecipe.DEFAULT_TIME), null);
    }

    /** What the Coagulator makes from the principles; no metal in Nigredo. The numbers are datapack examples. */
    private void coagulation(RecipeOutput output) {
        coagulate(output, "salt_crystal", 250, 0, 0, new ItemStack(ServusManufacturingItems.SALT_CRYSTAL.get()));
        coagulate(output, "sulfur", 0, 250, 0, new ItemStack(ServusManufacturingItems.SULFUR.get()));
        coagulate(output, "quicksilver", 0, 0, 250, new ItemStack(ServusManufacturingItems.QUICKSILVER.get()));
        coagulate(output, "clay_ball", 100, 0, 50, new ItemStack(Items.CLAY_BALL));
        coagulate(output, "flint", 150, 0, 0, new ItemStack(Items.FLINT));
        coagulate(output, "gunpowder", 50, 150, 0, new ItemStack(Items.GUNPOWDER));
        coagulate(output, "redstone", 50, 0, 150, new ItemStack(Items.REDSTONE));
        coagulate(output, "glowstone_dust", 0, 100, 100, new ItemStack(Items.GLOWSTONE_DUST));
        coagulate(output, "bone_meal", 60, 20, 0, new ItemStack(Items.BONE_MEAL));
        coagulate(output, "slime_ball", 0, 50, 150, new ItemStack(Items.SLIME_BALL));
    }

    private static void coagulate(RecipeOutput output, String name, int salt, int sulfur, int mercury, ItemStack result) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "coagulation/" + name);
        output.accept(id, new CoagulationRecipe(new Principles(salt, sulfur, mercury), Optional.empty(), result,
                CoagulationRecipe.DEFAULT_TIME, Optional.empty()), null);
    }

    /** What the Calciner burns common matter into; the numbers are datapack examples. */
    private void calcination(RecipeOutput output) {
        calcine(output, "cobblestone", Ingredient.of(Tags.Items.COBBLESTONES), 100, 0, 10);
        calcine(output, "stone", Ingredient.of(Tags.Items.STONES), 100, 0, 10);
        calcine(output, "dirt", Ingredient.of(ItemTags.DIRT), 50, 20, 20);
        calcine(output, "sand", Ingredient.of(Tags.Items.SANDS), 80, 0, 10);
        calcine(output, "gravel", Ingredient.of(Tags.Items.GRAVELS), 80, 0, 10);
        calcine(output, "logs", Ingredient.of(ItemTags.LOGS), 20, 100, 10);
        calcine(output, "planks", Ingredient.of(ItemTags.PLANKS), 5, 25, 0);
        calcine(output, "leaves", Ingredient.of(ItemTags.LEAVES), 0, 40, 20);
        calcine(output, "saplings", Ingredient.of(ItemTags.SAPLINGS), 0, 40, 20);
        calcine(output, "crops", Ingredient.of(Tags.Items.CROPS), 10, 30, 30);
        calcine(output, "seeds", Ingredient.of(Tags.Items.SEEDS), 10, 30, 30);
        calcine(output, "coal", Ingredient.of(ItemTags.COALS), 0, 200, 0);
        calcine(output, "raw_materials", Ingredient.of(Tags.Items.RAW_MATERIALS), 150, 0, 50);
        calcine(output, "redstone", Ingredient.of(Tags.Items.DUSTS_REDSTONE), 0, 0, 100);
        calcine(output, "bones", Ingredient.of(Tags.Items.BONES), 100, 0, 0);
    }

    private static void calcine(RecipeOutput output, String name, Ingredient input, int salt, int sulfur, int mercury) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "calcination/" + name);
        output.accept(id, new CalcinationRecipe(new SizedIngredient(input, 1), new Principles(salt, sulfur, mercury),
                CalcinationRecipe.DEFAULT_TIME), null);
    }

    private void crafting(RecipeOutput output) {

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ServusManufacturingItems.CALCINER.get())
                .pattern("BIB")
                .pattern("IFI")
                .pattern("BCB")
                .define('B', Items.BRICKS)
                .define('I', Items.IRON_INGOT)
                .define('F', Items.FURNACE)
                .define('C', Items.CAULDRON)
                .unlockedBy("has_furnace", has(Items.FURNACE))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ServusManufacturingItems.SALT_CRYSTAL_BLOCK.get())
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .define('S', ServusManufacturingItems.SALT_CRYSTAL.get())
                .unlockedBy("has_salt_crystal", has(ServusManufacturingItems.SALT_CRYSTAL.get()))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ServusManufacturingItems.SALT_CRYSTAL.get(), 9)
                .requires(ServusManufacturingItems.SALT_CRYSTAL_BLOCK.get())
                .unlockedBy("has_salt_crystal_block", has(ServusManufacturingItems.SALT_CRYSTAL_BLOCK.get()))
                .save(output, ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "salt_crystal_from_block"));

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
                .save(output, ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "journal"));

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
